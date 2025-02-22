package com.yulore.ollama.service;

import com.yulore.api.OllamaService;
import org.redisson.api.RFuture;
import org.redisson.api.annotation.RRemoteAsync;

import java.util.Map;

@RRemoteAsync(OllamaService.class)
public interface OllamaServiceAsync {
    RFuture<Map<String,String>> chat(final String[] roleAndContents);
}
