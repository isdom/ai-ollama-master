package com.yulore.ollama.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

// 1. 全局配置: 通过 ObjectMapper 的 DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES 参数，设置全局忽略未知属性
// 2. 基于注解的局部配置: 在 Java 类上添加 @JsonIgnoreProperties(ignoreUnknown = true) 注解，仅对特定类生效
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class WSCommandVO<PAYLOAD> {
    public Map<String, String> header;
    public PAYLOAD payload;
}
