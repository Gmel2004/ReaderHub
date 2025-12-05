package com.example.readerhub.parsers;

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
            FileInputStream fis = new FileInputStream(file);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fis);

            doc.getDocumentElement().normalize();

            // Parse title
            NodeList titleNodes = doc.getElementsByTagName("book-title");
            if (titleNodes.getLength() > 0) {
                book.title = titleNodes.item(0).getTextContent();
            }

            // Parse author
            NodeList authorNodes = doc.getElementsByTagName("author");
            if (authorNodes.getLength() > 0) {
                Element authorElement = (Element) authorNodes.item(0);
                String firstName = getTagValue("first-name", authorElement);
                String lastName = getTagValue("last-name", authorElement);
                book.author = firstName + " " + lastName;
            }

            // Parse body sections
            NodeList sectionNodes = doc.getElementsByTagName("section");
            for (int i = 0; i < sectionNodes.getLength(); i++) {
                Node sectionNode = sectionNodes.item(i);

                if (sectionNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element sectionElement = (Element) sectionNode;
                    String chapterContent = parseSection(sectionElement);
                    book.chapters.add(chapterContent);
                }
            }

            fis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return book;
    }

    private static String parseSection(Element section) {
        StringBuilder content = new StringBuilder();

        // Parse title
        NodeList titleNodes = section.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            content.append("<h2>")
                    .append(titleNodes.item(0).getTextContent())
                    .append("</h2>");
        }

        // Parse paragraphs
        NodeList pNodes = section.getElementsByTagName("p");
        for (int i = 0; i < pNodes.getLength(); i++) {
            String paragraph = pNodes.item(i).getTextContent();
            content.append("<p>").append(paragraph).append("</p>");
        }

        return content.toString();
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
        html.append("<style>");
        html.append("body { font-family: serif; line-height: 1.6; padding: 20px; }");
        html.append("h1 { text-align: center; }");
        html.append("h2 { margin-top: 30px; }");
        html.append("p { text-indent: 2em; margin: 10px 0; }");
        html.append("</style>");
        html.append("</head><body>");

        html.append("<h1>").append(book.title).append("</h1>");
        html.append("<p style='text-align:center;'><i>").append(book.author).append("</i></p>");

        for (String chapter : book.chapters) {
            html.append(chapter);
        }

        html.append("</body></html>");

        return html.toString();
    }
}