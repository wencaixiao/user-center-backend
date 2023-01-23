package com.xiaowc.usercenter.service;

import com.xiaowc.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xiaowc
 * @description 用户服务
 * @createDate 2022-12-23 20:24:35
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * 只需将用户信息存入session中即可
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
     * 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
     * @param originUser 原来的用户
     * @return 脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 退出登录功能
     * 只需将用户信息从session中移除即可
     * @return
     */
    int userLogout(HttpServletRequest request);
}
