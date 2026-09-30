package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.sackcentury.shinebuttonlib.ShineButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BooksHorizontalRecyclerViewAdapter extends RecyclerView.Adapter<BooksHorizontalRecyclerViewAdapter.BooksHorizontalRecyclerViewViewHolder> {

    Context context;
    ArrayList<Book> books;

    public BooksHorizontalRecyclerViewAdapter(Context context, ArrayList<Book> books) {
        this.context = context;
        this.books = books;
    }

    public void setBooks(ArrayList<Book> books) {
        this.books = books != null ? new ArrayList<>(books) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BooksHorizontalRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        BookMotion.attachPressEffect(view);
        return new BooksHorizontalRecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BooksHorizontalRecyclerViewViewHolder holder, int position) {
        Book book = books.get(position);
        holder.bookName.setText(book.getName());
        holder.bookName.setTextColor(MyColor.getTitleTextColor(context));
        holder.bookImage.setContentDescription(book.getName());

        try {
            InputStream ims = context.getAssets().open("covers/" + book.getBookCover() + ".png");
            Drawable d = Drawable.createFromStream(ims, null);
            holder.bookImage.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            holder.bookImage.setImageResource(R.drawable.cover_placeholder);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookOpener.open(context, book);
            }
        });

        holder.itemView.setOnLongClickListener(null);

        bindLibraryStar(holder, book);
    }

    private void bindLibraryStar(BooksHorizontalRecyclerViewViewHolder holder, Book book) {
        boolean saved = LibraryRepository.getInstance(holder.itemView.getContext()).isFavorite(book);
        holder.shineButton.setClickable(false);
        holder.shineButton.setFocusable(false);
        holder.shineButton.setChecked(saved);
        holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                saved ? R.string.remove_from_library : R.string.save_to_library) + ": " + book.getName());
        holder.likeHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !holder.shineButton.isChecked();
                holder.shineButton.setChecked(next, true);
                holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                        next ? R.string.remove_from_library : R.string.save_to_library) + ": " + book.getName());
                LibraryRepository libraryRepository = LibraryRepository.getInstance(holder.itemView.getContext());
                if (next) {
                    libraryRepository.addFavorite(book);

                    String addedString = holder.itemView.getContext().getResources().getString(R.string.Library_added);
                    DialogChrome.snack(holder.itemView, addedString + ": " + book.getName());
                } else {
                    libraryRepository.removeFavorite(book);
                    String removedString = holder.itemView.getContext().getResources().getString(R.string.Library_removed);
                    DialogChrome.snack(holder.itemView, removedString + ": " + book.getName());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public class BooksHorizontalRecyclerViewViewHolder extends RecyclerView.ViewHolder {

        TextView bookName;
        ImageView bookImage;
        ShineButton shineButton;
        View likeHost;

        public BooksHorizontalRecyclerViewViewHolder(@NonNull View itemView) {
            super(itemView);
            bookName = itemView.findViewById(R.id.book_name);
            bookImage = itemView.findViewById(R.id.book_image);
            shineButton = itemView.findViewById(R.id.like);
            likeHost = itemView.findViewById(R.id.like_host);
        }
    }

}
