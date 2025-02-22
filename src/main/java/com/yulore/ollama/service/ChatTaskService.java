package com.yulore.ollama.service;

import com.yulore.ollama.vo.*;

public interface ChatTaskService {
    void commitChatTask(final ChatTask task);
    WorkerStatus queryWorkerStatus();
    TaskStatus[] queryTaskStatus(final String[] taskId);
    TaskStatus[] queryAllTaskStatus();
    AgentMemo[] queryAllAgentStatus();
    TaskSummary queryTaskSummary();
}
