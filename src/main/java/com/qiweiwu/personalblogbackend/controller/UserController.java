package com.qiweiwu.personalblogbackend.controller;

import com.qiweiwu.personalblogbackend.config.PathConfig;
import com.qiweiwu.personalblogbackend.entity.User;
import com.qiweiwu.personalblogbackend.mapper.UserMapper;
import com.qiweiwu.personalblogbackend.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
public class UserController {

    @Autowired
    private PathConfig pathConfig;

    @Autowired
    private UserMapper userMapper;

    private final RedisService redisService;

    @Autowired
    public UserController(RedisService redisService) {
        this.redisService = redisService;
    }

    @GetMapping("/user/{id}")
    public @ResponseBody User getUserInfoById(@PathVariable String id) {
        // check redis first
        User user = (User)redisService.getKeyValue(id);
        if(user == null) {  // if it's not stored in redis, check database
            Long userId = Long.parseLong(id);
            user = userMapper.selectUserById(userId);
        }

        return user;
    }

    @GetMapping("/avatar/{id}")
    public ResponseEntity<byte[]> getUserAvatarById(@PathVariable String id) {
        String avatarPath = pathConfig.getUserAvatarStoreURL() + id + ".jpg";
        Path path = Paths.get(avatarPath);
        try {
            if(Files.exists(path)) {
                byte[] image = Files.readAllBytes(path);
                MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(path));
                return ResponseEntity.ok().contentType(mediaType).body(image);
            } else {
                String defaultAvatar = pathConfig.getUserAvatarStoreURL() + "default.jpg";
                Path defaultPath = Paths.get(defaultAvatar);
                byte[] image = Files.readAllBytes(defaultPath);
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(("Internal Server Error: getUserAvatarById").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * This function is for user register
     * @param userName
     * @param password
     * @param email
     * @param description
     * @return
     */
    @PostMapping("/user")
    public ResponseEntity<User> userRegister(@RequestParam String userName, @RequestParam String password, @RequestParam String email, @RequestParam String description) {
        // create new user
        User user = new User();
        user.setUserName(userName);
        user.setDescription(description);
        user.setEmail(email);
        user.setPassword(password);

        // insert into database
        int ret = userMapper.insertNewUser(user);
        if(ret == 0) {
            return ResponseEntity.internalServerError().body(null);
        }

        // add to redis, because the new created user will soon access to his/her personal page
        user.setPassword("");
        redisService.setKeyValue(user.getUserID().toString(), user);

        return ResponseEntity.ok(user);
    }

    /**
     * This function is for delete user
     * @param id user id
     * @return
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUserById(@PathVariable String id) {
        Long userId = Long.parseLong(id);
        int ret = userMapper.deleteUserById(userId);
        if(ret != 0) {
            return ResponseEntity.ok("delete new user successfully");
        } else {
            return ResponseEntity.internalServerError().body("Internal Server Error: deleteUserById");
        }
    }


    /**
     * This function is for processing user login
     * @param userIdentity the identification of user, like userId, email.
     * @param password  the password of the user
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> userLogin(@RequestParam(name = "userId") String userIdentity, @RequestParam String password) {
        // get user info from database by userIdentity where it matches user id and user email
        User user = null;
        long id = Long.parseLong(userIdentity);
        if(id != 0) {
            user = userMapper.selectUserByIdForInternal(id);
        } else {
            user = userMapper.selectUserByEmailForInternal(userIdentity);
        }

        // compare the password
        if(!user.getPassword().equals(password)) {
            return ResponseEntity.badRequest().body("Wrong Password!");
        }

        return ResponseEntity.ok("user login successfully");
    }
}
