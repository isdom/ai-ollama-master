package com.yulore.api;

import java.util.Map;

public interface OllamaService {
    Map<String, String> chat(final String[] roleAndContents);
}
