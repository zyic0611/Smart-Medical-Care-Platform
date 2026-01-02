package com.yicheng.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Data: 自动生成 Getter, Setter, toString, equals, hashCode
 * @EqualsAndHashCode(callSuper = false): 消除继承类的警告（可加可不加，加了更规范）
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomException extends RuntimeException {

    private String code;
    private String msg;

    /**
     * 构造函数建议【保留手写】
     * 这里的 super(msg) 非常重要！
     * 它能确保异常信息被 Java 原生机制捕获（比如 e.printStackTrace() 时能打印出消息）
     */


    public CustomException(String code, String msg) {
        super(msg); // ➕ 加上这一句，把消息传给父类 RuntimeException
        this.msg = msg;
        this.code = code;
    }


}
