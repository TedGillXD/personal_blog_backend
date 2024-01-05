package com.qiweiwu.personalblogbackend.mapper;

import com.qiweiwu.personalblogbackend.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select userID, userName, description, email from users where userID = #{id}")
    public User selectUserById(Long id);

    @Select("select * from users where userID = #{id}")
    public User selectUserByIdForInternal(Long id);

    @Select("select * from users where email = #{email}")
    public User selectUserByEmailForInternal(String emailAddr);

    @Insert("insert into users (userName, password, email) values (#{userName}, #{password}, #{email})")
    public int insertNewUser(User user);

    @Select("SELECT userID, userName, description, email " +
            "FROM users " +
            "WHERE LOWER(userID) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "OR LOWER(userName) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "OR LOWER(email) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "LIMIT 10 OFFSET #{offset}")
    public List<User> selectUsersByKeyword(String keyword, int offset);

    @Delete("delete from users where userID = #{id}")
    public int deleteUserById(Long id);
}
