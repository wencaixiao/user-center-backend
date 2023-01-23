package com.xiaowc.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaowc.usercenter.common.BaseResponse;
import com.xiaowc.usercenter.common.ErrorCode;
import com.xiaowc.usercenter.common.ResultUtils;
import com.xiaowc.usercenter.constant.UserConstant;
import com.xiaowc.usercenter.exception.BusinessException;
import com.xiaowc.usercenter.model.domain.User;
import com.xiaowc.usercenter.model.domain.request.UserLoginRequest;
import com.xiaowc.usercenter.model.domain.request.UserRegisterRequest;
import com.xiaowc.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController //适用于编写restful风格的api，返回值默认为json类型
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求体
     * @return 返回注册用户的id
     */
    @PostMapping("/register")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 返回参数错误的枚举，表示注册失败，这样比较繁琐
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        // 这里也要加一层校验比较好，为什么呢?
        // controller层倾向于对请求参数本身的校验，不涉及业务本身(越少越好)
        // service层是对业务逻辑的校验(有可能被controller之外的类调用)
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, userPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        //return new BaseResponse<>(0, result, "ok");
        return ResultUtils.success(result); // 优化后
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求体
     * @param request 用于保存session
     * @return 返回登录后脱敏的用户
     */
    @PostMapping("/login")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 这里也要加一层校验比较好，为什么呢?
        // controller层倾向于对请求参数本身的校验，不涉及业务本身(越少越好)
        // service层是对业务逻辑的校验(有可能被controller之外的类调用)
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        //return new BaseResponse<>(0, user, "ok");
        return ResultUtils.success(user); // 优化后
    }

    /**
     * 用户退出登录
     * @param request 用于获取用户session，便于移除
     * @return
     */
    @PostMapping("/logout")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        //return new BaseResponse<>(0, result, "ok");
        return ResultUtils.success(result); // 优化后
    }

    /**
     * 获取用户的登录态，获取当前登录用户信息接口
     * @param request
     * @return 返回用户脱敏后的用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // session中存储的是用户登录的凭据
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE); // session中的值相当于是一个缓存
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 不应该直接返回之前用户的登录态currentUser
        // 比如上次登录查看用户的积分是0分，下次登录时用户是100分，那下次登录就要更新用户的积分，因此需要从数据库中去查询更好
        // 信息频繁变化的系统的可以通过查询一次数据库来变化信息
        Long userId = currentUser.getId();
        // TODO: 校验用户是否合法
        User user = userService.getById(userId); // 可以直接从session中取再脱敏，但是去数据库中查更好
        User safetyUser = userService.getSafetyUser(user);// 返回脱敏后的信息(更安全)
        //return new BaseResponse<>(0, safetyUser, "ok");
        return ResultUtils.success(safetyUser); // 优化后
    }

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        // 判断是否为管理员，仅管理员可查询
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        // 用户已登录，并且是管理员权限
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username); // 模糊查询
        }
        // 应该返回脱敏后的信息
        List<User> userList = userService.list(queryWrapper); // 查询用户列表
        // 遍历userList将里面的userPassword置为空脱敏，再转换成列表进行返回
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        //return new BaseResponse<>(0, list, "ok");
        return ResultUtils.success(list); // 优化后
    }

    /**
     * 根据用户id删除用户(逻辑删除)
     * @param id 用户id
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        // 判断是否为管理员，仅管理员可查询
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH); // 改进：利用自定义的全局异常类来处理
        }
        // 用户已登录，并且是管理员权限
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        boolean b = userService.removeById(id);// 逻辑删除
        //return new BaseResponse<>(0, b, "ok");
        return ResultUtils.success(b); // 优化后
    }

    /**
     * 是否为管理员，提取作为一个方法
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        // 用户未登录，或者不是管理员权限，就不能进行下一步的查询用户操作
        if (user == null || user.getUserRole() != UserConstant.ADMIN_ROLE) {
            return false;
        }
        return true;
    }
}
