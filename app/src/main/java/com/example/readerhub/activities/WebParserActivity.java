package com.example.readerhub.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readerhub.R;
import com.example.readerhub.adapters.ParsedBookAdapter;
import com.example.readerhub.models.Book;
import com.example.readerhub.parsers.WebBookParser;
import com.example.readerhub.repository.BookRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class WebParserActivity extends AppCompatActivity
        implements WebBookParser.OnBooksParseListener,
        ParsedBookAdapter.OnParsedBookClickListener {

    private EditText urlEditText;
    private Button parseButton;
    private Button quickRanobeButton;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ParsedBookAdapter adapter;
    private WebBookParser parser;
    private BookRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_parser);

        parser = new WebBookParser();
        repository = new BookRepository(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        urlEditText = findViewById(R.id.urlEditText);
        parseButton = findViewById(R.id.parseButton);
        quickRanobeButton = findViewById(R.id.quickRanobeButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new ParsedBookAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Устанавливаем дефолтный URL для ranobe.me
        urlEditText.setHint("https://ranobe.me/");
    }

    private void setupListeners() {
        parseButton.setOnClickListener(v -> startParsing());

        // Быстрый доступ к популярным разделам ranobe.me
        quickRanobeButton.setOnClickListener(v -> showQuickLinks());
    }

    private void showQuickLinks() {
        String[] options = {
                "Новинки",
                "Популярные",
                "Топ за неделю",
                "Русские ранобэ",
                "Корейские ранобэ",
                "Японские ранобэ",
                "Китайские ранобэ"
        };

        String[] urls = {
                "https://ranobe.me/novels",
                "https://ranobe.me/novels?sort=popular",
                "https://ranobe.me/novels?sort=week",
                "https://ranobe.me/novels?country=russia",
                "https://ranobe.me/novels?country=korea",
                "https://ranobe.me/novels?country=japan",
                "https://ranobe.me/novels?country=china"
        };

        new AlertDialog.Builder(this)
                .setTitle("Быстрый переход")
                .setItems(options, (dialog, which) -> {
                    urlEditText.setText(urls[which]);
                    startParsing();
                })
                .show();
    }

    private void startParsing() {
        String url = urlEditText.getText().toString().trim();

        if (url.isEmpty()) {
            urlEditText.setError("Введите URL");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        setLoading(true);

        // Определяем тип парсинга
        if (WebBookParser.isRanobeUrl(url)) {
            parser.parseRanobeCatalog(url, this);
        } else {
            Toast.makeText(this,
                    "Этот парсер настроен для ranobe.me. Другие сайты могут не работать.",
                    Toast.LENGTH_LONG).show();
            parser.parseRanobeCatalog(url, this);
        }
    }

    @Override
    public void onParseComplete(List<WebBookParser.ParsedBook> books) {
        runOnUiThread(() -> {
            setLoading(false);

            if (books.isEmpty()) {
                Toast.makeText(this, "Книги не найдены", Toast.LENGTH_SHORT).show();
            } else {
                adapter.setBooks(books);
                Toast.makeText(this,
                        "Найдено книг: " + books.size(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onParseError(String error) {
        runOnUiThread(() -> {
            setLoading(false);
            new AlertDialog.Builder(this)
                    .setTitle("Ошибка парсинга")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onDownloadClick(WebBookParser.ParsedBook parsedBook) {
        new AlertDialog.Builder(this)
                .setTitle(parsedBook.title)
                .setMessage("Что вы хотите сделать?")
                .setPositiveButton("Открыть детали", (dialog, which) ->
                        openBookDetails(parsedBook))
                .setNegativeButton("Скачать все главы", (dialog, which) ->
                        downloadAllChapters(parsedBook))
                .setNeutralButton("Отмена", null)
                .show();
    }

    private void openBookDetails(WebBookParser.ParsedBook parsedBook) {
        setLoading(true);

        parser.parseRanobeBookDetails(parsedBook.bookUrl,
                new WebBookParser.OnBookDetailsListener() {
                    @Override
                    public void onDetailsLoaded(WebBookParser.ParsedBook book,
                                                List<WebBookParser.Chapter> chapters) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showBookDetailsDialog(book, chapters);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(WebParserActivity.this,
                                    "Ошибка: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void showBookDetailsDialog(WebBookParser.ParsedBook book,
                                       List<WebBookParser.Chapter> chapters) {
        String message = "Автор: " + book.author + "\n" +
                "Глав: " + chapters.size() + "\n\n" +
                (book.description != null ? book.description : "");

        new AlertDialog.Builder(this)
                .setTitle(book.title)
                .setMessage(message)
                .setPositiveButton("Скачать всё", (dialog, which) ->
                        downloadAllChapters(book))
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void downloadAllChapters(WebBookParser.ParsedBook parsedBook) {
        Toast.makeText(this, "Начинаем скачивание глав...", Toast.LENGTH_SHORT).show();
        setLoading(true);

        // Сначала получаем список всех глав
        parser.parseRanobeBookDetails(parsedBook.bookUrl,
                new WebBookParser.OnBookDetailsListener() {
                    @Override
                    public void onDetailsLoaded(WebBookParser.ParsedBook book,
                                                List<WebBookParser.Chapter> chapters) {
                        // Скачиваем главы и создаём HTML-файл
                        downloadChaptersSequentially(book, chapters, 0, new StringBuilder());
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(WebParserActivity.this,
                                    "Ошибка: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void downloadChaptersSequentially(WebBookParser.ParsedBook book,
                                              List<WebBookParser.Chapter> chapters,
                                              int index,
                                              StringBuilder fullContent) {
        if (index >= chapters.size()) {
            // Все главы скачаны, сохраняем книгу
            saveBookToDatabase(book, fullContent.toString());
            return;
        }

        WebBookParser.Chapter chapter = chapters.get(index);

        runOnUiThread(() ->
                Toast.makeText(this,
                        "Скачивание главы " + (index + 1) + "/" + chapters.size(),
                        Toast.LENGTH_SHORT).show()
        );

        parser.parseChapterContent(chapter.url,
                new WebBookParser.OnChapterParseListener() {
                    @Override
                    public void onChapterParsed(String title, String content) {
                        fullContent.append("<h2>").append(title).append("</h2>");
                        fullContent.append(content);
                        fullContent.append("<hr/>");

                        // Скачиваем следующую главу
                        downloadChaptersSequentially(book, chapters, index + 1, fullContent);
                    }

                    @Override
                    public void onError(String error) {
                        // Пропускаем ошибочную главу и продолжаем
                        downloadChaptersSequentially(book, chapters, index + 1, fullContent);
                    }
                });
    }

    private void saveBookToDatabase(WebBookParser.ParsedBook parsedBook, String htmlContent) {
        try {
            // Создаём HTML файл
            String fileName = parsedBook.title.replaceAll("[^a-zA-Zа-яА-Я0-9]", "_") + ".html";
            File booksDir = new File(getFilesDir(), "books");
            if (!booksDir.exists()) {
                booksDir.mkdirs();
            }

            File bookFile = new File(booksDir, fileName);

            // Формируем полный HTML документ
            String fullHtml = createHtmlDocument(parsedBook, htmlContent);

            FileOutputStream fos = new FileOutputStream(bookFile);
            fos.write(fullHtml.getBytes("UTF-8"));
            fos.close();

            // Сохраняем в базу данных
            Book book = new Book(
                    parsedBook.title,
                    parsedBook.author,
                    bookFile.getAbsolutePath(),
                    "HTML"
            );
            book.setCoverUrl(parsedBook.coverUrl);
            book.setTotalPages(parsedBook.chapters);

            repository.insertBook(book, bookId ->
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Книга успешно добавлена в библиотеку!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
            );

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(this,
                        "Ошибка сохранения: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String createHtmlDocument(WebBookParser.ParsedBook book, String content) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ru\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + book.title + "</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: 'Georgia', serif;\n" +
                "            line-height: 1.8;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "            background: #f5f5f5;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            text-align: center;\n" +
                "            color: #333;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .author {\n" +
                "            text-align: center;\n" +
                "            color: #666;\n" +
                "            font-style: italic;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        h2 {\n" +
                "            color: #2c3e50;\n" +
                "            margin-top: 40px;\n" +
                "            border-bottom: 2px solid #3498db;\n" +
                "            padding-bottom: 10px;\n" +
                "        }\n" +
                "        p {\n" +
                "            text-align: justify;\n" +
                "            text-indent: 2em;\n" +
                "            margin: 15px 0;\n" +
                "        }\n" +
                "        hr {\n" +
                "            margin: 30px 0;\n" +
                "            border: none;\n" +
                "            border-top: 1px solid #ddd;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>" + book.title + "</h1>\n" +
                "    <p class=\"author\">Автор: " + book.author + "</p>\n" +
                content +
                "</body>\n" +
                "</html>";
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            parseButton.setEnabled(!isLoading);
            quickRanobeButton.setEnabled(!isLoading);
            urlEditText.setEnabled(!isLoading);
        });
    }
}