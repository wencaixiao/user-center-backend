package com.xiaowc.usercenter.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowc.usercenter.common.ErrorCode;
import com.xiaowc.usercenter.constant.UserConstant;
import com.xiaowc.usercenter.exception.BusinessException;
import com.xiaowc.usercenter.model.domain.User;
import com.xiaowc.usercenter.service.UserService;
import com.xiaowc.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
* @author xiaowc
* @description 用户服务实现类
* @createDate 2022-12-23 20:24:35
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper; // 使用userMapper可以不用写sql自动进行增删改查

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "xiaowc";

    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 返回新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1.校验：
        // (1)账户、密码、校验密码不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 返回参数错误的枚举，表示注册失败，这样比较繁琐
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户、密码或校验密码为空"); // 改进：利用自定义的全局异常类来处理
        }
        // (2)账户不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (3)密码不能小于8位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (4)星球编号的长度不能大于5
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户星球编号过长"); // 改进：利用自定义的全局异常类来处理
        }
        // (5)账户不能包含特殊字符：正则表达式
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~!@#￥%……&*()——+|{}【】';:”“’。,、?]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount); // 看传入的用户名和正则表达式是否匹配
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户包含特殊字符"); // 改进：利用自定义的全局异常类来处理
        }
        // (6)密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户两次输入的密码不相同"); // 改进：利用自定义的全局异常类来处理
        }
        // (7)用户不能重复：传入的用户和数据库中的用户进行比较(如何用户包含了特殊字符，就不用进行这一步了，使内存得到了优化)
        // 查询数据库：
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount); // 指定查询条件
        long count = userMapper.selectCount(queryWrapper); // 查询在数据库中的账户是否和前端传入的注册账户相等，看有多少个
        if (count > 0) { // 用户重复直接返回，表示注册失败
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户重复"); // 改进：利用自定义的全局异常类来处理
        }
        // (8)星球编号不能重复
        // 查询数据库：
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode); // 指定查询条件
        count = userMapper.selectCount(queryWrapper); // 查询在数据库中的账户是否和前端传入的注册账户相等，看有多少个
        if (count > 0) { // 星球编号重复直接返回，表示注册失败
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户星球编号重复"); // 改进：利用自定义的全局异常类来处理
        }
        // 2.对密码进行加密：可以加盐(salt)来进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()); // MD5加密
        // 3.插入数据：将新用户的用户名和密码保存到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) { // 保存数据失败直接返回-1
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新用户保存到数据库失败"); // 改进：利用自定义的全局异常类来处理
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验：
        // (1)账户、密码、校验密码不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户或密码为空"); // 改进：利用自定义的全局异常类来处理
        }
        // (2)账户不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (3)密码不能小于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (4)账户不能包含特殊字符：正则表达式
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~!@#￥%……&*()——+|{}【】';:”“’。,、?]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount); // 看传入的用户名和正则表达式是否匹配
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户包含特殊字符"); // 改进：利用自定义的全局异常类来处理
        }
        // 2.对密码进行加密：可以加盐(salt)来进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()); // MD5加密
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 定义查询条件
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword); // 注意这个密码一定要是传入加密后的密码
        //User user = this.getOne(queryWrapper);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户不存在"); // 改进：利用自定义的全局异常类来处理
        }
        // 3.用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
        // 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
        User safetyUser = getSafetyUser(user);
        // 4.记录用户的登录态
        // (1)连接服务器端后，得到一个session状态(匿名会话)，返回给前端
        // (2)登录成功后，得到了登录成功的session，并且给该session设置一些值(比如用户信息)，
        //    返回给前端一个设置cookie的命令 session->cookie
        // (3)前端接受到后端的命令，设置cookie，保存到浏览器内
        // (4)前端再次请去后端的时候(相同的域名)，在请求头中带上cookie去请求
        // (5)后端拿到前端传来的cookie，找到对应的session
        // (6)后端从session中可以取出基于该session存储的变量(用户的登录信息，登录名)
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
     * 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
     * @param originUser 原来的用户
     * @return 脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setCreateTime(new Date());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 退出登录功能
     * 只需将用户信息从session中移除即可
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除session
        request.removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }
}




