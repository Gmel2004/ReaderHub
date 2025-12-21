package com.example.readerhub.parsers;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Fb2Parser {

    private static final String TAG = "Fb2Parser";

    public static class Fb2Book {
        public String title;
        public String author;
        public List<String> chapters;
        public String coverImage;

        public Fb2Book() {
            chapters = new ArrayList<>();
        }
    }

    public static Fb2Book parse(String filePath) {
        Fb2Book book = new Fb2Book();

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "FB2 file not found: " + filePath);
                return book;
            }

            FileInputStream fis = new FileInputStream(file);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fis);

            doc.getDocumentElement().normalize();

            // Parse title - пробуем разные варианты
            NodeList titleNodes = doc.getElementsByTagName("book-title");
            if (titleNodes.getLength() == 0) {
                // Пробуем без namespace
                titleNodes = doc.getElementsByTagNameNS("*", "book-title");
            }
            if (titleNodes.getLength() > 0) {
                book.title = titleNodes.item(0).getTextContent().trim();
                Log.d(TAG, "Title found: " + book.title);
            } else {
                Log.w(TAG, "Title not found");
            }

            // Parse author - пробуем разные варианты
            NodeList authorNodes = doc.getElementsByTagName("author");
            if (authorNodes.getLength() == 0) {
                authorNodes = doc.getElementsByTagNameNS("*", "author");
            }
            if (authorNodes.getLength() > 0) {
                Element authorElement = (Element) authorNodes.item(0);
                String firstName = getTagValue("first-name", authorElement);
                String lastName = getTagValue("last-name", authorElement);
                String middleName = getTagValue("middle-name", authorElement);
                
                StringBuilder authorBuilder = new StringBuilder();
                if (firstName != null && !firstName.isEmpty()) {
                    authorBuilder.append(firstName);
                }
                if (middleName != null && !middleName.isEmpty()) {
                    if (authorBuilder.length() > 0) authorBuilder.append(" ");
                    authorBuilder.append(middleName);
                }
                if (lastName != null && !lastName.isEmpty()) {
                    if (authorBuilder.length() > 0) authorBuilder.append(" ");
                    authorBuilder.append(lastName);
                }
                
                book.author = authorBuilder.toString().trim();
                if (book.author.isEmpty()) {
                    // Пробуем получить текст напрямую
                    book.author = authorElement.getTextContent().trim();
                }
                Log.d(TAG, "Author found: " + book.author);
            } else {
                Log.w(TAG, "Author not found");
            }

            // Parse cover image (binary data)
            NodeList binaryNodes = doc.getElementsByTagName("binary");
            for (int i = 0; i < binaryNodes.getLength(); i++) {
                Element binaryElement = (Element) binaryNodes.item(i);
                String contentType = binaryElement.getAttribute("content-type");
                String id = binaryElement.getAttribute("id");
                
                // Проверяем, является ли это обложкой
                if (contentType != null && contentType.startsWith("image/") && 
                    (id.contains("cover") || id.contains("coverimage"))) {
                    String imageData = binaryElement.getTextContent();
                    if (imageData != null && !imageData.isEmpty()) {
                        book.coverImage = imageData;
                    }
                }
            }

            // Parse body sections
            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() == 0) {
                bodyNodes = doc.getElementsByTagNameNS("*", "body");
            }
            
            if (bodyNodes.getLength() > 0) {
                Element bodyElement = (Element) bodyNodes.item(0);
                NodeList sectionNodes = bodyElement.getElementsByTagName("section");
                if (sectionNodes.getLength() == 0) {
                    sectionNodes = bodyElement.getElementsByTagNameNS("*", "section");
                }
                
                Log.d(TAG, "Found " + sectionNodes.getLength() + " sections");
                
                for (int i = 0; i < sectionNodes.getLength(); i++) {
                    Node sectionNode = sectionNodes.item(i);

                    if (sectionNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element sectionElement = (Element) sectionNode;
                        String chapterContent = parseSection(sectionElement);
                        if (chapterContent != null && !chapterContent.trim().isEmpty()) {
                            book.chapters.add(chapterContent);
                        }
                    }
                }
                
                // Если секций нет, пробуем получить весь текст из body
                if (book.chapters.isEmpty()) {
                    String bodyText = parseBodyDirectly(bodyElement);
                    if (bodyText != null && !bodyText.trim().isEmpty()) {
                        book.chapters.add(bodyText);
                    }
                }
            } else {
                Log.w(TAG, "Body not found");
            }

            Log.d(TAG, "Parsed " + book.chapters.size() + " chapters");
            fis.close();

        } catch (Exception e) {
            Log.e(TAG, "Error parsing FB2", e);
            e.printStackTrace();
        }

        return book;
    }

    private static String parseBodyDirectly(Element bodyElement) {
        StringBuilder content = new StringBuilder();
        NodeList pNodes = bodyElement.getElementsByTagName("p");
        if (pNodes.getLength() == 0) {
            pNodes = bodyElement.getElementsByTagNameNS("*", "p");
        }
        
        for (int i = 0; i < pNodes.getLength(); i++) {
            String paragraph = pNodes.item(i).getTextContent();
            if (paragraph != null && !paragraph.trim().isEmpty()) {
                content.append("<p>").append(escapeHtml(paragraph)).append("</p>");
            }
        }
        
        return content.toString();
    }

    private static String parseSection(Element section) {
        StringBuilder content = new StringBuilder();

        // Parse title
        NodeList titleNodes = section.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            content.append("<h2>")
                    .append(escapeHtml(titleNodes.item(0).getTextContent()))
                    .append("</h2>");
        }

        // Parse paragraphs
        NodeList pNodes = section.getElementsByTagName("p");
        for (int i = 0; i < pNodes.getLength(); i++) {
            String paragraph = pNodes.item(i).getTextContent();
            content.append("<p>").append(escapeHtml(paragraph)).append("</p>");
        }

        // Parse nested sections
        NodeList nestedSections = section.getElementsByTagName("section");
        for (int i = 0; i < nestedSections.getLength(); i++) {
            Node nestedNode = nestedSections.item(i);
            if (nestedNode.getParentNode() == section && nestedNode.getNodeType() == Node.ELEMENT_NODE) {
                content.append(parseSection((Element) nestedNode));
            }
        }

        return content.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                return node.getTextContent();
            }
        }
        return "";
    }

    public static String convertToHtml(Fb2Book book) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: serif; line-height: 1.6; padding: 20px; max-width: 800px; margin: 0 auto; }");
        html.append("h1 { text-align: center; }");
        html.append("h2 { margin-top: 30px; }");
        html.append("p { text-indent: 2em; margin: 10px 0; }");
        html.append("</style>");
        html.append("</head><body>");

        if (book.title != null && !book.title.isEmpty()) {
            html.append("<h1>").append(escapeHtml(book.title)).append("</h1>");
        } else {
            html.append("<h1>Без названия</h1>");
        }
        
        if (book.author != null && !book.author.isEmpty() && !book.author.trim().equals("null")) {
            html.append("<p style='text-align:center;'><i>").append(escapeHtml(book.author)).append("</i></p>");
        }

        if (book.chapters != null && !book.chapters.isEmpty()) {
            for (String chapter : book.chapters) {
                if (chapter != null && !chapter.isEmpty()) {
                    html.append(chapter);
                }
            }
        } else {
            html.append("<p>Содержимое не найдено</p>");
        }

        html.append("</body></html>");

        return html.toString();
    }
}