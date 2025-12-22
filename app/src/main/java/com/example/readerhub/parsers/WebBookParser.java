package com.example.readerhub.parsers;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    // Парсинг поиска ranobe.me
    public void searchRanobe(String query, OnBooksParseListener listener) {
        try {
            // Правильное URL кодирование для поддержки русского языка
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://ranobe.me/index.php?section=search&str=" + encodedQuery;
            new ParseRanobeTask(searchUrl, listener).execute();
        } catch (Exception e) {
            // Fallback на простую замену пробелов, если кодирование не удалось
            String searchUrl = "https://ranobe.me/index.php?section=search&str=" +
                    query.replace(" ", "+");
            new ParseRanobeTask(searchUrl, listener).execute();
        }
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

                // ranobe.me использует класс "FicTable" для карточек книг
                Elements bookCards = doc.select(".FicTable");

                Log.d(TAG, "Found " + bookCards.size() + " book cards");

                for (Element card : bookCards) {
                    try {
                        // Извлекаем заголовок и ссылку
                        Element titleElement = card.selectFirst(".FicTable_Title a");
                        if (titleElement == null) continue;

                        String title = titleElement.text().trim();
                        String bookUrl = titleElement.attr("abs:href");

                        // Извлекаем автора (может не быть на странице поиска)
                        String author = "Unknown Author";

                        ParsedBook book = new ParsedBook(title, author, bookUrl);

                        // Извлекаем обложку
                        Element coverElement = card.selectFirst(".FicTable_Cover img");
                        if (coverElement != null) {
                            book.coverUrl = coverElement.attr("abs:src");
                            if (book.coverUrl.isEmpty()) {
                                book.coverUrl = coverElement.attr("abs:data-src");
                            }
                        }

                        // Извлекаем описание
                        Element descElement = card.selectFirst(".FicTable_Description");
                        if (descElement != null) {
                            // Убираем вложенные элементы жанров
                            Element genresElement = descElement.selectFirst(".FicTable_Genres");
                            if (genresElement != null) {
                                genresElement.remove();
                            }
                            book.description = descElement.text().trim();
                            if (book.description.length() > 200) {
                                book.description = book.description.substring(0, 200) + "...";
                            }
                        }

                        // Извлекаем количество глав
                        Element chaptersElement = card.selectFirst(".ChaptersCount");
                        if (chaptersElement != null) {
                            String chaptersText = chaptersElement.text().trim();
                            try {
                                book.chapters = Integer.parseInt(chaptersText);
                            } catch (NumberFormatException e) {
                                book.chapters = 0;
                            }
                        }

                        book.fileType = "RANOBE";

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
        private String downloadUrl;

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

                // Извлекаем основную информацию из FicHead
                String title = doc.selectFirst("h1").text().trim();

                // Автор
                Element authorElement = doc.selectFirst(".title:contains(Автор:) + .content a");
                String author = authorElement != null ? authorElement.text().trim() : "Unknown Author";

                ParsedBook book = new ParsedBook(title, author, bookUrl);

                // Обложка
                Element coverElement = doc.selectFirst(".FicCover img");
                if (coverElement != null) {
                    book.coverUrl = coverElement.attr("abs:src");
                }

                // Описание
                Element descElement = doc.selectFirst(".summary_text_fic3");
                if (descElement != null) {
                    // Убираем теги
                    descElement.select("a").remove();
                    descElement.select("#more_tags").remove();
                    descElement.select("#more_tags_button").remove();
                    book.description = descElement.text().trim();
                }

                // Количество глав
                Element chaptersElement = doc.selectFirst(".title:contains(Глав в переводе:) + .content");
                if (chaptersElement != null) {
                    String chaptersText = chaptersElement.text().split(" ")[0];
                    try {
                        book.chapters = Integer.parseInt(chaptersText);
                    } catch (NumberFormatException e) {
                        book.chapters = 0;
                    }
                }

                // Жанры
                Element genresElement = doc.selectFirst(".title:contains(Жанры:) + .content");
                if (genresElement != null) {
                    book.description += "\n\nЖанры: " + genresElement.text();
                }

                // Год выпуска
                Element yearElement = doc.selectFirst(".title:contains(Год выпуска:) + .content");
                if (yearElement != null) {
                    book.description += "\nГод: " + yearElement.text();
                }

                // URL для скачивания
                Element downloadElement = doc.selectFirst("a.green[href*='action=download']");
                if (downloadElement != null) {
                    downloadUrl = downloadElement.attr("abs:href");
                }

                // Парсим ссылку на чтение для получения глав
                Element readElement = doc.selectFirst("a.red[href*='/0']");
                if (readElement != null) {
                    String readUrl = readElement.attr("abs:href");
                    // Здесь можно распарсить список глав, но это требует дополнительного запроса
                }

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
                book.downloadUrl = downloadUrl;
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