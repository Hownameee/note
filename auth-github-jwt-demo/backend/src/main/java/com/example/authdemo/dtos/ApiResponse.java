package com.example.authdemo.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
}
