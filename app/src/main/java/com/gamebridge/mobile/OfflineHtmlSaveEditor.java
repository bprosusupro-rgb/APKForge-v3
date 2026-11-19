package com.gamebridge.mobile;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inspects and edits only an APK Forge backup from a local-file HTML game's localStorage. */
public final class OfflineHtmlSaveEditor {
    private static final String LAB_ORIGIN = "file:///apkforge-offline-save-editor/index.html";
    private static final int MAX_EDIT_FIELDS = 64;
    private static final int MAX_VALUE_CHARS = 40;

    private OfflineHtmlSaveEditor() { }

    /** Import rejects online-origin backups through LocalGameStateStore's origin binding. */
    public static void importBackup(File appFiles, byte[] backup) throws Exception {
        if (appFiles == null) throw new IllegalArgumentException("Private app storage is required.");
        LocalGameStateStore.restoreBackup(labDirectory(appFiles), LAB_ORIGIN, backup);
        parseState(appFiles);
    }

    /** Numeric values only; paths use JSON Pointer escaping and identify existing save fields. */
    public static Map<String, String> numericFields(File appFiles) throws Exception {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        collect(parseState(appFiles), "", fields);
        return fields;
    }

    /** Applies a batch atomically to the imported local backup; it never edits a live page. */
    public static void applyValues(File appFiles, Map<String, String> changes) throws Exception {
        if (changes == null || changes.isEmpty()) return;
        if (changes.size() > MAX_EDIT_FIELDS) throw new IllegalArgumentException("Too many save fields were selected.");
        Object state = parseState(appFiles);
        for (Map.Entry<String, String> change : changes.entrySet()) {
            String value = change.getValue();
            if (value == null || value.trim().isEmpty() || value.length() > MAX_VALUE_CHARS) {
                throw new IllegalArgumentException("Enter a finite numeric value with at most 40 characters.");
            }
            BigDecimal parsed = number(value);
            if (parsed.precision() > 30 || parsed.abs().compareTo(new BigDecimal("1000000000000000")) > 0) {
                throw new IllegalArgumentException("The numeric value is outside the supported safe range.");
            }
            Object oldValue = valueAt(state, change.getKey());
            if (!(oldValue instanceof String || oldValue instanceof BigDecimal) || !isNumber(oldValue)) {
                throw new IllegalArgumentException("Only an existing numeric save field can be edited.");
            }
        }
        for (Map.Entry<String, String> change : changes.entrySet()) {
            Object oldValue = valueAt(state, change.getKey());
            Object replacement = oldValue instanceof String ? change.getValue() : number(change.getValue());
            setValue(state, change.getKey(), replacement);
        }
        LocalGameStateStore.save(labDirectory(appFiles), LAB_ORIGIN, Json.write(state));
    }

    public static byte[] exportBackup(File appFiles) throws Exception {
        return LocalGameStateStore.exportBackup(labDirectory(appFiles), LAB_ORIGIN);
    }

