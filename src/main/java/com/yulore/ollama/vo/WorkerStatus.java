package com.yulore.ollama.vo;

import lombok.Builder;

@Builder
public class WorkerStatus {
    public int total_workers;
    public int free_workers;
}
