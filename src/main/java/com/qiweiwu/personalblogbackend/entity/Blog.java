package com.qiweiwu.personalblogbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jdk.jfr.Unsigned;

import java.sql.Timestamp;

public class Blog {

    @TableId(type = IdType.AUTO)
    private Long blogID;

    private String blogName;

    private Long authorID;

    private String category;

    private String title;

    private Long viewCount;

    private Timestamp releaseTime;

    public Blog() {  }

    public Blog(String blogName, Long authorID, String category, String title) {
        this.blogName = blogName;
        this.authorID = authorID;
        this.category = category;
        this.title = title;
    }

    public Long getBlogID() {
        return blogID;
    }

    public void setBlogID(Long blogID) {
        this.blogID = blogID;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public Long getAuthorID() {
        return authorID;
    }

    public void setAuthorID(Long authorID) {
        this.authorID = authorID;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Timestamp getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Timestamp releaseTime) {
        this.releaseTime = releaseTime;
    }

    @Override
    public String toString() {
        return "Blog{" +
                "blogID=" + blogID +
                ", blogName='" + blogName + '\'' +
                ", authorID=" + authorID +
                ", category='" + category + '\'' +
                '}';
    }
}
