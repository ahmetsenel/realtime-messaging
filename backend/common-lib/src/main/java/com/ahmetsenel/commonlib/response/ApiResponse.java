package com.ahmetsenel.commonlib.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success = true;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setMessage(message);
        return response;
    }
}
