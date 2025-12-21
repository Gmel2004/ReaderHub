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

            // Проверяем, что файл действительно FB2 (XML), а не ZIP (EPUB)
            FileInputStream fis = new FileInputStream(file);
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            fis.close();
            
            if (bytesRead >= 2) {
                // Проверяем сигнатуру ZIP (PK - это начало ZIP архива)
                if (header[0] == 0x50 && header[1] == 0x4B) {
                    Log.e(TAG, "File appears to be a ZIP archive (EPUB), not FB2! File: " + filePath);
                    return book;
                }
                // Проверяем сигнатуру XML (<?xml)
                if (header[0] != 0x3C && header[1] != 0x3F) {
                    // Не начинается с <?, возможно есть BOM или пробелы
                    // Проверяем дальше
                    fis = new FileInputStream(file);
                    byte[] moreBytes = new byte[100];
                    int moreRead = fis.read(moreBytes);
                    fis.close();
                    String start = new String(moreBytes, 0, Math.min(moreRead, 100), "UTF-8").trim();
                    if (!start.startsWith("<") && !start.startsWith("<?xml")) {
                        Log.e(TAG, "File does not appear to be valid XML/FB2! File: " + filePath);
                        return book;
                    }
                }
            }
            
            // Открываем файл заново для парсинга
            fis = new FileInputStream(file);

            // Пробуем сначала без namespace-aware
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false); // Отключаем namespace для более простого парсинга
            dbFactory.setIgnoringElementContentWhitespace(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fis);

            doc.getDocumentElement().normalize();
            
            Log.d(TAG, "Document root: " + doc.getDocumentElement().getNodeName());
            Log.d(TAG, "Root children count: " + doc.getDocumentElement().getChildNodes().getLength());

            // Parse title - пробуем разные варианты
            NodeList titleNodes = doc.getElementsByTagName("book-title");
            Log.d(TAG, "book-title nodes found: " + titleNodes.getLength());
            
            // Также пробуем найти в description
            if (titleNodes.getLength() == 0) {
                Element root = doc.getDocumentElement();
                NodeList descNodes = root.getElementsByTagName("description");
                if (descNodes.getLength() > 0) {
                    Element descElement = (Element) descNodes.item(0);
                    titleNodes = descElement.getElementsByTagName("book-title");
                    Log.d(TAG, "book-title in description: " + titleNodes.getLength());
                }
            }
            
            if (titleNodes.getLength() > 0) {
                book.title = titleNodes.item(0).getTextContent().trim();
                Log.d(TAG, "Title found: " + book.title);
            } else {
                Log.w(TAG, "Title not found, trying alternative methods");
                // Последняя попытка - поиск в любом месте
                titleNodes = doc.getElementsByTagName("*");
                for (int i = 0; i < titleNodes.getLength(); i++) {
                    Node node = titleNodes.item(i);
                    if (node.getNodeName().contains("title") && node.getTextContent() != null) {
                        String text = node.getTextContent().trim();
                        if (!text.isEmpty() && text.length() < 200) { // Разумная длина для названия
                            book.title = text;
                            Log.d(TAG, "Title found by wildcard: " + book.title);
                            break;
                        }
                    }
                }
            }

            // Parse author - пробуем разные варианты
            NodeList authorNodes = doc.getElementsByTagName("author");
            Log.d(TAG, "author nodes found: " + authorNodes.getLength());
            
            // Ищем в description/title-info
            if (authorNodes.getLength() == 0) {
                Element root = doc.getDocumentElement();
                NodeList descNodes = root.getElementsByTagName("description");
                if (descNodes.getLength() > 0) {
                    Element descElement = (Element) descNodes.item(0);
                    NodeList titleInfoNodes = descElement.getElementsByTagName("title-info");
                    if (titleInfoNodes.getLength() > 0) {
                        Element titleInfoElement = (Element) titleInfoNodes.item(0);
                        authorNodes = titleInfoElement.getElementsByTagName("author");
                        Log.d(TAG, "author in title-info: " + authorNodes.getLength());
                    }
                }
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

            // Parse body sections - пробуем разные способы поиска
            Element bodyElement = null;
            
            // Способ 1: прямой поиск по тегу
            NodeList bodyNodes = doc.getElementsByTagName("body");
            Log.d(TAG, "Body nodes found by tag: " + bodyNodes.getLength());
            
            if (bodyNodes.getLength() > 0) {
                bodyElement = (Element) bodyNodes.item(0);
                Log.d(TAG, "Body found by tag name: " + bodyElement.getNodeName());
            }
            
            // Способ 2: поиск в дочерних элементах корневого элемента
            if (bodyElement == null) {
                Element root = doc.getDocumentElement();
                Log.d(TAG, "Root element: " + root.getNodeName());
                NodeList children = root.getChildNodes();
                Log.d(TAG, "Root has " + children.getLength() + " child nodes");
                
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = child.getNodeName();
                        Log.d(TAG, "Child node " + i + ": " + nodeName);
                        if (nodeName.equals("body") || nodeName.endsWith(":body") || nodeName.toLowerCase().contains("body")) {
                            bodyElement = (Element) child;
                            Log.d(TAG, "Body found in root children: " + nodeName);
                            break;
                        }
                    }
                }
            }
            
            // Способ 3: поиск через getElementsByTagName("*") и фильтрация
            if (bodyElement == null) {
                NodeList allNodes = doc.getElementsByTagName("*");
                Log.d(TAG, "Total elements in document: " + allNodes.getLength());
                for (int i = 0; i < allNodes.getLength(); i++) {
                    Node node = allNodes.item(i);
                    String nodeName = node.getNodeName().toLowerCase();
                    if (nodeName.equals("body") || nodeName.endsWith(":body")) {
                        bodyElement = (Element) node;
                        Log.d(TAG, "Body found by wildcard search: " + node.getNodeName());
                        break;
                    }
                }
            }
            
            if (bodyElement != null) {
                Log.d(TAG, "Body element found, parsing content...");
                Log.d(TAG, "Body has " + bodyElement.getChildNodes().getLength() + " child nodes");
                
                // Сначала пробуем найти секции
                NodeList sectionNodes = bodyElement.getElementsByTagName("section");
                if (sectionNodes.getLength() == 0) {
                    sectionNodes = bodyElement.getElementsByTagNameNS("*", "section");
                }
                Log.d(TAG, "Found " + sectionNodes.getLength() + " sections");
                
                // Если есть секции, парсим их
                if (sectionNodes.getLength() > 0) {
                    for (int i = 0; i < sectionNodes.getLength(); i++) {
                        Node sectionNode = sectionNodes.item(i);
                        if (sectionNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element sectionElement = (Element) sectionNode;
                            // Проверяем, что это прямая дочерняя секция body
                            Node parent = sectionElement.getParentNode();
                            if (parent == bodyElement || 
                                (parent != null && (parent.getNodeName().equals("body") || parent.getNodeName().endsWith(":body")))) {
                                String chapterContent = parseSection(sectionElement);
                                if (chapterContent != null && !chapterContent.trim().isEmpty()) {
                                    book.chapters.add(chapterContent);
                                    Log.d(TAG, "Added section " + i + ", length: " + chapterContent.length());
                                }
                            }
                        }
                    }
                }
                
                // Если секций нет или они пустые, пробуем рекурсивный парсинг body
                if (book.chapters.isEmpty()) {
                    Log.d(TAG, "No sections found, trying recursive body parsing");
                    String bodyContent = parseBodyRecursive(bodyElement);
                    if (bodyContent != null && !bodyContent.trim().isEmpty()) {
                        book.chapters.add(bodyContent);
                        Log.d(TAG, "Added body content recursively, length: " + bodyContent.length());
                    } else {
                        // Пробуем прямой парсинг
                        Log.d(TAG, "Recursive parsing failed, trying direct parsing");
                        String bodyText = parseBodyDirectly(bodyElement);
                        if (bodyText != null && !bodyText.trim().isEmpty()) {
                            book.chapters.add(bodyText);
                            Log.d(TAG, "Added body content directly, length: " + bodyText.length());
                        } else {
                            // Последняя попытка - получить весь текст из body
                            Log.d(TAG, "Direct parsing failed, getting all text");
                            String allText = getAllTextFromElement(bodyElement);
                            if (allText != null && !allText.trim().isEmpty()) {
                                // Разбиваем на параграфы
                                String[] paragraphs = allText.split("\n\n|\r\n\r\n");
                                StringBuilder finalContent = new StringBuilder();
                                for (String para : paragraphs) {
                                    if (para != null && !para.trim().isEmpty()) {
                                        finalContent.append("<p>").append(escapeHtml(para.trim())).append("</p>");
                                    }
                                }
                                if (finalContent.length() > 0) {
                                    book.chapters.add(finalContent.toString());
                                    Log.d(TAG, "Added all text from body, split into " + paragraphs.length + " paragraphs");
                                }
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Body not found! Root element: " + doc.getDocumentElement().getNodeName());
                
                // Выводим структуру документа для отладки
                Log.d(TAG, "Document structure:");
                printNodeStructure(doc.getDocumentElement(), 0);
                
                // Пробуем найти body через все возможные способы
                Element root = doc.getDocumentElement();
                
                // Пробуем найти любой элемент с текстом
                NodeList allElements = root.getElementsByTagName("*");
                Log.d(TAG, "Trying to find content in " + allElements.getLength() + " elements");
                
                StringBuilder allText = new StringBuilder();
                for (int i = 0; i < allElements.getLength(); i++) {
                    Element elem = (Element) allElements.item(i);
                    String tagName = elem.getTagName().toLowerCase();
                    
                    // Пропускаем служебные теги
                    if (tagName.equals("description") || tagName.equals("title-info") || 
                        tagName.equals("document-info") || tagName.equals("publish-info") ||
                        tagName.equals("stylesheet") || tagName.equals("binary")) {
                        continue;
                    }
                    
                    // Ищем параграфы и секции везде
                    if (tagName.equals("p") || tagName.endsWith(":p")) {
                        String text = elem.getTextContent();
                        if (text != null && !text.trim().isEmpty() && text.trim().length() > 5) {
                            allText.append("<p>").append(escapeHtml(text.trim())).append("</p>");
                            Log.d(TAG, "Found paragraph in element " + i + ": " + text.trim().substring(0, Math.min(50, text.trim().length())));
                        }
                    } else if (tagName.equals("section") || tagName.endsWith(":section")) {
                        String sectionText = parseSection(elem);
                        if (sectionText != null && !sectionText.trim().isEmpty()) {
                            allText.append(sectionText);
                            Log.d(TAG, "Found section in element " + i);
                        }
                    } else if (tagName.equals("body") || tagName.endsWith(":body")) {
                        // Если нашли body таким способом, парсим его
                        String bodyText = parseBodyRecursive(elem);
                        if (bodyText != null && !bodyText.trim().isEmpty()) {
                            allText.append(bodyText);
                            Log.d(TAG, "Found body element and parsed it");
                        } else {
                            // Пробуем прямой парсинг
                            String directText = parseBodyDirectly(elem);
                            if (directText != null && !directText.trim().isEmpty()) {
                                allText.append(directText);
                                Log.d(TAG, "Found body element and parsed it directly");
                            }
                        }
                    }
                }
                
                if (allText.length() > 0) {
                    book.chapters.add(allText.toString());
                    Log.d(TAG, "Added content from all elements, length: " + allText.length());
                } else {
                    // Последняя попытка - получить весь текст из документа
                    String allContent = getAllTextFromElement(root);
                    if (allContent != null && !allContent.trim().isEmpty()) {
                        // Разбиваем на параграфы если текст длинный
                        String[] paragraphs = allContent.split("\n\n|\r\n\r\n");
                        StringBuilder finalContent = new StringBuilder();
                        for (String para : paragraphs) {
                            if (para != null && !para.trim().isEmpty() && para.trim().length() > 10) {
                                finalContent.append("<p>").append(escapeHtml(para.trim())).append("</p>");
                            }
                        }
                        if (finalContent.length() > 0) {
                            book.chapters.add(finalContent.toString());
                            Log.d(TAG, "Added all text from document as fallback, split into paragraphs");
                        } else {
                            Log.e(TAG, "No meaningful text content found in document!");
                        }
                    } else {
                        Log.e(TAG, "No text content found in document!");
                    }
                }
            }

            Log.d(TAG, "Parsed " + book.chapters.size() + " chapters");
            
            // Если chapters пустой, делаем последнюю попытку найти любой текст в документе
            if (book.chapters.isEmpty()) {
                Log.w(TAG, "No chapters found! Trying to extract any text from document...");
                Element root = doc.getDocumentElement();
                
                // Пробуем найти все элементы с текстом, исключая метаданные
                NodeList allNodes = root.getElementsByTagName("*");
                StringBuilder fallbackContent = new StringBuilder();
                
                for (int i = 0; i < allNodes.getLength(); i++) {
                    Element elem = (Element) allNodes.item(i);
                    String tagName = elem.getTagName().toLowerCase();
                    
                    // Пропускаем все метаданные и служебные теги
                    if (tagName.equals("description") || tagName.equals("title-info") || 
                        tagName.equals("document-info") || tagName.equals("publish-info") ||
                        tagName.equals("stylesheet") || tagName.equals("binary") ||
                        tagName.equals("book-title") || tagName.equals("author") ||
                        tagName.equals("first-name") || tagName.equals("last-name") ||
                        tagName.equals("middle-name") || tagName.equals("fictionbook")) {
                        continue;
                    }
                    
                    // Получаем текст из элемента
                    String elemText = elem.getTextContent();
                    if (elemText != null) {
                        elemText = elemText.trim();
                        // Проверяем, что это не метаданные (короткие строки обычно метаданные)
                        if (elemText.length() > 20 && !elemText.matches("^[\\d\\s\\-:]+$")) {
                            // Проверяем, что это не дубликат уже добавленного текста
                            if (fallbackContent.length() == 0 || !fallbackContent.toString().contains(elemText.substring(0, Math.min(50, elemText.length())))) {
                                fallbackContent.append("<p>").append(escapeHtml(elemText)).append("</p>");
                                Log.d(TAG, "Added fallback text from element " + tagName + ", length: " + elemText.length());
                            }
                        }
                    }
                }
                
                if (fallbackContent.length() > 0) {
                    book.chapters.add(fallbackContent.toString());
                    Log.d(TAG, "Added fallback content, total length: " + fallbackContent.length());
                } else {
                    Log.e(TAG, "Could not extract any meaningful content from FB2 file!");
                }
            }
            
            fis.close();

        } catch (Exception e) {
            Log.e(TAG, "Error parsing FB2", e);
            e.printStackTrace();
        }

        return book;
    }

    private static String parseBodyRecursive(Element element) {
        if (element == null) return "";
        
        StringBuilder content = new StringBuilder();
        NodeList children = element.getChildNodes();
        
        Log.d(TAG, "parseBodyRecursive: processing " + children.getLength() + " child nodes");
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    content.append("<p>").append(escapeHtml(text.trim())).append("</p>");
                    Log.d(TAG, "Added text node: " + text.trim().substring(0, Math.min(50, text.trim().length())));
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String nodeName = childElement.getNodeName().toLowerCase();
                
                Log.d(TAG, "Processing element: " + nodeName);
                
                if (nodeName.equals("p") || nodeName.endsWith(":p")) {
                    String paragraph = childElement.getTextContent();
                    if (paragraph != null && !paragraph.trim().isEmpty()) {
                        content.append("<p>").append(escapeHtml(paragraph.trim())).append("</p>");
                        Log.d(TAG, "Added paragraph: " + paragraph.trim().substring(0, Math.min(50, paragraph.trim().length())));
                    }
                } else if (nodeName.equals("section") || nodeName.endsWith(":section")) {
                    // Если есть секции как прямые дочерние элементы
                    String sectionContent = parseSection(childElement);
                    if (sectionContent != null && !sectionContent.trim().isEmpty()) {
                        content.append(sectionContent);
                        Log.d(TAG, "Added section content, length: " + sectionContent.length());
                    }
                } else if (nodeName.equals("title") || nodeName.endsWith(":title")) {
                    String title = childElement.getTextContent();
                    if (title != null && !title.trim().isEmpty()) {
                        content.append("<h2>").append(escapeHtml(title.trim())).append("</h2>");
                    }
                } else {
                    // Рекурсивно обрабатываем другие элементы
                    String childContent = parseBodyRecursive(childElement);
                    if (childContent != null && !childContent.trim().isEmpty()) {
                        content.append(childContent);
                    } else {
                        // Если рекурсивный парсинг не дал результата, пробуем получить текст напрямую
                        String directText = childElement.getTextContent();
                        if (directText != null && !directText.trim().isEmpty()) {
                            // Разбиваем на параграфы если текст длинный
                            String[] lines = directText.split("\n");
                            for (String line : lines) {
                                if (line != null && !line.trim().isEmpty()) {
                                    content.append("<p>").append(escapeHtml(line.trim())).append("</p>");
                                }
                            }
                            Log.d(TAG, "Added direct text from element " + nodeName + ": " + directText.trim().substring(0, Math.min(50, directText.trim().length())));
                        }
                    }
                }
            }
        }
        
        String result = content.toString();
        Log.d(TAG, "parseBodyRecursive result length: " + result.length());
        return result;
    }

    private static String parseBodyDirectly(Element bodyElement) {
        StringBuilder content = new StringBuilder();
        
        // Пробуем найти параграфы
        NodeList pNodes = bodyElement.getElementsByTagName("p");
        if (pNodes.getLength() == 0) {
            pNodes = bodyElement.getElementsByTagNameNS("*", "p");
        }
        
        Log.d(TAG, "Found " + pNodes.getLength() + " paragraphs");
        
        // Парсим прямые дочерние элементы body
        NodeList children = bodyElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals("p") || nodeName.endsWith(":p")) {
                    String paragraph = child.getTextContent();
                    if (paragraph != null && !paragraph.trim().isEmpty()) {
                        content.append("<p>").append(escapeHtml(paragraph)).append("</p>");
                    }
                } else if (nodeName.equals("section") || nodeName.endsWith(":section")) {
                    // Если есть секции как прямые дочерние элементы
                    String sectionContent = parseSection((Element) child);
                    if (sectionContent != null && !sectionContent.trim().isEmpty()) {
                        content.append(sectionContent);
                    }
                }
            }
        }
        
        // Если ничего не нашли, пробуем через getElementsByTagName
        if (content.length() == 0 && pNodes.getLength() > 0) {
            for (int i = 0; i < pNodes.getLength(); i++) {
                String paragraph = pNodes.item(i).getTextContent();
                if (paragraph != null && !paragraph.trim().isEmpty()) {
                    content.append("<p>").append(escapeHtml(paragraph)).append("</p>");
                }
            }
        }
        
        return content.toString();
    }

    private static String getAllTextFromElement(Element element) {
        if (element == null) return null;
        
        // Пробуем получить текст через getTextContent
        String text = element.getTextContent();
        if (text != null) {
            text = text.trim();
            // Убираем лишние пробелы и табы, но сохраняем переносы строк
            text = text.replaceAll("[ \\t]+", " "); // Заменяем множественные пробелы и табы на один пробел
            text = text.replaceAll("\\n\\s*\\n", "\n\n"); // Сохраняем двойные переносы строк
        }
        
        // Если текст слишком короткий или пустой, пробуем рекурсивно собрать текст из дочерних элементов
        if (text == null || text.trim().isEmpty() || text.trim().length() < 10) {
            StringBuilder sb = new StringBuilder();
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String childText = child.getTextContent();
                    if (childText != null && !childText.trim().isEmpty()) {
                        sb.append(childText.trim()).append(" ");
                    }
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElem = (Element) child;
                    String tagName = childElem.getTagName().toLowerCase();
                    // Пропускаем служебные теги
                    if (!tagName.equals("description") && !tagName.equals("title-info") && 
                        !tagName.equals("document-info") && !tagName.equals("publish-info") &&
                        !tagName.equals("stylesheet") && !tagName.equals("binary") &&
                        !tagName.equals("book-title") && !tagName.equals("author") &&
                        !tagName.equals("first-name") && !tagName.equals("last-name") &&
                        !tagName.equals("middle-name")) {
                        String childText = childElem.getTextContent();
                        if (childText != null && !childText.trim().isEmpty() && childText.trim().length() > 5) {
                            sb.append(childText.trim()).append(" ");
                        }
                    }
                }
            }
            String collectedText = sb.toString().trim();
            if (collectedText.length() > text.length()) {
                text = collectedText;
            }
        }
        
        return text;
    }

    private static void printNodeStructure(Node node, int depth) {
        if (depth > 3) return; // Ограничиваем глубину для читаемости
        
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String nodeName = element.getNodeName();
            int childCount = 0;
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    childCount++;
                }
            }
            Log.d(TAG, indent.toString() + nodeName + " (" + childCount + " element children)");
            
            // Показываем первые несколько дочерних элементов
            if (depth < 2) {
                for (int i = 0; i < Math.min(children.getLength(), 10); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        printNodeStructure(child, depth + 1);
                    }
                }
            }
        }
    }

    private static String parseSection(Element section) {
        if (section == null) return "";
        
        StringBuilder content = new StringBuilder();

        // Parse title - пробуем разные варианты
        NodeList titleNodes = section.getElementsByTagName("title");
        if (titleNodes.getLength() == 0) {
            titleNodes = section.getElementsByTagNameNS("*", "title");
        }
        if (titleNodes.getLength() > 0) {
            String titleText = titleNodes.item(0).getTextContent();
            if (titleText != null && !titleText.trim().isEmpty()) {
                content.append("<h2>")
                        .append(escapeHtml(titleText.trim()))
                        .append("</h2>");
            }
        }

        // Парсим прямые дочерние элементы секции
        NodeList children = section.getChildNodes();
        Log.d(TAG, "parseSection: processing " + children.getLength() + " child nodes");
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String nodeName = childElement.getNodeName().toLowerCase();
                
                Log.d(TAG, "Section child element: " + nodeName);
                
                // Пропускаем title, так как уже обработали
                if (nodeName.equals("title") || nodeName.endsWith(":title")) {
                    continue;
                }
                
                // Парсим параграфы
                if (nodeName.equals("p") || nodeName.endsWith(":p")) {
                    String paragraph = childElement.getTextContent();
                    if (paragraph != null && !paragraph.trim().isEmpty()) {
                        content.append("<p>").append(escapeHtml(paragraph.trim())).append("</p>");
                        Log.d(TAG, "Added paragraph from section: " + paragraph.trim().substring(0, Math.min(50, paragraph.trim().length())));
                    }
                }
                // Парсим вложенные секции
                else if (nodeName.equals("section") || nodeName.endsWith(":section")) {
                    String nestedContent = parseSection(childElement);
                    if (nestedContent != null && !nestedContent.trim().isEmpty()) {
                        content.append(nestedContent);
                    }
                }
                // Обрабатываем другие элементы (например, стихи, эпиграфы и т.д.)
                else {
                    // Пробуем получить весь текст из элемента
                    String childText = childElement.getTextContent();
                    if (childText != null && !childText.trim().isEmpty()) {
                        // Разбиваем на параграфы если текст длинный
                        String[] lines = childText.split("\n");
                        for (String line : lines) {
                            if (line != null && !line.trim().isEmpty()) {
                                content.append("<p>").append(escapeHtml(line.trim())).append("</p>");
                            }
                        }
                        Log.d(TAG, "Added text from element " + nodeName + ": " + childText.trim().substring(0, Math.min(50, childText.trim().length())));
                    }
                }
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                // Обрабатываем текстовые узлы напрямую
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    content.append("<p>").append(escapeHtml(text.trim())).append("</p>");
                }
            }
        }

        // Fallback: если ничего не нашли через дочерние элементы, пробуем getElementsByTagName
        if (content.length() == 0) {
            Log.d(TAG, "No content found via child nodes, trying getElementsByTagName");
            NodeList pNodes = section.getElementsByTagName("p");
            if (pNodes.getLength() == 0) {
                pNodes = section.getElementsByTagNameNS("*", "p");
            }
            Log.d(TAG, "Found " + pNodes.getLength() + " paragraphs via getElementsByTagName");
            for (int i = 0; i < pNodes.getLength(); i++) {
                String paragraph = pNodes.item(i).getTextContent();
                if (paragraph != null && !paragraph.trim().isEmpty()) {
                    content.append("<p>").append(escapeHtml(paragraph.trim())).append("</p>");
                }
            }
            
            // Если все еще пусто, получаем весь текст
            if (content.length() == 0) {
                String allText = section.getTextContent();
                if (allText != null && !allText.trim().isEmpty()) {
                    String[] paragraphs = allText.split("\n\n|\r\n\r\n");
                    for (String para : paragraphs) {
                        if (para != null && !para.trim().isEmpty()) {
                            content.append("<p>").append(escapeHtml(para.trim())).append("</p>");
                        }
                    }
                    Log.d(TAG, "Added all text from section, split into " + paragraphs.length + " paragraphs");
                }
            }
        }

        String result = content.toString();
        Log.d(TAG, "parseSection result length: " + result.length());
        return result;
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