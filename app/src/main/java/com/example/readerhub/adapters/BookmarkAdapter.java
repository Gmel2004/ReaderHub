package com.example.readerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readerhub.R;
import com.example.readerhub.models.Bookmark;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private Context context;
    private List<Bookmark> bookmarks;
    private OnBookmarkDeleteListener listener;

    public interface OnBookmarkDeleteListener {
        void onDelete(Bookmark bookmark);
    }

    public BookmarkAdapter(Context context, OnBookmarkDeleteListener listener) {
        this.context = context;
        this.bookmarks = new ArrayList<>();
        this.listener = listener;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bookmark, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);

        holder.chapterTextView.setText(bookmark.getChapterName());
        holder.pageTextView.setText("Page " + bookmark.getPageNumber());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.dateTextView.setText(sdf.format(new Date(bookmark.getCreatedAt())));

        if (bookmark.getNote() != null && !bookmark.getNote().isEmpty()) {
            holder.noteTextView.setText(bookmark.getNote());
            holder.noteTextView.setVisibility(View.VISIBLE);
        } else {
            holder.noteTextView.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(bookmark);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTextView, pageTextView, dateTextView, noteTextView;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTextView = itemView.findViewById(R.id.chapterTextView);
            pageTextView = itemView.findViewById(R.id.pageTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}