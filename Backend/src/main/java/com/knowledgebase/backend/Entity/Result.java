package com.knowledgebase.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data                   // 自动创建 Builder 类
@NoArgsConstructor      // 无参构造
@AllArgsConstructor     // 全参构造
@Builder                // 自动生成Builder类 new Result(v1,v2,v3) <=> Result.builder().v1().v2().v3().build()
public class Result<T> {
    private Integer code;       // 响应码 200，404，500等
    private String message;     // success， error等
    private T data;             // 响应数据

    public static <T> Result<T> success(T data) {           // 常用 return Result.success(data)
        return Result.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> Result<T> success(T data, String message) {   // 一般也不用
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Result<T> error(Integer code, String message) {   // 自定义报错，不常用
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> Result<T> error(String message) {     // 服务器内部错误
        return error(500, message);
    }

    public static <T> Result<T> unauthorized() {            // 未认证
        return error(401, "Unauthorized");
    }

    public static <T> Result<T> forbidden() {               // 未授权
        return error(403, "Forbidden");
    }

    public static <T> Result<T> notFound() {                // 404
        return error(404, "Not Found");
    }
}