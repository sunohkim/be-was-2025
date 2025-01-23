package model;

import java.time.LocalTime;

public class Article {
    private int id;
    private String content;
    private String authorId;
    private String authorName;
    private LocalTime createTime;
    private String imageUrl;

    public Article(int id, String content, String authorId, String authorName, LocalTime createTime) {
        this.id = id;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createTime = createTime;
    }

    public Article(String content, String authorId, String authorName, LocalTime createTime) {
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createTime = createTime;
    }

    public Article(String content, String authorId, String authorName, LocalTime createTime, String imageUrl) {
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createTime = createTime;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public LocalTime getCreateTime() {
        return createTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
