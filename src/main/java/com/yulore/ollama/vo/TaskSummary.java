package com.yulore.ollama.vo;

import lombok.Builder;

@Builder
public class TaskSummary {
    public int pending;
    public int done;
}
