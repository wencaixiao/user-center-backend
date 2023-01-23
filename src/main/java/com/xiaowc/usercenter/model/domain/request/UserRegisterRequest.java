package com.xiaowc.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 对象自动生成的序列号
     */
    private static final long serialVersionUID = 7775568102716827770L;

    /**
     * 用户账户
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户校验密码
     */
    private String checkPassword;

    /**
     * 星球编号
     */
    private String planetCode;

}
