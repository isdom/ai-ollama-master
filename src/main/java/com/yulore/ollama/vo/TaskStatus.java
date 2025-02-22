package com.yulore.ollama.vo;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class TaskStatus {
    public String task_id;
    public String status;
}
