package com.gamebridge.mobile;

import android.app.ActivityManager;
import android.content.Context;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Downloads a pinned small model on explicit opt-in and runs short-lived CPU inference. */
final class RepairAssistantModel {
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 45_000;
    private static final int MAX_PROMPT_CHARS = RepairAssistantPolicy.MAX_PROMPT_CHARS;

    interface ProgressListener {
        void onProgress(long received, long expected);
        default boolean isCancelled() { return false; }
    }

    private RepairAssistantModel() { }

    static File modelFile(Context context) {
        return modelFile(context, selectedModel(context));
    }

    static boolean isInstalled(Context context) {
        return isInstalled(context, selectedModel(context));
    }

    static RepairModelCatalog.Model selectedModel(Context context) {
        String id = context.getSharedPreferences("repair-assistant", Context.MODE_PRIVATE)
                .getString("model_id", RepairModelCatalog.COMPACT_ID);
        return RepairModelCatalog.byId(id);
    }

    static File modelFile(Context context, RepairModelCatalog.Model model) {
        return new File(new File(context.getFilesDir(), "local-ai"), model.fileName);
    }

    static boolean isInstalled(Context context, RepairModelCatalog.Model model) {
        File file = modelFile(context, model);
        File digestFile = new File(file.getParentFile(), model.fileName + ".sha256");
        return file.isFile() && RepairAssistantPolicy.isSupportedModelSize(model.id, file.length())
                && digestFile.isFile() && digestFile.length() >= 64 && digestFile.length() <= 256;
    }

    static File download(Context context, ProgressListener listener) throws Exception {
        return download(context, selectedModel(context), listener);
    }

