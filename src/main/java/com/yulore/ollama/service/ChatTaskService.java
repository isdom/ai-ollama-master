package com.yulore.ollama.service;

import com.yulore.ollama.vo.*;

import java.util.function.Consumer;

public interface ChatTaskService {
    void commitChatTask(final ChatTask task, final Consumer<String> onResult);
    WorkerStatus queryWorkerStatus();
    TaskStatus[] queryTaskStatus(final String[] taskId);
    TaskStatus[] queryAllTaskStatus();
    AgentMemo[] queryAllAgentStatus();
    TaskSummary queryTaskSummary();
}
