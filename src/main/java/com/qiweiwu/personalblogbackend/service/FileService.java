package com.qiweiwu.personalblogbackend.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileService {
    public String readFileContent(String filePath) {
        Path path = Paths.get(filePath);
        String content = "";
        try {
            content = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return content;
    }

    public boolean safeStringInfoLocation(String content, String location) {
        if(content.isEmpty()) {
            return false;
        }

        try {
            File dir = Paths.get(location).getParent().toFile();
            if(!dir.exists()) {
                boolean b = dir.mkdir();
                if(!b) return false;
            }

            File newFile = new File(location);
            Files.write(newFile.toPath(), content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean safeFileIntoLocation(MultipartFile file, String location) {
        if(file.isEmpty()) {
            return false;
        }

        try {
            File dir = Paths.get(location).getParent().toFile();
            if(!dir.exists()) {
                boolean b = dir.mkdir();
                if(!b) return false;
            }

            File newFile = new File(location);
            file.transferTo(newFile);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        return true;
    }

    public String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        int index = originalFilename.lastIndexOf(".");
        if(originalFilename.lastIndexOf(".") != -1) {
            return originalFilename.substring(index);
        }
        return "";
    }

    public boolean deleteBlog(String location) {
        Path path = Paths.get(location);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public List<String> getImagesReferenceInMarkdown(String markdown) {
        Pattern pattern = Pattern.compile("!\\[.*?\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(markdown);

        List<String> ret = new ArrayList<>();
        while(matcher.find()) {
            ret.add(matcher.group(1));
        }

        return ret;
    }

    public byte[] readFile(Path filePath) {
        byte[] image = null;
        try {
            if(Files.exists(filePath)) {
                image = Files.readAllBytes(filePath);
            } else {
                throw new Exception("cannot find the file.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return image;
    }
}
