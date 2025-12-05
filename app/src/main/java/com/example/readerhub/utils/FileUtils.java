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
}