package com.example.readerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.readerhub.R;
import com.example.readerhub.models.Book;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private Context context;
    private List<Book> books;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
        void onBookLongClick(Book book);
    }

    public BookAdapter(Context context, OnBookClickListener listener) {
        this.context = context;
        this.books = new ArrayList<>();
        this.listener = listener;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
        notifyDataSetChanged();
    }

    public void addBook(Book book) {
        books.add(book);
        notifyItemInserted(books.size() - 1);
    }

    public void removeBook(int position) {
        books.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);

        holder.titleTextView.setText(book.getTitle());
        holder.authorTextView.setText(book.getAuthor());
        holder.fileTypeTextView.setText(book.getFileType());

        // Progress
        if (book.getTotalPages() > 0) {
            int progress = (int) ((book.getCurrentPage() / (float) book.getTotalPages()) * 100);
            holder.progressBar.setProgress(progress);
            holder.progressTextView.setText(progress + "%");
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressTextView.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.progressTextView.setVisibility(View.GONE);
        }

        // Last opened
        if (book.getLastOpened() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String date = sdf.format(new Date(book.getLastOpened()));
            holder.lastOpenedTextView.setText("Last read: " + date);
            holder.lastOpenedTextView.setVisibility(View.VISIBLE);
        } else {
            holder.lastOpenedTextView.setVisibility(View.GONE);
        }

        // Favorite icon
        holder.favoriteIcon.setVisibility(book.isFavorite() ? View.VISIBLE : View.GONE);

        // Cover image
        if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(book.getCoverUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(book);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBookLongClick(book);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView authorTextView;
        TextView fileTypeTextView;
        TextView lastOpenedTextView;
        TextView progressTextView;
        ProgressBar progressBar;
        ImageView favoriteIcon;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            fileTypeTextView = itemView.findViewById(R.id.fileTypeTextView);
            lastOpenedTextView = itemView.findViewById(R.id.lastOpenedTextView);
            progressTextView = itemView.findViewById(R.id.progressTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}