package com.yulore.ollama.vo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder
@Data
@ToString
public class ChatResponse {
    public String task_id;    // "<uuid>"
    public String result;
}
