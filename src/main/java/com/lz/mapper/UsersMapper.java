package com.lz.mapper;

import com.lz.pojo.entity.Users;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lz.pojo.Page.UsersConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 存储系统用户信息 Mapper 接口
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Mapper
public interface UsersMapper extends BaseMapper<Users> {

    /**
     * 按用户名获取
     *
     * @param username 用户名
     *
     * @return {@code Users}
     */
    Users getByUsername(String username);

    /**
     * 按条件（username/email/isActive/authStatus）JOIN usersinfo 过滤并分页返回 userId
     *
     * @param config 查询条件（含 pageSize/offset）
     *
     * @return {@code List<Long>}
     */
    @Select("<script>"
            + "SELECT u.UserID FROM users u LEFT JOIN usersinfo ui ON ui.UserID = u.UserID "
            + "<where>"
            + "<if test='username != null and username.trim() != \"\"'> AND u.Username LIKE CONCAT('%', #{username}, '%')</if>"
            + "<if test='email != null and email.trim() != \"\"'> AND u.Email LIKE CONCAT('%', #{email}, '%')</if>"
            + "<if test='isActive != null'> AND u.IsActive = #{isActive}</if>"
            + "<if test='authStatusDb != null'> AND ui.AuthStatus = #{authStatusDb}</if>"
            + "<if test='unauthenticatedOnly'> AND ui.UserID IS NULL</if>"
            + "</where>"
            + "ORDER BY u.UserID "
            + "LIMIT #{pageSize} OFFSET #{offset}"
            + "</script>")
    List<Long> selectFilteredUserIds(UsersConfig config);

    /**
     * 同条件 count（供分页 total 使用）
     *
     * @param config 查询条件
     *
     * @return {@code Long}
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM users u LEFT JOIN usersinfo ui ON ui.UserID = u.UserID "
            + "<where>"
            + "<if test='username != null and username.trim() != \"\"'> AND u.Username LIKE CONCAT('%', #{username}, '%')</if>"
            + "<if test='email != null and email.trim() != \"\"'> AND u.Email LIKE CONCAT('%', #{email}, '%')</if>"
            + "<if test='isActive != null'> AND u.IsActive = #{isActive}</if>"
            + "<if test='authStatusDb != null'> AND ui.AuthStatus = #{authStatusDb}</if>"
            + "<if test='unauthenticatedOnly'> AND ui.UserID IS NULL</if>"
            + "</where>"
            + "</script>")
    Long countFilteredUsers(UsersConfig config);
}
