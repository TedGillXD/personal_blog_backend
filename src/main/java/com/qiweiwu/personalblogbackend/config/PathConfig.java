package com.qiweiwu.personalblogbackend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:path.properties")
public class PathConfig {

    @Value("${file.store.baseURL}")
    private String baseURL;

    @Value("${file.store.blogURL}")
    private String blogURL;

    @Value("${file.store.user.avatarURL}")
    private String userAvatarURL;

    @Value("${server.address.base}")
    private String serverBaseAddr;

    public String getBaseURL() {
        return baseURL;
    }

    public String getBlogURL() {
        return blogURL;
    }

    public String getUserAvatarURL() {
        return userAvatarURL;
    }

    public String getBlogStoreURL() {
        return baseURL + blogURL;
    }

    public String getUserAvatarStoreURL() {
        return baseURL + userAvatarURL;
    }

    public String getServerBaseAddr() {
        return serverBaseAddr;
    }
}
