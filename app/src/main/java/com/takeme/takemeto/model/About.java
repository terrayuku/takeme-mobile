package com.takeme.takemeto.model;

public class About {
    private String title;
    private String header;
    private int image;
    private String content;

    public About(String title, String header, int image, String content) {
        this.title = title;
        this.header = header;
        this.image = image;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "About{" +
                "title='" + title + '\'' +
                ", header='" + header + '\'' +
                ", image=" + image +
                ", content='" + content + '\'' +
                '}';
    }
}
