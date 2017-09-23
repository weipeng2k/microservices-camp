package org.hola.wildflyswarm.rest;

import java.util.Date;

/**
 * @author weipeng2k 2017年09月20日 下午18:21:18
 */
public class Book {
    private Long id;
    private String name;
    private int version;
    private String authorName;
    private Date publishDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", authorName='" + authorName + '\'' +
                ", publishDate=" + publishDate +
                '}';
    }
}
