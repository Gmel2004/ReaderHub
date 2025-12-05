package com.example.readerhub.parsers;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebBookParser {

    private static final String TAG = "WebBookParser";

    public interface OnBooksParseListener {
        void onParseComplete(List<ParsedBook> books);
        void onParseError(String error);
    }

    public static class ParsedBook {
        public String title;
        public String author;
        public String downloadUrl;
        public String coverUrl;
        public String description;
        public String fileType;
        public String bookUrl; // Ссылка на страницу книги
        public int chapters; // Количество глав

        public ParsedBook(String title, String author, String bookUrl) {
            this.title = title;
            this.author = author;
            this.bookUrl = bookUrl;
        }
    }

    // Парсинг главной страницы или каталога ranobe.me
    public void parseRanobeCatalog(String url, OnBooksParseListener listener) {
        new ParseRanobeTask(url, listener).execute();
    }

    // Парсинг конкретной книги для получения подробной информации
    public void parseRanobeBookDetails(String bookUrl, OnBookDetailsListener listener) {
        new ParseBookDetailsTask(bookUrl, listener).execute();
    }

    public interface OnBookDetailsListener {
        void onDetailsLoaded(ParsedBook book, List<Chapter> chapters);
        void onError(String error);
    }

    public static class Chapter {
        public String title;
        public String url;
        public int number;

        public Chapter(int number, String title, String url) {
            this.number = number;
            this.title = title;
            this.url = url;
        }
    }

    private static class ParseRanobeTask extends AsyncTask<Void, Void, List<ParsedBook>> {
        private String url;
        private OnBooksParseListener listener;
        private String error;

        public ParseRanobeTask(String url, OnBooksParseListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        protected List<ParsedBook> doInBackground(Void... voids) {
            List<ParsedBook> books = new ArrayList<>();

            try {
                Log.d(TAG, "Parsing URL: " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15000)
                        .get();

                // ranobe.me использует класс "short-cont" для карточек книг
                Elements bookCards = doc.select(".short-cont");

                Log.d(TAG, "Found " + bookCards.size() + " book cards");

                for (Element card : bookCards) {
                    try {
                        // Извлекаем заголовок и ссылку
                        Element titleElement = card.selectFirst("h2 a, .sh-title a");
                        if (titleElement == null) continue;

                        String title = titleElement.text().trim();
                        String bookUrl = titleElement.attr("abs:href");

                        // Извлекаем автора
                        Element authorElement = card.selectFirst(".sh-author");
                        String author = authorElement != null ?
                                authorElement.text().replace("Автор:", "").trim() :
                                "Unknown Author";

                        ParsedBook book = new ParsedBook(title, author, bookUrl);

                        // Извлекаем обложку
                        Element coverElement = card.selectFirst(".short-img img");
                        if (coverElement != null) {
                            book.coverUrl = coverElement.attr("abs:src");
                            // Если src пустой, пробуем data-src (lazy loading)
                            if (book.coverUrl.isEmpty()) {
                                book.coverUrl = coverElement.attr("abs:data-src");
                            }
                        }

                        // Извлекаем краткое описание
                        Element descElement = card.selectFirst(".sh-desc, .short-desk");
                        if (descElement != null) {
                            book.description = descElement.text().trim();
                            // Ограничиваем длину описания
                            if (book.description.length() > 200) {
                                book.description = book.description.substring(0, 200) + "...";
                            }
                        }

                        // Извлекаем количество глав
                        Element chaptersElement = card.selectFirst(".sh-chapters");
                        if (chaptersElement != null) {
                            String chaptersText = chaptersElement.text();
                            try {
                                book.chapters = Integer.parseInt(
                                        chaptersText.replaceAll("[^0-9]", "")
                                );
                            } catch (NumberFormatException e) {
                                book.chapters = 0;
                            }
                        }

                        book.fileType = "RANOBE"; // Веб-формат

                        books.add(book);
                        Log.d(TAG, "Parsed book: " + title);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing book card", e);
                    }
                }

            } catch (IOException e) {
                error = "Network error: " + e.getMessage();
                Log.e(TAG, "Network error", e);
                return null;
            } catch (Exception e) {
                error = "Parsing error: " + e.getMessage();
                Log.e(TAG, "Parsing error", e);
                return null;
            }

            return books;
        }

        @Override
        protected void onPostExecute(List<ParsedBook> books) {
            if (books != null && listener != null) {
                listener.onParseComplete(books);
            } else if (listener != null) {
                listener.onParseError(error != null ? error : "Unknown error");
            }
        }
    }

    private static class ParseBookDetailsTask extends AsyncTask<Void, Void, ParsedBook> {
        private String bookUrl;
        private OnBookDetailsListener listener;
        private String error;
        private List<Chapter> chapters;

        public ParseBookDetailsTask(String bookUrl, OnBookDetailsListener listener) {
            this.bookUrl = bookUrl;
            this.listener = listener;
            this.chapters = new ArrayList<>();
        }

        @Override
        protected ParsedBook doInBackground(Void... voids) {
            try {
                Log.d(TAG, "Parsing book details: " + bookUrl);

                Document doc = Jsoup.connect(bookUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15000)
                        .get();

                // Извлекаем основную информацию о книге
                String title = doc.selectFirst(".book-title, h1").text().trim();

                Element authorElement = doc.selectFirst(".book-author");
                String author = authorElement != null ?
                        authorElement.text().replace("Автор:", "").trim() :
                        "Unknown Author";

                ParsedBook book = new ParsedBook(title, author, bookUrl);

                // Обложка
                Element coverElement = doc.selectFirst(".book-img img");
                if (coverElement != null) {
                    book.coverUrl = coverElement.attr("abs:src");
                }

                // Полное описание
                Element descElement = doc.selectFirst(".book-description, .full-description");
                if (descElement != null) {
                    book.description = descElement.text().trim();
                }

                // Парсим список глав
                Elements chapterElements = doc.select(".chapter-item, .chapters-list li");

                Log.d(TAG, "Found " + chapterElements.size() + " chapters");

                int chapterNum = 1;
                for (Element chapterElement : chapterElements) {
                    Element linkElement = chapterElement.selectFirst("a");
                    if (linkElement != null) {
                        String chapterTitle = linkElement.text().trim();
                        String chapterUrl = linkElement.attr("abs:href");

                        chapters.add(new Chapter(chapterNum++, chapterTitle, chapterUrl));
                    }
                }

                book.chapters = chapters.size();

                return book;

            } catch (IOException e) {
                error = "Network error: " + e.getMessage();
                Log.e(TAG, "Network error", e);
                return null;
            } catch (Exception e) {
                error = "Parsing error: " + e.getMessage();
                Log.e(TAG, "Parsing error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(ParsedBook book) {
            if (book != null && listener != null) {
                listener.onDetailsLoaded(book, chapters);
            } else if (listener != null) {
                listener.onError(error != null ? error : "Unknown error");
            }
        }
    }

    // Парсинг содержимого главы
    public void parseChapterContent(String chapterUrl, OnChapterParseListener listener) {
        new ParseChapterTask(chapterUrl, listener).execute();
    }

    public interface OnChapterParseListener {
        void onChapterParsed(String title, String content);
        void onError(String error);
    }

    private static class ParseChapterTask extends AsyncTask<Void, Void, String[]> {
        private String chapterUrl;
        private OnChapterParseListener listener;
        private String error;

        public ParseChapterTask(String chapterUrl, OnChapterParseListener listener) {
            this.chapterUrl = chapterUrl;
            this.listener = listener;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            try {
                Log.d(TAG, "Parsing chapter: " + chapterUrl);

                Document doc = Jsoup.connect(chapterUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15000)
                        .get();

                String title = doc.selectFirst(".chapter-title, h1").text().trim();

                // Извлекаем текст главы
                Element contentElement = doc.selectFirst(".chapter-content, .text");

                if (contentElement == null) {
                    error = "Chapter content not found";
                    return null;
                }

                // Очищаем от рекламы и лишних элементов
                contentElement.select(".ads, .advertisement, script").remove();

                // Получаем HTML содержимое для сохранения форматирования
                String content = contentElement.html();

                return new String[]{title, content};

            } catch (IOException e) {
                error = "Network error: " + e.getMessage();
                Log.e(TAG, "Network error", e);
                return null;
            } catch (Exception e) {
                error = "Parsing error: " + e.getMessage();
                Log.e(TAG, "Parsing error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null && listener != null) {
                listener.onChapterParsed(result[0], result[1]);
            } else if (listener != null) {
                listener.onError(error != null ? error : "Unknown error");
            }
        }
    }

    // Утилита для проверки, является ли URL с ranobe.me
    public static boolean isRanobeUrl(String url) {
        return url != null && url.contains("ranobe.me");
    }
}