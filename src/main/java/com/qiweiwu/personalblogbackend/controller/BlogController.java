package com.qiweiwu.personalblogbackend.controller;

import com.qiweiwu.personalblogbackend.config.PathConfig;
import com.qiweiwu.personalblogbackend.entity.Blog;
import com.qiweiwu.personalblogbackend.entity.User;
import com.qiweiwu.personalblogbackend.mapper.BlogMapper;
import com.qiweiwu.personalblogbackend.mapper.UserMapper;
import com.qiweiwu.personalblogbackend.service.RedisService;
import com.qiweiwu.personalblogbackend.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class BlogController {

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
    public BlogController(RedisService blogRedisService) {
        this.blogRedisService = blogRedisService;
    }

    @GetMapping("/blog/{id}")
    public Blog getBlogById(@PathVariable String id) {
        return blogMapper.getBlogById(Long.parseLong(id));
    }

    @GetMapping("/blog/author/{id}")
    public List<Blog> getBlogsByAuthorId(@PathVariable String id) {
        return blogMapper.getBlogsByAuthor(Long.parseLong(id));
    }

    @GetMapping("/blog/content/{id}")
    public ResponseEntity<String> getBlogContentById(@PathVariable String id) {
        Blog blog = blogMapper.getBlogById(Long.parseLong(id));

        // if content is stored in redis, just use it.
        String content = (String)blogRedisService.getKeyValue(id);
        if(content == null) {
            String blogName = blog.getBlogName();
            String filePath = pathConfig.getBlogStoreURL() + blog.getAuthorID() + "/" + blog.getBlogID() + "/" + blogName;
            content = fileService.readFileContent(filePath);
            if (Objects.equals(content, "")) {
                return ResponseEntity.badRequest().body("Not such file");
            }
        }

        // preload images to redis
        List<String> images = fileService.getImagesReferenceInMarkdown(content);
        for(String image : images) {
            try {
                URL url = new URL(image);
                String fileName = new File(url.getPath()).getName();
                String imagePath = pathConfig.getBlogStoreURL() + blog.getAuthorID() + "/" + blog.getBlogID() + "/" + fileName;

                byte[] imageBytes = fileService.readFile(Paths.get(imagePath));
                if(imageBytes == null) {
                    throw new Exception("something went wrong when trying to read images in function getBlogContentById");
                }
                blogRedisService.setKeyValue(fileName, imageBytes, 30, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return ResponseEntity.ok(content);
    }

    @GetMapping("/blog/images/{authorId}/{id}/{imageName}")
    public ResponseEntity<byte[]> getBlogImage(@PathVariable String authorId, @PathVariable String id, @PathVariable String imageName) {
        String imageLocation = pathConfig.getBlogStoreURL() + authorId + "/" + id + "/" + imageName;
        Path path = Paths.get(imageLocation);

        // check if the image is store in redis
        byte[] imageContent = (byte[])blogRedisService.getKeyValue(imageName);
        if(imageContent != null) {
            try {
                MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(path));
                return ResponseEntity.ok().contentType(mediaType).body(imageContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            byte[] image = fileService.readFile(path);
            if(image != null) {
                // for temporal locality
                blogRedisService.setKeyValue(imageName, image, 30, TimeUnit.SECONDS);

                MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(path));
                return ResponseEntity.ok().contentType(mediaType).body(image);
            }
            // read image failed
            return ResponseEntity.badRequest().body("image is not exist".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(("Internal Server Error: getUserAvatarById").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * This function is for creating a new blog
     * @param authorId  the author id of the blog
     * @param category  the category of the blog
     * @param content   the content of the blog in string
     * @param files     all the images used in the blog
     * @return          if creation ok or not
     */
    @PostMapping("/blog/string")
    public ResponseEntity<String> createNewBlog(
            @RequestParam("authorId") String authorId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("files") MultipartFile[] files) {
        // 1. generate an uuid to rename the md file
        String uuid = UUID.randomUUID().toString();

        // 2. insert a new data row into database
        String fileName = uuid + ".md";
        Blog blog = new Blog(fileName, Long.parseLong(authorId), category, title);
        if(blogMapper.insertNewBlog(blog) == 0) {
            return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
        }

        String location = pathConfig.getBlogStoreURL() + authorId + "/" + blog.getBlogID() + "/";
        // 3. change the image reference in markdown file
        List<String> imagesReference = fileService.getImagesReferenceInMarkdown(content);
        for(String image : imagesReference) {
            String imagePath = pathConfig.getServerBaseAddr() + "/blog/images/" + authorId + "/" + blog.getBlogID() + "/" + image;
            content = content.replaceFirst(image, imagePath);
        }

        // 4. store the md string into pathConfig.getBlogStoreURL() + authorId + "/" + blogId + "/" + fileName
        if(!fileService.safeStringInfoLocation(content, location + fileName)) {
            // roll back if something went wrong
            blogMapper.deleteBlogById(blog.getBlogID());
            fileService.deleteBlog(location);
            return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
        }

        // 5. store all the images into the same directory
        for(MultipartFile file : files) {
            if(!fileService.safeFileIntoLocation(file, location + file.getOriginalFilename())) {
                // roll back if something went wrong
                blogMapper.deleteBlogById(blog.getBlogID());
                fileService.deleteBlog(location);
                return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
            }
        }

        return ResponseEntity.ok("Create new blog successfully");
    }

    /**
     * This function is for creating a new blog
     * @param authorId  the author id of the blog
     * @param category  the category of the blog
     * @param files     images and the blog
     * @return          if creation ok or not
     */
    @PostMapping("/blog/file")
    public ResponseEntity<String> createNewBlog(String authorId, String category, String title, @RequestParam("files") List<MultipartFile> files) {
        // get the markdown file
        MultipartFile markdown = null;
        for(MultipartFile file : files) {
           if(fileService.getExtension(file).equals(".md")) {
               markdown = file;
               break;
           }
        }
        String content;
        try {
            assert markdown != null;
            content = new String(markdown.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
        }

        if(content.isEmpty()) {
            return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
        }

        // 1. generate an uuid to rename the md file
        String uuid = UUID.randomUUID().toString();

        // 2. insert a new data row into database
        String fileName = uuid + ".md";
        Blog blog = new Blog(fileName, Long.parseLong(authorId), category, title);
        blogMapper.insertNewBlog(blog);

        // 3. store the md file into pathConfig.getBlogStoreURL() + authorId + "/" + blogId + "/" + fileName
        String location = pathConfig.getBlogStoreURL() + authorId + "/" + blog.getBlogID() + "/";
        // change the image reference in markdown file
        List<String> imagesReference = fileService.getImagesReferenceInMarkdown(content);
        for(String image : imagesReference) {
            String imagePath = pathConfig.getServerBaseAddr() + "/blog/images/" + blog.getBlogID() + "/" + image;
            content = content.replaceFirst(image, imagePath);
        }

        if(!fileService.safeStringInfoLocation(content, location + fileName)) {
            // roll back if something went wrong
            blogMapper.deleteBlogById(blog.getBlogID());
            fileService.deleteBlog(location);
            return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
        }

        // 4. store all the images into the same directory
        for(MultipartFile file : files) {
            if(fileService.getExtension(file).equals(".md")) {
                continue;
            }

            if(!fileService.safeFileIntoLocation(file, location + file.getOriginalFilename())) {
                // roll back if something went wrong
                blogMapper.deleteBlogById(blog.getBlogID());
                fileService.deleteBlog(location);
                return ResponseEntity.internalServerError().body("Internal Server Error: createNewBlog");
            }
        }

        return ResponseEntity.ok("Create new blog successfully");
    }

    // TODO: need to test
    @DeleteMapping("/blog/{id}")
    public ResponseEntity<String> deleteBlogById(@PathVariable String id) {
        // delete blog from database
        Long longId = Long.parseLong(id);
        Blog blog = blogMapper.getBlogById(longId);
        int ret = blogMapper.deleteBlogById(longId);
        if(ret == 0) {
            ResponseEntity.internalServerError().body("Internal Server Error: deleteBlogById");
        }

        // delete file
        String blogName = blog.getBlogName();
        String filePath = pathConfig.getBlogStoreURL() + blog.getAuthorID() + "/" + blog.getBlogID() + "/";
        if(fileService.deleteBlog(filePath)) {
            return ResponseEntity.ok("Delete blog successfully");
        }

        return ResponseEntity.internalServerError().body("Internal Server Error: deleteBlogById");
    }
}
