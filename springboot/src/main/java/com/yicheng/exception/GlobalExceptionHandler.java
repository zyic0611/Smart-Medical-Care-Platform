package com.yicheng.exception;


import com.yicheng.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 1. 这个注解表示：我是个“大喇叭”，专门监听所有 Controller 的动静
@RestControllerAdvice("com.yicheng.controller")
public class GlobalExceptionHandler {

    // 2. 这个注解表示：我要捕获 Exception 类型的异常（也就是所有异常）
    @ExceptionHandler(Exception.class)
    public Result exception(Exception e) {


        // 打印错误日志到控制台，方便后端开发排查
        e.printStackTrace();

        // 3. 统一返回给前端的格式
        // 这里的 "系统错误" 可以根据异常的具体类型写的更细，比如 "密码错误"、"账号不存在"
        //return Result.error("500", "系统出了点小差错，请联系管理员");
        return Result.error();
    }


    //在全局异常中注册 用户异常
    @ExceptionHandler(CustomException.class)
    public Result customException(CustomException e) {
        return  Result.error(e.getCode(), e.getMsg());
    }


}
