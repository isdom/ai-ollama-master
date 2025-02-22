package com.yulore.ollama.service;

import com.yulore.api.OllamaService;
import org.redisson.api.RFuture;
import org.redisson.api.annotation.RRemoteAsync;

@RRemoteAsync(OllamaService.class)
public interface OllamaServiceAsync {
    RFuture<String> chat(final String[] roleAndContents);
}
