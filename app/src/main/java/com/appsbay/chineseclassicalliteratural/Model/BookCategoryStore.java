package com.appsbay.chineseclassicalliteratural.Model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class BookCategoryStore {
    public static BookCategoryStore shared = new BookCategoryStore();

    public ArrayList<BookCategory> getCategories(Context context) {

        ArrayList<BookCategory> categories = new ArrayList<>();

        ArrayList<Book> siShuWuJing = new ArrayList<>();
        ArrayList<Book> zhuZiBaiJia = new ArrayList<>();
        ArrayList<Book> novel = new ArrayList<>();
        ArrayList<Book> other = new ArrayList<>();

        SharedPreferences preferences = context.getSharedPreferences("Language Preference", Context.MODE_PRIVATE);
        int language = preferences.getInt("language", 0);

        if (language == 0) {
            for (Book book : BookStore.shared.getBooks(context)) {
                if (book.getBookType() == BookType.siShuWuJing) {
                    siShuWuJing.add(book);
                } else if (book.getBookType() == BookType.zhuZiBaiJia) {
                    zhuZiBaiJia.add(book);
                } else if (book.getBookType() == BookType.novel) {
                    novel.add(book);
                } else if (book.getBookType() == BookType.other) {
                    other.add(book);
                }
            }
            categories.add(new BookCategory("四书五经", siShuWuJing));
            categories.add(new BookCategory("诸子百家", zhuZiBaiJia));
            categories.add(new BookCategory("经典小说", novel));
            categories.add(new BookCategory("其他", other));
        } else {
            for (Book book : BookStore.shared.getBooks(context)) {
                if (book.getBookType() == BookType.siShuWuJing_Fan) {
                    siShuWuJing.add(book);
                } else if (book.getBookType() == BookType.zhuZiBaiJia_Fan) {
                    zhuZiBaiJia.add(book);
                } else if (book.getBookType() == BookType.novel_Fan) {
                    novel.add(book);
                } else if (book.getBookType() == BookType.other_Fan) {
                    other.add(book);
                }
            }
            categories.add(new BookCategory("四書五經", siShuWuJing));
            categories.add(new BookCategory("諸子百家", zhuZiBaiJia));
            categories.add(new BookCategory("經典小說", novel));
            categories.add(new BookCategory("其他", other));
        }

        return categories;
    }

    public void setCategories(ArrayList<BookCategory> categories) {
        this.categories = categories;
    }

    public ArrayList<BookCategory> categories;

}
