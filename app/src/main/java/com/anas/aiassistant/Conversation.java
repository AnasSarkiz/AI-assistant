package com.anas.aiassistant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Conversation {
    private long id;
    private String title;
    private long createdAt;

    public Conversation(long id, String title, long createdAt) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
    }

    public Conversation(String title) {
        this.title = title;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }
}