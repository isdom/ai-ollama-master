package com.yulore.ollama.vo;

import lombok.Builder;

@Builder
public class TaskStatus {
    public String task_id;
    public String status;
}
