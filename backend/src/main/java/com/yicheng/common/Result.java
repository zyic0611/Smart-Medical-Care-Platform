package com.yicheng.common;


import lombok.Data;

/*
    统一返回的包装类
 */
@Data
public class Result<T>{

    private String code; // 状态码：200成功，其他失败
    private String msg;  // 提示信息：如“登录成功”，“密码错误”
    private T data;      // 这里的数据类型不能写 Object，要写 T

    // 无参构造
    public Result() {

    }

    //有参构造
    public Result(T data) {
        this.data = data;
    }

    // ==================== 成功的方法 ====================

    // 1. 成功，但不带数据（比如删除成功）
    public static <T>Result<T> success() {
        Result<T> result = new Result<T>();
        result.setCode("200");
        result.setMsg("操作成功");
        return result;
    }

    // 2. 成功，且带数据（比如查询文章列表） !! 静态方法必须在返回值前声明 <T>
    public static <T>Result<T> success(T data) {
        Result<T> result = new Result<T>(data); //调用上面的有参构造
        result.setCode("200");
        result.setMsg("操作成功");
        return result;
    }

    // ==================== 失败的方法 ====================

    // 3. 失败
    public static <T>Result<T> error() {
        Result<T> result = new Result<T>();
        result.setCode("500");
        result.setMsg("系统异常");
        return result;
    }

    // 4.失败
    public static <T>Result<T> error(String code, String msg) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode("500"); // 默认设置成 500
        result.setMsg(msg);
        return result;
    }

}