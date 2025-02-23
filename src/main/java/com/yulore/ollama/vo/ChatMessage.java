package com.yulore.ollama.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChatMessage {
    public String role;
    public String content;
}
