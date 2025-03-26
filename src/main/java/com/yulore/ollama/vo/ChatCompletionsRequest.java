package com.yulore.ollama.vo;

// See: https://www.ctyun.cn/developer/article/589767737143365
//      https://github.com/openai/openai-cookbook
//      https://platform.openai.com/docs/guides/text?api-mode=chat
public class ChatCompletionsRequest {
    public String model;
    public ChatMessage[] messages;
}
