package com.example.readerhub.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "";

        return fileName.substring(lastDot + 1).toLowerCase();
    }

    public static String getFileType(String extension) {
        switch (extension) {
            case "epub":
                return "EPUB";
            case "pdf":
                return "PDF";
            case "fb2":
                return "FB2";
            default:
                return "UNKNOWN";
        }
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            }
        } else {
            File file = new File(uri.getPath());
            size = file.length();
        }
        return size;
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static File copyFileToInternalStorage(Context context, Uri uri, String fileName) {
        try {
            File booksDir = new File(context.getFilesDir(), "books");
            if (!booksDir.exists()) {
                booksDir.mkdirs();
            }

            File destFile = new File(booksDir, fileName);

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return destFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    /**
     * Определяет реальный тип файла по его содержимому (сигнатуре)
     * @return "EPUB", "FB2", "PDF" или "UNKNOWN"
     */
    public static String detectFileTypeByContent(String filePath) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(filePath);
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            fis.close();
            
            if (bytesRead >= 2) {
                // EPUB/ZIP signature: PK (0x50 0x4B)
                if (header[0] == 0x50 && header[1] == 0x4B) {
                    // Это ZIP архив, нужно проверить содержимое
                    // EPUB должен содержать mimetype файл с содержимым "application/epub+zip"
                    // FB2.ZIP должен содержать .fb2 файл
                    try {
                        java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(filePath);
                        java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
                        
                        boolean hasMimetype = false;
                        boolean hasFb2File = false;
                        boolean hasOpfFile = false;
                        
                        while (entries.hasMoreElements()) {
                            java.util.zip.ZipEntry entry = entries.nextElement();
                            String name = entry.getName().toLowerCase();
                            
                            if (name.equals("mimetype")) {
                                // Проверяем содержимое mimetype
                                java.io.InputStream is = zipFile.getInputStream(entry);
                                byte[] mimetypeBytes = new byte[20];
                                int read = is.read(mimetypeBytes);
                                is.close();
                                if (read > 0) {
                                    String mimetype = new String(mimetypeBytes, 0, read, "UTF-8");
                                    if (mimetype.contains("epub")) {
                                        hasMimetype = true;
                                    }
                                }
                            }
                            
                            if (name.endsWith(".fb2") || name.endsWith(".xml")) {
                                // Проверяем, является ли это FB2 файлом
                                java.io.InputStream is = zipFile.getInputStream(entry);
                                byte[] fb2Header = new byte[100];
                                int read = is.read(fb2Header);
                                is.close();
                                if (read > 0) {
                                    String content = new String(fb2Header, 0, read, "UTF-8");
                                    if (content.contains("FictionBook") || content.contains("fictionbook")) {
                                        hasFb2File = true;
                                    }
                                }
                            }
                            
                            if (name.endsWith(".opf")) {
                                hasOpfFile = true;
                            }
                        }
                        
                        zipFile.close();
                        
                        // EPUB должен иметь mimetype или opf файл
                        if (hasMimetype || hasOpfFile) {
                            return "EPUB";
                        }
                        // FB2.ZIP должен иметь .fb2 файл
                        if (hasFb2File) {
                            return "FB2";
                        }
                        // Если есть opf, но нет mimetype, все равно EPUB
                        if (hasOpfFile) {
                            return "EPUB";
                        }
                    } catch (Exception e) {
                        // Если не удалось открыть как ZIP, возможно это поврежденный файл
                        // Но по сигнатуре это ZIP, поэтому скорее всего EPUB
                        return "EPUB";
                    }
                }
                // PDF signature: %PDF
                if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                    return "PDF";
                }
                // XML/FB2 signature: <? or <FictionBook
                if (header[0] == 0x3C) {
                    // Читаем больше байт для проверки
                    fis = new java.io.FileInputStream(filePath);
                    byte[] moreBytes = new byte[200];
                    int moreRead = fis.read(moreBytes);
                    fis.close();
                    String start = new String(moreBytes, 0, Math.min(moreRead, 200), "UTF-8").trim();
                    if (start.startsWith("<?xml") || start.startsWith("<FictionBook") || start.startsWith("<fictionbook")) {
                        return "FB2";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }
}