package com.gamebridge.mobile;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Inventories every file in an imported archive and mirrors the packager's game-root selection. */
final class GameSourceInventory {
    private static final Pattern DRIVE_PATH = Pattern.compile("^[A-Za-z]:");
    private static final Pattern IMAGE = Pattern.compile("\\.(png|jpe?g|webp|gif|bmp|avif|svg)$");
    private static final Pattern AUDIO_VIDEO = Pattern.compile("\\.(ogg|mp3|wav|flac|opus|m4a|mp4|webm|ogv|mov)$");

    private GameSourceInventory() { }
    static final class Result {
        final boolean isRenPy;
        final int totalFiles, includedFiles, excludedFiles, scriptFiles, archiveFiles, imageFiles, audioVideoFiles, webFiles, otherFiles;
        final long includedBytes, runtimeBytes;
        final String sourceRoot;
        Result(boolean renpy,int total,int included,int scripts,int archives,int images,int av,int web,int other,long bytes,long runtime,String root) {
            isRenPy=renpy; totalFiles=total; includedFiles=included; excludedFiles=total-included;
            scriptFiles=scripts; archiveFiles=archives; imageFiles=images; audioVideoFiles=av; webFiles=web;
            otherFiles=other; includedBytes=bytes; runtimeBytes=runtime; sourceRoot=root;
        }
        long workBytes(long inputArchiveBytes) {
            long input=Math.max(0,inputArchiveBytes);
            long data=includedBytes>Long.MAX_VALUE-runtimeBytes?Long.MAX_VALUE:includedBytes+runtimeBytes;
            return data>Long.MAX_VALUE-input?Long.MAX_VALUE:data+input;
        }
        long estimatedApkBytes(){return includedBytes>Long.MAX_VALUE-runtimeBytes?Long.MAX_VALUE:includedBytes+runtimeBytes;}
        String summary() {
            return "Scanned "+totalFiles+" files · "+includedFiles+" included · "+excludedFiles+" outside/hidden · "
                    +scriptFiles+" scripts · "+archiveFiles+" archives · "+imageFiles+" images · "+audioVideoFiles+" audio/video · "+webFiles+" web files · "+otherFiles+" other";
        }
    }

    private static final class Item { final String path; final long bytes; Item(String p,long b){path=p;bytes=b;} }
    static Result scan(File archive,long runtimeBytes) throws Exception {
        List<Item> files=new ArrayList<>();
        try(ZipFile zip=new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement(); if(entry.isDirectory()) continue;
                String path=entry.getName().replace('\\','/');
                if(path.startsWith("/")||DRIVE_PATH.matcher(path).find()||hasParentSegment(path))
                    throw new IllegalArgumentException("The selected archive contains an unsafe file path: "+path);
                long bytes=entry.getSize()>=0?entry.getSize():Math.max(0,entry.getCompressedSize());
                files.add(new Item(path,bytes));
            }
        }
        String gameRoot=findRenPyGameRoot(files);
        boolean renpy=gameRoot!=null;
        String root=renpy?gameRoot:findHtmlRoot(files);
        int included=0,scripts=0,archives=0,images=0,av=0,web=0,other=0; long bytes=0;
        for(Item item:files) {
            String relative;
            if(renpy) {
                if(!item.path.startsWith(root)) continue;
                relative=item.path.substring(root.length());
                if(relative.isEmpty()||hasHiddenSegment(relative)) continue;
            } else {
                if(root==null||!item.path.startsWith(root)) continue;
                relative=item.path.substring(root.length()); if(relative.isEmpty()) continue;
            }
            included++; String lower=relative.toLowerCase(Locale.ROOT);
            if(lower.endsWith(".rpy")||lower.endsWith(".rpyc")||lower.endsWith(".py")||lower.endsWith(".js")||lower.endsWith(".mjs")||lower.endsWith(".css")) scripts++;
            else if(lower.endsWith(".rpa")||lower.endsWith(".zip")||lower.endsWith(".pak")||lower.endsWith(".obb")) archives++;
            else if(IMAGE.matcher(lower).find()) images++;
            else if(AUDIO_VIDEO.matcher(lower).find()) av++;
            else if(lower.endsWith(".html")||lower.endsWith(".htm")) web++;
            else other++;
            bytes=bytes>Long.MAX_VALUE-item.bytes?Long.MAX_VALUE:bytes+item.bytes;
        }
        return new Result(renpy,files.size(),included,scripts,archives,images,av,web,other,bytes,renpy?runtimeBytes:21_459L,root);
    }
    private static String findRenPyGameRoot(List<Item> files) {
        for(Item item:files) {
            String lower=item.path.toLowerCase(Locale.ROOT);
            if(!(lower.endsWith(".rpyc")||lower.endsWith(".rpa")||lower.endsWith(".rpy"))) continue;
            String[] parts=item.path.split("/");
            for(int i=0;i<parts.length-1;i++) if(parts[i].equalsIgnoreCase("game")) {
                boolean compiled=lower.endsWith(".rpyc")||lower.endsWith(".rpa");
                if(compiled) { StringBuilder root=new StringBuilder(); for(int j=0;j<=i;j++) root.append(parts[j]).append('/'); return root.toString(); }
            }
        }
        return null;
    }
    private static String findHtmlRoot(List<Item> files) {
        String[] common=null; int length=0;
        for(Item item:files) {
            String[] parts=item.path.split("/"); int directories=parts.length-1;
            if(common==null) {common=parts;length=directories;continue;}
            int shared=0; while(shared<length&&shared<directories&&common[shared].equals(parts[shared])) shared++;
            length=shared;
        }
        if(common==null||length==0)return "";
        StringBuilder root=new StringBuilder(); for(int i=0;i<length;i++)root.append(common[i]).append('/');
        return root.toString();
    }
    private static boolean hasHiddenSegment(String path) { for(String part:path.split("/")) if(part.startsWith(".")) return true; return false; }
    private static boolean hasParentSegment(String path) { for(String part:path.split("/")) if(part.equals("..")) return true; return false; }
}
