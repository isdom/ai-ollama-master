package com.yulore.ollama.vo;

import lombok.Data;
import lombok.ToString;

@Data
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
