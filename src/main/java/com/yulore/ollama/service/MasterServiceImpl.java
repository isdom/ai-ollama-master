package com.yulore.ollama.service;

import com.yulore.api.MasterService;
import com.yulore.ollama.vo.*;
import com.yulore.ollama.vo.AgentMemo;
import com.yulore.util.ExceptionUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.api.RemoteInvocationOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
public class MasterServiceImpl implements MasterService, ChatTaskService {
    private static final long AGENT_UPDATE_TIMEOUT_IN_MS = 1000 * 30; // 30s

    @Autowired
    public MasterServiceImpl(@Value("${service.ollama}") final String serviceName,
                             final RedissonClient redisson) {
        asyncService = redisson.getRemoteService(serviceName)
                .get(OllamaServiceAsync.class, RemoteInvocationOptions.defaults()
                        .noAck()
                        .expectResultWithin(600 * 1000L));

        checkAndExecuteTasks();
    }

    @Override
    public void updateAgentStatus(final String agentId, final int freeWorks, final long timestamp) {
        log.info("updateAgentStatus: agent[{}] - freeWorks: {} - {}", agentId, freeWorks, timestamp);
        agentMemos.put(agentId, new AgentMemo(agentId, freeWorks, System.currentTimeMillis()));
    }

    @Override
    public void commitChatTask(final ChatTask task) {
        if ( null != pendingTasks.putIfAbsent(task.task_id,
                TaskMemo.builder().task(task).status(0).build()) ) {
            log.warn("commitChatTask: task_id:{} has_committed_already, ignore", task.task_id);
        }
    }

    @Override
    public WorkerStatus queryWorkerStatus() {
        return WorkerStatus.builder().total_workers(agentMemos.size()).free_workers(totalFreeWorks()).build();
    }

    @Override
    public TaskStatus[] queryTaskStatus(final String[] ids) {
        final List<TaskStatus> statues = new ArrayList<>();
        for (String taskId : ids) {
            TaskMemo memo = pendingTasks.get(taskId);
            if (null == memo) {
                // not found
                memo = completedTasks.get(taskId);
                if (null != memo) {
                    statues.add(TaskStatus.builder()
                            .task(memo.task)
                            .status("done")
                            .result(memo.result)
                            .build());
                } else {
                    statues.add(TaskStatus.builder()
                            .task(ChatTask.builder().task_id(taskId).build())
                            .status("not_found")
                            .build());
                }
            } else {
                statues.add(TaskStatus.builder()
                        .task(memo.task)
                        .status("pending")
                        .build());
            }
        }
        return statues.toArray(new TaskStatus[0]);
    }

    @Override
    public TaskStatus[] queryAllTaskStatus() {
        final List<TaskStatus> statues = new ArrayList<>();
        for (final TaskMemo memo : pendingTasks.values()) {
            statues.add(TaskStatus.builder()
                    .task(memo.task)
                    .status("pending")
                    .build());
        }
        for (final TaskMemo memo : completedTasks.values()) {
            statues.add(TaskStatus.builder()
                    .task(memo.task)
                    .status("done")
                    .result(memo.result)
                    .build());
        }
        return statues.toArray(new TaskStatus[0]);
    }

    @Override
    public AgentMemo[] queryAllAgentStatus() {
        return agentMemos.values().toArray(new AgentMemo[0]);
    }

    @Override
    public TaskSummary queryTaskSummary() {
        return TaskSummary.builder().pending(pendingTasks.size()).done(completedTasks.size()).build();
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();

        log.info("CVMasterServiceImpl: shutdown");
    }

    private void checkAndExecuteTasks() {
        updateAgents();
        try {
            if (pendingTaskCount() > 0) {
                if (totalFreeWorks() > 0) {
                    for (final TaskMemo memo : this.pendingTasks.values()) {
                        if (0 == memo.status) {
                            memo.status = 1; // executing
                            log.info("start_chat_task: {}", memo.task);
                            final long now = System.currentTimeMillis();
                            final RFuture<Map<String,String>> future = asyncService.chat(msg2list(memo.task.messages));
                            future.whenComplete((resp, ex) -> {
                                if (resp != null) {
                                    memo.result = resp.get("result");
                                    this.pendingTasks.remove(memo.task.task_id);
                                    completedTasks.put(memo.task.task_id, memo);
                                    log.info("task: {} complete_with: {}, cost: {} s",
                                            memo.task.task_id, resp, (System.currentTimeMillis() - now) / 1000.0f);
                                }
                                if (ex != null) {
                                    log.info("task: {} failed_with: {}, schedule_to_retry",
                                            memo.task.task_id, ExceptionUtil.exception2detail(ex));
                                    // set status => 0, to re-try
                                    memo.status = 0;
                                }
                            });
                            log.info("async_start_chat_task: {} ok", memo.task);
                            break;
                        }
                    }
                } else {
                    //log.debug("no more free workers for pending tasks: {}", pendingTasks);
                }
            }
        } finally {
            scheduler.schedule(this::checkAndExecuteTasks, _task_check_interval, TimeUnit.MILLISECONDS);
        }
    }

    private String[] msg2list(final ChatTask.Message[] messages) {
        final List<String> strs = new ArrayList<>(messages.length * 2);
        for (ChatTask.Message msg : messages) {
            strs.add(msg.role);
            strs.add(msg.content);
        }
        return strs.toArray(new String[0]);
    }


    private void updateAgents() {
        final long now = System.currentTimeMillis();
        if (now - last_agent_check_timestamp > _agent_check_interval) {
            last_agent_check_timestamp = now;
            for (AgentMemo memo : agentMemos.values()) {
                if (now - memo.updateTimestamp() >= AGENT_UPDATE_TIMEOUT_IN_MS) {
                    if (agentMemos.remove(memo.id()) != null) {
                        log.warn("updateAgents: remove_update_timeout agent: {}", memo);
                    }
                }
            }
        }
    }

    private int totalFreeWorks() {
        int freeWorks = 0;
        for (AgentMemo memo : agentMemos.values()) {
            freeWorks += memo.freeWorks();
        }
        return freeWorks;
    }

    private int pendingTaskCount() {
        int pendingTasks = 0;
        for (TaskMemo memo : this.pendingTasks.values()) {
            pendingTasks += memo.status == 0 ? 1 : 0;
        }
        return pendingTasks;
    }

    private final OllamaServiceAsync asyncService;

    @Builder
    @Data
    @ToString
    static public class TaskMemo {
        private ChatTask task;
        // 0: todo  1: executing
        private int status;
        private String result;
    }

    @Value("${task.check_interval:100}") // default: 100ms
    private long _task_check_interval;

    @Value("${agent.check_interval:10000}") // default: 1000ms
    private long _agent_check_interval;

    private long last_agent_check_timestamp = 0;

    private final ConcurrentMap<String, AgentMemo> agentMemos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskMemo> pendingTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskMemo> completedTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, new DefaultThreadFactory("chatTaskExecutor"));
}
