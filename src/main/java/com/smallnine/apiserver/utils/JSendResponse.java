package com.smallnine.apiserver.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JSendResponse<T> {
    
    private String status;
    private T data;
    private String message;
    
    public static <T> JSendResponse<T> success(T data) {
        return new JSendResponse<>("success", data, null);
    }
    
    public static <T> JSendResponse<T> fail(String message) {
        return new JSendResponse<>("fail", null, message);
    }
    
    public static <T> JSendResponse<T> error(String message) {
        return new JSendResponse<>("error", null, message);
    }
}