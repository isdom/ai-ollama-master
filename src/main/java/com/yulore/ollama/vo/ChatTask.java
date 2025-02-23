package com.yulore.ollama.vo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder
@Data
@Jacksonized
@ToString
public class ChatTask {
    public String task_id;    // "<uuid>"
    public ChatMessage[] messages;
}
