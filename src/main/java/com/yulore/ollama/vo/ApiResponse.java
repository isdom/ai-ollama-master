package com.yulore.ollama.vo;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@Data
@ToString
public class ApiResponse<DATA> {
    private String code;
    private String message;
    private DATA data;
}