    static File download(Context context, RepairModelCatalog.Model model, ProgressListener listener) throws Exception {
        if (!RepairAssistantPolicy.isTrustedModelUrl(model.downloadUrl)) {
            throw new SecurityException("Model download source failed its built-in allow-list.");
        }
        File target = modelFile(context, model);
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IllegalStateException("Could not create private model storage.");
        }
        if (parent.getUsableSpace() < model.bytes + 256L * 1024L * 1024L) {
            throw new IllegalStateException("Not enough free storage for the selected model and a safe download buffer.");
        }
        File partial = new File(parent, model.fileName + ".part");
        File partialDigest = new File(parent, model.fileName + ".sha256.part");
        HttpURLConnection connection = (HttpURLConnection) new URL(model.downloadUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "APKForge/3.10 (on-device model download)");
        try {
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Model host returned HTTP " + connection.getResponseCode() + ".");
            }
            if (!RepairAssistantPolicy.isTrustedModelRedirect(connection.getURL().toString())) {
                throw new SecurityException("Model download redirected outside the Hugging Face HTTPS domain.");
            }
            long expected = connection.getContentLengthLong();
            if (expected > 0 && !RepairAssistantPolicy.isSupportedModelSize(model.id, expected)) {
                throw new IllegalStateException("The model file size does not match the pinned model artifact.");
            }
            long received = 0;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || (listener != null && listener.isCancelled()))
                        throw new InterruptedException("Model download cancelled.");
                    received += count;
                    if (received > model.bytes) {
                        throw new IllegalStateException("The model download exceeded its pinned artifact size.");
                    }
                    output.write(buffer, 0, count);
                    if (listener != null) listener.onProgress(received, expected);
                }
            }
            if (expected > 0 && received != expected) throw new IllegalStateException("The model download ended before completion.");
            if (!RepairAssistantPolicy.isSupportedModelSize(model.id, received)) {
                throw new IllegalStateException("Downloaded model has an unexpected size; it was discarded.");
            }
            String sha256 = sha256(partial);
            if (!RepairAssistantPolicy.isExpectedModelDigest(model.id, sha256)) {
                throw new SecurityException("The downloaded model did not match the pinned SHA-256 and was discarded.");
            }
            return installVerifiedModel(target, partial, partialDigest, sha256, model);
        } catch (Exception failure) {
            if (partial.isFile()) partial.delete();
            if (partialDigest.isFile()) partialDigest.delete();
            throw failure;
        } finally {
            connection.disconnect();
        }
    }

    static File importModel(Context context, InputStream source, ProgressListener listener) throws Exception {
        return importModel(context, selectedModel(context), source, listener);
    }

    static File importModel(Context context, RepairModelCatalog.Model model, InputStream source, ProgressListener listener) throws Exception {
        if (source == null) throw new IllegalArgumentException("No model file was selected.");
        File target = modelFile(context, model);
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            source.close();
            throw new IllegalStateException("Could not create private model storage.");
        }
        File partial = new File(parent, model.fileName + ".part");
        File partialDigest = new File(parent, model.fileName + ".sha256.part");
        long received = 0;
        try (InputStream input = new BufferedInputStream(source);
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted() || (listener != null && listener.isCancelled()))
                    throw new InterruptedException("Model import cancelled.");
                received += count;
                if (received > model.bytes)
                    throw new IllegalStateException("The selected file is larger than the pinned model.");
                output.write(buffer, 0, count);
                if (listener != null) listener.onProgress(received, model.bytes);
            }
            if (!RepairAssistantPolicy.isSupportedModelSize(model.id, received))
                throw new IllegalStateException("The selected file is not the selected pinned model.");
            String digest = sha256(partial);
            if (!RepairAssistantPolicy.isExpectedModelDigest(model.id, digest))
                throw new SecurityException("The selected model did not match the pinned SHA-256 and was discarded.");
            return installVerifiedModel(target, partial, partialDigest, digest, model);
        } catch (Exception failure) {
            if (partial.isFile()) partial.delete();
            if (partialDigest.isFile()) partialDigest.delete();
            throw failure;
        }
    }

    private static File installVerifiedModel(File target, File partial, File partialDigest, String sha256, RepairModelCatalog.Model model) throws Exception {
        File digestFile = new File(target.getParentFile(), model.fileName + ".sha256");
        writeDigest(partialDigest, sha256);
        if (digestFile.exists() && !digestFile.delete()) throw new IllegalStateException("Could not replace the old model integrity record.");
        if (target.exists() && !target.delete()) throw new IllegalStateException("Could not replace the old local model.");
        if (!partial.renameTo(target)) throw new IllegalStateException("Could not finish saving the model.");
        if (!partialDigest.renameTo(digestFile)) {
            target.delete();
            throw new IllegalStateException("Could not save the model integrity record.");
        }
        return target;
    }

    static String generate(Context context, String prompt) throws Exception {
        return generateWithPrompt(context, prompt, false);
    }

    static String generateSearchSuggestions(Context context, String gameTitle) throws Exception {
        return generateWithPrompt(context, DownloadSearchPlanner.buildPrompt(gameTitle), true);
    }

    static String analyzeOfflineSave(Context context, String localFieldPrompt) throws Exception {
        return generateWithPrompt(context, localFieldPrompt, true);
    }

    private static String generateWithPrompt(Context context, String prompt, boolean searchPrompt) throws Exception {
        if (!RepairAssistantPolicy.isEnabled(
                context.getSharedPreferences("repair-assistant", Context.MODE_PRIVATE)
                        .getBoolean("enabled", false))) {
            throw new IllegalStateException("The local assistant is turned off in Settings.");
        }
        RepairModelCatalog.Model modelInfo = selectedModel(context);
        if (!isInstalled(context, modelInfo)) throw new IllegalStateException("Download the selected local model from Settings first.");
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) activityManager.getMemoryInfo(memoryInfo);
        if (activityManager != null && !RepairModelCatalog.hasEnoughMemory(modelInfo.id, memoryInfo.availMem)) {
            throw new IllegalStateException("This device currently has too little free memory for Gemma 4 E2B. Close other apps or select the Compact model in Settings.");
        }
        File model = modelFile(context, modelInfo);
        String savedDigest = new String(java.nio.file.Files.readAllBytes(
                new File(model.getParentFile(), modelInfo.fileName + ".sha256").toPath()),
                java.nio.charset.StandardCharsets.US_ASCII).trim().split("\\s+")[0];
        if (!RepairAssistantPolicy.isExpectedModelDigest(modelInfo.id, savedDigest)
                || !savedDigest.equalsIgnoreCase(sha256(model))) {
            throw new SecurityException("The local model failed its stored integrity check. Re-download it from Settings.");
        }
        String boundedPrompt = prompt == null ? "" : prompt.trim();
        if (boundedPrompt.isEmpty()) throw new IllegalArgumentException("Enter an error or repair question first.");
        if (boundedPrompt.length() > MAX_PROMPT_CHARS) boundedPrompt = boundedPrompt.substring(0, MAX_PROMPT_CHARS);

        Engine engine = new Engine(new EngineConfig(
                model.getAbsolutePath(),
                new Backend.CPU(RepairAssistantPolicy.cpuThreadsFor(Runtime.getRuntime().availableProcessors()), null),
                null, null, Integer.valueOf(modelInfo.maxContextTokens), null,
                new File(context.getCacheDir(), "repair-ai").getAbsolutePath()));
        try {
            engine.initialize();
            try (Conversation conversation = engine.createConversation(new ConversationConfig())) {
        Message answer = conversation.sendMessage(searchPrompt ? boundedPrompt : buildPrompt(boundedPrompt),
                        java.util.Collections.emptyMap(), null, null, null, Integer.valueOf(96));
                return answer == null ? "The model returned no answer." : answer.toString().trim();
            }
        } finally {
            engine.close();
        }
    }

    private static String buildPrompt(String question) {
        return RepairAssistantPolicy.buildRepairPrompt(question);
    }

    static String sha256(File file) throws Exception {
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return sha256(input);
        }
    }

    static String sha256(InputStream source) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new BufferedInputStream(source)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder value = new StringBuilder(64);
        for (byte item : digest.digest()) value.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return value.toString();
    }

    private static void writeDigest(File destination, String sha256) throws Exception {
        try (FileOutputStream output = new FileOutputStream(destination)) {
            String modelName = destination.getName().replace(".sha256.part", "");
            output.write((sha256 + "  " + modelName + "\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
    }
}
