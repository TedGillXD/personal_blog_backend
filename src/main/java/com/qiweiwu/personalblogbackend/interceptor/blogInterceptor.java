package com.qiweiwu.personalblogbackend.interceptor;


import com.qiweiwu.personalblogbackend.entity.User;
import com.qiweiwu.personalblogbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class blogInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // check if the new blog request has valid author ID
        String author = request.getParameter("authorId");
        User user = userMapper.selectUserById(Long.parseLong(author));
        if(user == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentType(String.valueOf(MediaType.TEXT_PLAIN));
            response.getWriter().write("User " + author + " doesn't exist.");
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
