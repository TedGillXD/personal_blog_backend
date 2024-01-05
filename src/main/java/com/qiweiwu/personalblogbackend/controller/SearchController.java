package com.qiweiwu.personalblogbackend.controller;

import com.qiweiwu.personalblogbackend.config.PathConfig;
import com.qiweiwu.personalblogbackend.entity.Blog;
import com.qiweiwu.personalblogbackend.mapper.BlogMapper;
import com.qiweiwu.personalblogbackend.mapper.UserMapper;
import com.qiweiwu.personalblogbackend.service.RedisService;
import com.qiweiwu.personalblogbackend.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class SearchController {

    @Autowired
    private PathConfig pathConfig;

    @Autowired
    private FileService fileService;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private UserMapper userMapper;

    private final RedisService blogRedisService;

    @Autowired
    public SearchController(RedisService blogRedisService) {
        this.blogRedisService = blogRedisService;
    }

    @GetMapping("/search/{keyword}/{offset}")
    public ResponseEntity<List<Object>> getSearchResultByKeyword(@PathVariable String keyword, @PathVariable String offset) {

        // search related users
        List<Object> list = new ArrayList<>(userMapper.selectUsersByKeyword(keyword, Integer.parseInt(offset)));

        // search related blogs in database
        List<Blog> blogList = blogMapper.getBlogsByKeyword(keyword, Integer.parseInt(offset));
        list.addAll(blogList);

        // preload blog content into redis
        for(Blog blog : blogList) {
            String blogLocation = pathConfig.getBlogStoreURL() + blog.getAuthorID() + "/" + blog.getBlogID() + "/" + blog.getBlogName();
            byte[] file = fileService.readFile(Path.of(blogLocation));
            if(file == null) {
                System.out.println("getSearchResultByKeyword: Something went wrong when trying to read blog into redis!");
            }
            blogRedisService.setKeyValue(blog.getBlogID().toString(), file, 30, TimeUnit.SECONDS);
        }

        return ResponseEntity.ok(list);
    }
}
