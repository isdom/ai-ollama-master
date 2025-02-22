package com.yulore.ollama.vo;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class TaskStatus {
    public ChatTask task;
    public String status;
    public String response;
}
