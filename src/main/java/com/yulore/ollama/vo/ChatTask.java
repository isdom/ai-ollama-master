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
    @Data
    @ToString
    public static class Message {
        public String role;
        public String content;
    }
    public String task_id;    // "<uuid>"
    public Message[] messages;
}
