package com.qiweiwu.personalblogbackend.mapper;

import com.qiweiwu.personalblogbackend.entity.Blog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BlogMapper {

    @Select("select * from blogs where blogID = #{id}")
    public Blog getBlogById(Long id);

    @Select("select blogID, title, category, viewCount, releaseTime from blogs where authorID = #{id}")
    public List<Blog> getBlogsByAuthor(Long id);

    @Select("select blogID, title, category, viewCount, releaseTime from blogs where authorID = #{id} order by viewCount limit #{count}, #{offset}")
    public List<Blog> getBlogsByAuthorIdOrderByViewCount(Long id, int count, int offset);

    @Select("SELECT blogID, title, category, viewCount, releaseTime, blogName, authorID " +
            "FROM blogs " +
            "WHERE LOWER(title) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "OR LOWER(category) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "LIMIT 10 OFFSET #{offset}")
    public List<Blog> getBlogsByKeyword(String keyword, int offset);

    @Insert("insert into blogs (blogName, authorID, category, title) values (#{blogName}, #{authorID}, #{category}, #{title})")
    @Options(useGeneratedKeys = true, keyProperty = "blogID", keyColumn = "blogID")
    public int insertNewBlog(Blog blog);

    @Delete("delete from blogs where blogID = #{id}")
    public int deleteBlogById(Long id);
}
