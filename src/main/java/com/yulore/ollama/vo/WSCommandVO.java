package com.yulore.ollama.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class WSCommandVO<PAYLOAD> {
    Map<String, String> header;
    PAYLOAD payload;
}
