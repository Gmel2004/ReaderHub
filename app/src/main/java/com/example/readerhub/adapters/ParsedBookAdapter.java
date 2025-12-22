package com.example.readerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.readerhub.R;
import com.example.readerhub.parsers.WebBookParser;

import java.util.ArrayList;
import java.util.List;

public class ParsedBookAdapter extends RecyclerView.Adapter<ParsedBookAdapter.ViewHolder> {

    private Context context;
    private List<WebBookParser.ParsedBook> books;
    private OnParsedBookClickListener listener;

    public interface OnParsedBookClickListener {
        void onDownloadClick(WebBookParser.ParsedBook book);
    }

    public ParsedBookAdapter(Context context, OnParsedBookClickListener listener) {
        this.context = context;
        this.books = new ArrayList<>();
        this.listener = listener;
    }

    public void setBooks(List<WebBookParser.ParsedBook> books) {
        this.books = books;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parsed_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WebBookParser.ParsedBook book = books.get(position);

        // Устанавливаем название книги
        if (book.title != null && !book.title.trim().isEmpty()) {
            holder.titleTextView.setText(book.title.trim());
            holder.titleTextView.setVisibility(View.VISIBLE);
            android.util.Log.d("ParsedBookAdapter", "Setting title: " + book.title);
        } else {
            holder.titleTextView.setVisibility(View.GONE);
            android.util.Log.w("ParsedBookAdapter", "Title is null or empty for book at position " + position);
        }

        // Скрываем автора, если он "Unknown Author" или пустой
        if (book.author != null && !book.author.trim().isEmpty() && 
            !book.author.equals("Unknown Author") && !book.author.equals("Unknown")) {
            holder.authorTextView.setText(book.author);
            holder.authorTextView.setVisibility(View.VISIBLE);
        } else {
            holder.authorTextView.setVisibility(View.GONE);
        }

        holder.fileTypeTextView.setText(book.fileType != null ? book.fileType : "Unknown");

        // Показываем количество глав
        if (book.chapters > 0) {
            holder.chaptersTextView.setText(book.chapters + " глав");
            holder.chaptersTextView.setVisibility(View.VISIBLE);
        } else {
            holder.chaptersTextView.setVisibility(View.GONE);
        }

        if (book.description != null && !book.description.isEmpty()) {
            holder.descriptionTextView.setText(book.description);
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        if (book.coverUrl != null && !book.coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(R.drawable.ic_book_placeholder);
        }

        holder.downloadButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(book);
            }
        });

        // Клик по всей карточке тоже работает
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView authorTextView;
        TextView fileTypeTextView;
        TextView chaptersTextView;
        TextView descriptionTextView;
        Button downloadButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            fileTypeTextView = itemView.findViewById(R.id.fileTypeTextView);
            chaptersTextView = itemView.findViewById(R.id.chaptersTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }
    }
}