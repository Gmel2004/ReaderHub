package com.example.readerhub.parsers;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class EpubParser {

    private static final String TAG = "EpubParser";

    public static class EpubBook {
        public String title;
        public String author;
        public List<String> chapters;
        public byte[] coverImage;
        public String coverImagePath;

        public EpubBook() {
            chapters = new ArrayList<>();
        }
    }

    public static EpubBook parse(String filePath) {
        EpubBook book = new EpubBook();

        try {
            File epubFile = new File(filePath);
            if (!epubFile.exists()) {
                Log.e(TAG, "EPUB file not found: " + filePath);
                return book;
            }

            ZipFile zipFile = new ZipFile(epubFile);

            // Парсим метаданные из OPF файла
            ZipEntry opfEntry = findOpfEntry(zipFile);
            if (opfEntry != null) {
                parseOpf(zipFile, opfEntry, book);
            }

            // Ищем обложку
            findCoverImage(zipFile, book);

            // Парсим spine (порядок глав)
            if (opfEntry != null) {
                parseSpine(zipFile, opfEntry, book);
            }

            zipFile.close();

        } catch (Exception e) {
            Log.e(TAG, "Error parsing EPUB", e);
            e.printStackTrace();
        }

        return book;
    }

    private static ZipEntry findOpfEntry(ZipFile zipFile) {
        // Ищем container.xml
        ZipEntry containerEntry = zipFile.getEntry("META-INF/container.xml");
        if (containerEntry != null) {
            try {
                InputStream is = zipFile.getInputStream(containerEntry);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();

                NodeList rootfileNodes = doc.getElementsByTagName("rootfile");
                if (rootfileNodes.getLength() > 0) {
                    Element rootfileElement = (Element) rootfileNodes.item(0);
                    String fullPath = rootfileElement.getAttribute("full-path");
                    if (fullPath != null && !fullPath.isEmpty()) {
                        ZipEntry opfEntry = zipFile.getEntry(fullPath);
                        if (opfEntry != null) {
                            return opfEntry;
                        }
                    }
                }
                is.close();
            } catch (Exception e) {
                Log.e(TAG, "Error reading container.xml", e);
            }
        }

        // Fallback: ищем любой .opf файл
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".opf")) {
                return entry;
            }
        }

        return null;
    }

    private static void parseOpf(ZipFile zipFile, ZipEntry opfEntry, EpubBook book) {
        try {
            InputStream is = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // Парсим метаданные - пробуем разные варианты namespace
            NodeList titleNodes = doc.getElementsByTagName("dc:title");
            if (titleNodes.getLength() == 0) {
                // Пробуем без namespace
                titleNodes = doc.getElementsByTagNameNS("*", "title");
            }
            if (titleNodes.getLength() == 0) {
                // Пробуем просто title
                titleNodes = doc.getElementsByTagName("title");
            }
            if (titleNodes.getLength() > 0) {
                book.title = titleNodes.item(0).getTextContent().trim();
                Log.d(TAG, "Title found: " + book.title);
            } else {
                Log.w(TAG, "Title not found");
            }

            NodeList creatorNodes = doc.getElementsByTagName("dc:creator");
            if (creatorNodes.getLength() == 0) {
                creatorNodes = doc.getElementsByTagNameNS("*", "creator");
            }
            if (creatorNodes.getLength() == 0) {
                creatorNodes = doc.getElementsByTagName("creator");
            }
            if (creatorNodes.getLength() > 0) {
                book.author = creatorNodes.item(0).getTextContent().trim();
                Log.d(TAG, "Author found: " + book.author);
            } else {
                Log.w(TAG, "Author not found");
            }

            // Ищем обложку в метаданных
            NodeList metaNodes = doc.getElementsByTagName("meta");
            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element metaElement = (Element) metaNodes.item(i);
                String name = metaElement.getAttribute("name");
                if ("cover".equals(name)) {
                    String content = metaElement.getAttribute("content");
                    if (content != null && !content.isEmpty()) {
                        book.coverImagePath = content;
                    }
                }
            }

            is.close();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing OPF", e);
        }
    }

    private static void findCoverImage(ZipFile zipFile, EpubBook book) {
        try {
            // Если путь к обложке найден в метаданных
            if (book.coverImagePath != null) {
                ZipEntry coverEntry = zipFile.getEntry(book.coverImagePath);
                if (coverEntry == null) {
                    // Пробуем найти относительно корня
                    String opfDir = "";
                    int lastSlash = book.coverImagePath.lastIndexOf('/');
                    if (lastSlash > 0) {
                        opfDir = book.coverImagePath.substring(0, lastSlash + 1);
                    }
                    coverEntry = zipFile.getEntry(opfDir + book.coverImagePath);
                }
                if (coverEntry != null) {
                    InputStream is = zipFile.getInputStream(coverEntry);
                    book.coverImage = readAllBytes(is);
                    is.close();
                    return;
                }
            }

            // Ищем изображения вручную
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().toLowerCase();
                if (                    (name.contains("cover") || name.contains("title")) && 
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                    InputStream is = zipFile.getInputStream(entry);
                    book.coverImage = readAllBytes(is);
                    book.coverImagePath = entry.getName();
                    is.close();
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding cover image", e);
        }
    }

    private static void parseSpine(ZipFile zipFile, ZipEntry opfEntry, EpubBook book) {
        try {
            InputStream is = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // Получаем manifest (список всех ресурсов)
            java.util.Map<String, String> manifest = new java.util.HashMap<>();
            NodeList itemNodes = doc.getElementsByTagName("item");
            String opfDir = "";
            int lastSlash = opfEntry.getName().lastIndexOf('/');
            if (lastSlash > 0) {
                opfDir = opfEntry.getName().substring(0, lastSlash + 1);
            }

            Log.d(TAG, "OPF directory: " + opfDir);
            Log.d(TAG, "Found " + itemNodes.getLength() + " items in manifest");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);
                String id = itemElement.getAttribute("id");
                String href = itemElement.getAttribute("href");
                if (id != null && href != null) {
                    // Нормализуем путь
                    String fullPath = opfDir + href;
                    // Убираем двойные слеши
                    fullPath = fullPath.replace("//", "/");
                    manifest.put(id, fullPath);
                    Log.d(TAG, "Manifest: id=" + id + ", href=" + fullPath);
                }
            }

            // Парсим spine (порядок глав)
            // Ищем элемент spine
            Element spineElement = null;
            NodeList spineNodes = doc.getElementsByTagName("spine");
            if (spineNodes.getLength() == 0) {
                spineNodes = doc.getElementsByTagNameNS("*", "spine");
            }
            if (spineNodes.getLength() > 0) {
                spineElement = (Element) spineNodes.item(0);
                Log.d(TAG, "Spine element found");
            } else {
                Log.w(TAG, "Spine element not found!");
            }
            
            NodeList itemrefNodes = null;
            if (spineElement != null) {
                itemrefNodes = spineElement.getElementsByTagName("itemref");
                if (itemrefNodes.getLength() == 0) {
                    itemrefNodes = spineElement.getElementsByTagNameNS("*", "itemref");
                }
            } else {
                // Fallback: ищем itemref во всем документе
                itemrefNodes = doc.getElementsByTagName("itemref");
                if (itemrefNodes.getLength() == 0) {
                    itemrefNodes = doc.getElementsByTagNameNS("*", "itemref");
                }
            }
            
            Log.d(TAG, "Found " + (itemrefNodes != null ? itemrefNodes.getLength() : 0) + " itemrefs in spine");
            
            if (itemrefNodes != null && itemrefNodes.getLength() > 0) {
                for (int i = 0; i < itemrefNodes.getLength(); i++) {
                    Element itemrefElement = (Element) itemrefNodes.item(i);
                    String idref = itemrefElement.getAttribute("idref");
                    String href = manifest.get(idref);
                    
                    if (href != null) {
                        Log.d(TAG, "Loading chapter: idref=" + idref + ", href=" + href);
                        
                        // Пробуем разные варианты пути
                        ZipEntry chapterEntry = zipFile.getEntry(href);
                        if (chapterEntry == null && !opfDir.isEmpty() && href.startsWith(opfDir)) {
                            // Убираем opfDir если он есть в начале
                            String relativePath = href.substring(opfDir.length());
                            chapterEntry = zipFile.getEntry(relativePath);
                        }
                        if (chapterEntry == null && !href.startsWith("/")) {
                            // Пробуем с абсолютным путем от корня
                            chapterEntry = zipFile.getEntry("/" + href);
                        }
                        if (chapterEntry == null) {
                            // Пробуем найти файл по имени
                            String fileName = href.substring(href.lastIndexOf('/') + 1);
                            Enumeration<? extends ZipEntry> entries = zipFile.entries();
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                if (entry.getName().endsWith(fileName)) {
                                    chapterEntry = entry;
                                    Log.d(TAG, "Found chapter by filename: " + entry.getName());
                                    break;
                                }
                            }
                        }
                        
                        if (chapterEntry != null) {
                            InputStream chapterIs = zipFile.getInputStream(chapterEntry);
                            byte[] content = readAllBytes(chapterIs);
                            chapterIs.close();
                            String htmlContent = new String(content, "UTF-8");
                            book.chapters.add(htmlContent);
                            Log.d(TAG, "Chapter " + i + " loaded, size: " + htmlContent.length());
                        } else {
                            Log.w(TAG, "Chapter entry not found: " + href);
                        }
                    } else {
                        Log.w(TAG, "No href found for idref: " + idref);
                    }
                }
            } else {
                Log.w(TAG, "No itemrefs found! Trying to find HTML/XHTML files directly...");
                // Fallback: ищем HTML/XHTML файлы напрямую
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName().toLowerCase();
                    if ((name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) 
                        && !name.contains("nav") && !name.contains("toc") && !name.contains("cover")) {
                        try {
                            InputStream chapterIs = zipFile.getInputStream(entry);
                            byte[] content = readAllBytes(chapterIs);
                            chapterIs.close();
                            String htmlContent = new String(content, "UTF-8");
                            book.chapters.add(htmlContent);
                            Log.d(TAG, "Found chapter file directly: " + entry.getName() + ", size: " + htmlContent.length());
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading chapter file: " + entry.getName(), e);
                        }
                    }
                }
            }

            Log.d(TAG, "Total chapters parsed: " + book.chapters.size());
            is.close();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing spine", e);
            e.printStackTrace();
        }
    }

    public static String convertToHtml(EpubBook book) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: 'Georgia', serif; line-height: 1.8; padding: 20px; max-width: 800px; margin: 0 auto; background: #f5f5f5; }");
        html.append("h1, h2, h3 { color: #2c3e50; margin-top: 30px; }");
        html.append("p { text-indent: 2em; margin: 10px 0; text-align: justify; }");
        html.append("img { max-width: 100%; height: auto; }");
        html.append("</style>");
        html.append("</head><body>");

        if (book.title != null && !book.title.isEmpty()) {
            html.append("<h1>").append(escapeHtml(book.title)).append("</h1>");
        }
        if (book.author != null && !book.author.isEmpty()) {
            html.append("<p style='text-align:center;'><i>").append(escapeHtml(book.author)).append("</i></p>");
        }

        for (String chapter : book.chapters) {
            html.append(chapter);
        }

        html.append("</body></html>");

        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}