    /** Sends only local field paths and numbers to the explicitly enabled on-device model. */
    public static String analysisPrompt(File appFiles) throws Exception {
        Map<String, String> fields = numericFields(appFiles);
        StringBuilder prompt = new StringBuilder("Offline HTML save review on this device only. These numeric fields came from a user-selected local game save. Identify up to three paths whose names may indicate virtual currency; state uncertainty. Do not suggest online/server changes, real-money changes, code, or automatic edits.\nFields:\n");
        int included = 0;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (included++ >= 24) break;
            String line = field.getKey() + " = " + field.getValue() + "\n";
            if (prompt.length() + line.length() > RepairAssistantPolicy.MAX_PROMPT_CHARS) break;
            prompt.append(line);
        }
        if (fields.isEmpty()) prompt.append("No numeric fields were found.\n");
        return prompt.length() > RepairAssistantPolicy.MAX_PROMPT_CHARS
                ? prompt.substring(0, RepairAssistantPolicy.MAX_PROMPT_CHARS) : prompt.toString();
    }

    public static boolean mayEditOnlinePage(String url) {
        return false;
    }

    public static boolean looksLikeCurrency(String path) {
        if (path == null) return false;
        String words = path.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
        return java.util.regex.Pattern.compile("(^|[^a-z])(coin|coins|cash|money|gold|gem|gems|credit|credits|token|tokens|balance|shard|shards|currency)([^a-z]|$)")
                .matcher(words).find();
    }

    private static File labDirectory(File appFiles) {
        return new File(appFiles, "offline-html-save-editor");
    }

    @SuppressWarnings("unchecked")
    private static Object parseState(File appFiles) throws Exception {
        String source = LocalGameStateStore.load(labDirectory(appFiles), LAB_ORIGIN);
        if (source == null) throw new IllegalStateException("Import a save backup exported from an offline HTML game first.");
        Object parsed = new Json(source).parse();
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("The local save is not a storage object.");
        return decodeEmbeddedJson(parsed);
    }

    private static Object decodeEmbeddedJson(Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : new ArrayList<>(map.entrySet())) {
                map.put(entry.getKey(), decodeEmbeddedJson(entry.getValue()));
            }
            return map;
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) list.set(i, decodeEmbeddedJson(list.get(i)));
            return list;
        }
        if (value instanceof String) {
            String text = (String) value;
            String trimmed = text.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    Object nested = new Json(trimmed).parse();
                    if (nested instanceof Map || nested instanceof List) return new EmbeddedJsonString(decodeEmbeddedJson(nested));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return value;
    }

    private static void collect(Object value, String path, Map<String, String> output) {
        if (output.size() >= MAX_EDIT_FIELDS) return;
        if (value instanceof EmbeddedJsonString) {
            collect(((EmbeddedJsonString) value).value, path, output);
        } else if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                collect(entry.getValue(), path + "/" + escapePointer(String.valueOf(entry.getKey())), output);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) collect(list.get(i), path + "/" + i, output);
        } else if (isNumber(value)) {
            output.put(path.isEmpty() ? "/" : path, value.toString());
        }
    }

    private static Object valueAt(Object root, String pointer) {
        String[] parts = pointerParts(pointer);
        Object current = root;
        for (String part : parts) {
            current = unwrap(current);
            if (current instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) current;
                if (!map.containsKey(part)) throw new IllegalArgumentException("That save field no longer exists.");
                current = map.get(part);
            } else if (current instanceof List) {
                int index = listIndex(part, ((List<?>) current).size());
                current = ((List<?>) current).get(index);
            } else {
                throw new IllegalArgumentException("That save path is not editable.");
            }
        }
        return current;
    }

    private static void setValue(Object root, String pointer, Object replacement) {
        String[] parts = pointerParts(pointer);
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            current = unwrap(current);
            if (current instanceof Map) current = ((Map<?, ?>) current).get(parts[i]);
            else if (current instanceof List) current = ((List<?>) current).get(listIndex(parts[i], ((List<?>) current).size()));
            else throw new IllegalArgumentException("That save path is not editable.");
        }
        current = unwrap(current);
        String last = parts[parts.length - 1];
        if (current instanceof Map) ((Map<String, Object>) current).put(last, replacement);
        else if (current instanceof List) ((List<Object>) current).set(listIndex(last, ((List<?>) current).size()), replacement);
        else throw new IllegalArgumentException("That save path is not editable.");
    }

    private static Object unwrap(Object value) {
        return value instanceof EmbeddedJsonString ? ((EmbeddedJsonString) value).value : value;
    }

    private static int listIndex(String value, int size) {
        try {
            int index = Integer.parseInt(value);
            if (index >= 0 && index < size) return index;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("That save field no longer exists.");
    }

    private static String[] pointerParts(String pointer) {
        if (pointer == null || !pointer.startsWith("/") || pointer.length() > 512) {
            throw new IllegalArgumentException("The save field path is invalid.");
        }
        String[] parts = pointer.substring(1).split("/", -1);
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].replace("~1", "/").replace("~0", "~");
        return parts;
    }

    private static String escapePointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private static boolean isNumber(Object value) {
        if (!(value instanceof String || value instanceof BigDecimal)) return false;
        try { number(value.toString()); return true; }
        catch (NumberFormatException invalid) { return false; }
    }

    private static BigDecimal number(String value) {
        if (value == null || value.length() > MAX_VALUE_CHARS) throw new NumberFormatException("not a supported number");
        BigDecimal number = new BigDecimal(value.trim());
        if (number.scale() > 12 || number.precision() > 30) throw new NumberFormatException("not a supported number");
        return number;
    }

    private static final class EmbeddedJsonString {
        final Object value;
        EmbeddedJsonString(Object value) { this.value = value; }
    }

    /** Small strict JSON parser/serializer keeps the editor independent from network code and libraries. */
    private static final class Json {
        private final String source;
        private int position;

        Json(String source) {
            if (source == null || source.length() > 8 * 1024 * 1024) throw new IllegalArgumentException("The local save is empty or too large.");
            this.source = source;
        }

        Object parse() {
            Object value = readValue(0);
            whitespace();
            if (position != source.length()) throw error();
            return value;
        }

        private Object readValue(int depth) {
            if (depth > 40) throw new IllegalArgumentException("Save data is nested too deeply.");
            whitespace();
            if (position >= source.length()) throw error();
            char c = source.charAt(position);
            if (c == '{') return readObject(depth + 1);
            if (c == '[') return readArray(depth + 1);
            if (c == '"') return readString();
            if (c == 't' && take("true")) return Boolean.TRUE;
            if (c == 'f' && take("false")) return Boolean.FALSE;
            if (c == 'n' && take("null")) return null;
            return readNumber();
        }

        private Map<String, Object> readObject(int depth) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            position++;
            whitespace();
            if (consume('}')) return map;
            while (true) {
                whitespace();
                if (position >= source.length() || source.charAt(position) != '"') throw error();
                String key = readString();
                whitespace();
                if (!consume(':')) throw error();
                if (map.containsKey(key)) throw new IllegalArgumentException("The save contains duplicate JSON keys.");
                map.put(key, readValue(depth));
                whitespace();
                if (consume('}')) return map;
                if (!consume(',')) throw error();
            }
        }

        private List<Object> readArray(int depth) {
            ArrayList<Object> list = new ArrayList<>();
            position++;
            whitespace();
            if (consume(']')) return list;
            while (true) {
                list.add(readValue(depth));
                whitespace();
                if (consume(']')) return list;
                if (!consume(',')) throw error();
            }
        }

        private String readString() {
            position++;
            StringBuilder value = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '"') return value.toString();
                if (c < 0x20) throw error();
                if (c != '\\') { value.append(c); continue; }
                if (position >= source.length()) throw error();
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"': case '\\': case '/': value.append(escaped); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'u':
                        if (position + 4 > source.length()) throw error();
                        try { value.append((char) Integer.parseInt(source.substring(position, position + 4), 16)); }
                        catch (NumberFormatException invalid) { throw error(); }
                        position += 4;
                        break;
                    default: throw error();
                }
            }
            throw error();
        }

        private BigDecimal readNumber() {
            int start = position;
            if (position < source.length() && source.charAt(position) == '-') position++;
            if (position >= source.length() || !Character.isDigit(source.charAt(position))) throw error();
            if (source.charAt(position) == '0') position++;
            else while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            if (position < source.length() && source.charAt(position) == '.') {
                position++;
                int decimal = position;
                while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
                if (decimal == position) throw error();
            }
            if (position < source.length() && (source.charAt(position) == 'e' || source.charAt(position) == 'E')) {
                position++;
                if (position < source.length() && (source.charAt(position) == '+' || source.charAt(position) == '-')) position++;
                int exponent = position;
                while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
                if (exponent == position) throw error();
            }
            try { return new BigDecimal(source.substring(start, position)); }
            catch (NumberFormatException invalid) { throw error(); }
        }

        private boolean take(String word) {
            if (!source.startsWith(word, position)) return false;
            position += word.length();
            return true;
        }

        private boolean consume(char expected) {
            if (position < source.length() && source.charAt(position) == expected) { position++; return true; }
            return false;
        }

        private void whitespace() {
            while (position < source.length() && (source.charAt(position) == ' ' || source.charAt(position) == '\n'
                    || source.charAt(position) == '\r' || source.charAt(position) == '\t')) position++;
        }

        private IllegalArgumentException error() { return new IllegalArgumentException("The imported save contains malformed JSON."); }

        static String write(Object value) {
            if (value == null) return "null";
            if (value instanceof EmbeddedJsonString) return quote(write(((EmbeddedJsonString) value).value));
            if (value instanceof String) return quote((String) value);
            if (value instanceof BigDecimal) return ((BigDecimal) value).stripTrailingZeros().toPlainString();
            if (value instanceof Boolean) return value.toString();
            if (value instanceof Map) {
                StringBuilder out = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (!first) out.append(',');
                    first = false;
                    out.append(quote(String.valueOf(entry.getKey()))).append(':').append(write(entry.getValue()));
                }
                return out.append('}').toString();
            }
            if (value instanceof List) {
                StringBuilder out = new StringBuilder("[");
                for (int i = 0; i < ((List<?>) value).size(); i++) {
                    if (i > 0) out.append(',');
                    out.append(write(((List<?>) value).get(i)));
                }
                return out.append(']').toString();
            }
            throw new IllegalArgumentException("The save contains an unsupported value.");
        }

        private static String quote(String value) {
            StringBuilder out = new StringBuilder("\"");
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"': out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '\b': out.append("\\b"); break;
                    case '\f': out.append("\\f"); break;
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default:
                        if (c < 0x20) out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                        else out.append(c);
                }
            }
            return out.append('"').toString();
        }
    }
}
