package com.example.readerhub.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.readerhub.parsers.Fb2Parser;
import com.example.readerhub.parsers.EpubParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BookCoverExtractor {

    /**
     * Извлекает обложку из EPUB файла и сохраняет её локально
     * @return Путь к сохраненному файлу обложки или null
     */
    public static String extractEpubCover(Context context, String epubPath) {
        try {
            EpubParser.EpubBook epubBook = EpubParser.parse(epubPath);
            if (epubBook != null && epubBook.coverImage != null && epubBook.coverImage.length > 0) {
                return saveCoverImage(context, epubBook.coverImage, "epub_cover");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Извлекает обложку из FB2 файла и сохраняет её локально
     * @return Путь к сохраненному файлу обложки или null
     */
    public static String extractFb2Cover(Context context, String fb2Path) {
        try {
            Fb2Parser.Fb2Book fb2Book = Fb2Parser.parse(fb2Path);
            if (fb2Book != null && fb2Book.coverImage != null && !fb2Book.coverImage.isEmpty()) {
                // Обложка в FB2 хранится в base64
                byte[] imageData = Base64.decode(fb2Book.coverImage, Base64.DEFAULT);
                return saveCoverImage(context, imageData, "fb2_cover");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Сохраняет изображение обложки локально
     * @return Путь к сохраненному файлу или null
     */
    private static String saveCoverImage(Context context, byte[] imageData, String prefix) {
        try {
            File coversDir = new File(context.getFilesDir(), "covers");
            if (!coversDir.exists()) {
                coversDir.mkdirs();
            }

            String fileName = prefix + "_" + System.currentTimeMillis() + ".jpg";
            File coverFile = new File(coversDir, fileName);

            FileOutputStream fos = new FileOutputStream(coverFile);
            fos.write(imageData);
            fos.close();

            return coverFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Извлекает обложку в зависимости от типа файла
     */
    public static String extractCover(Context context, String filePath, String fileType) {
        if (filePath == null || fileType == null) {
            return null;
        }

        switch (fileType.toUpperCase()) {
            case "EPUB":
                return extractEpubCover(context, filePath);
            case "FB2":
                return extractFb2Cover(context, filePath);
            default:
                return null;
        }
    }
}

