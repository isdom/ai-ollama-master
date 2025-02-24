package com.yulore.ollama.controller;

import com.yulore.ollama.vo.*;
import com.yulore.ollama.service.ChatTaskService;
import com.yulore.ollama.vo.ApiResponse;
import com.yulore.ollama.vo.ChatTask;
import com.yulore.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

@Controller
@Slf4j
@RequestMapping("/ollama")
public class ApiController {
    @RequestMapping(value = "/commit_chat_task", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ApiResponse<ChatResponse>> commitChatTask(@RequestBody final ChatTask task) {
        log.info("commit_chat_task: {}", task);

        // 定义异步结果（10 minutes 超时）
        final DeferredResult<ApiResponse<ChatResponse>> deferredResult = new DeferredResult<>(1000L * 60 * 10);
        try {
            taskService.commitChatTask(task, (result)->{
                deferredResult.setResult(ApiResponse.<ChatResponse>builder().code("0000").data(result).build());
                log.info("commit_chat_task: complete with resp: {}", result);
            });
        } catch (final Exception ex) {
            log.warn("commit_chat_task failed: {}", ExceptionUtil.exception2detail(ex));
            deferredResult.setResult(ApiResponse.<ChatResponse>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build());
        } finally {
        }
        return deferredResult;
    }

    @RequestMapping(value = "/worker/status", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<WorkerStatus> queryWorkerStatus() {
        ApiResponse<WorkerStatus> resp = null;
        try {
            final WorkerStatus status = taskService.queryWorkerStatus();
            resp = ApiResponse.<WorkerStatus>builder().code("0000").data(status).build();
        } catch (final Exception ex) {
            log.warn("/worker/status failed: {}", ExceptionUtil.exception2detail(ex));
            resp = ApiResponse.<WorkerStatus>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build();
        } finally {
            log.info("/worker/status: complete with resp: {}", resp);
        }
        return resp;
    }

    @RequestMapping(value = "/agent/all", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<AgentMemo[]> queryAllAgentStatus() {
        ApiResponse<AgentMemo[]> resp = null;
        try {
            final AgentMemo[] memos = taskService.queryAllAgentStatus();
            resp = ApiResponse.<AgentMemo[]>builder().code("0000").data(memos).build();
        } catch (final Exception ex) {
            log.warn("/agent/all failed: {}", ExceptionUtil.exception2detail(ex));
            resp = ApiResponse.<AgentMemo[]>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build();
        } finally {
            log.info("/agent/all: complete with resp: {}", resp);
        }
        return resp;
    }

    @RequestMapping(value = "/task/status", method = RequestMethod.POST)
    @ResponseBody
    public ApiResponse<TaskStatus[]> queryTaskStatus(@RequestBody final QueryTaskStatusRequest request) {
        ApiResponse<TaskStatus[]> resp = null;
        try {
            final TaskStatus[] result = taskService.queryTaskStatus(request.task_ids);
            resp = ApiResponse.<TaskStatus[]>builder().code("0000").data(result).build();
        } catch (final Exception ex) {
            log.warn("/task/status failed: {}", ExceptionUtil.exception2detail(ex));
            resp = ApiResponse.<TaskStatus[]>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build();
        } finally {
            log.info("/task/status: complete with resp: {}", resp);
        }
        return resp;
    }

    @RequestMapping(value = "/task/all", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<TaskStatus[]> queryAllTaskStatus() {
        ApiResponse<TaskStatus[]> resp = null;
        try {
            final TaskStatus[] result = taskService.queryAllTaskStatus();
            resp = ApiResponse.<TaskStatus[]>builder().code("0000").data(result).build();
        } catch (final Exception ex) {
            log.warn("/task/all failed: {}", ExceptionUtil.exception2detail(ex));
            resp = ApiResponse.<TaskStatus[]>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build();
        } finally {
            log.info("/task/all: complete with resp: {}", resp);
        }
        return resp;
    }

    @RequestMapping(value = "/task/summary", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<TaskSummary> queryTaskSummary() {
        ApiResponse<TaskSummary> resp = null;
        try {
            final TaskSummary result = taskService.queryTaskSummary();
            resp = ApiResponse.<TaskSummary>builder().code("0000").data(result).build();
        } catch (final Exception ex) {
            log.warn("/task/summary failed: {}", ExceptionUtil.exception2detail(ex));
            resp = ApiResponse.<TaskSummary>builder().code("2000").message(ExceptionUtil.exception2detail(ex)).build();
        } finally {
            log.info("/task/summary: complete with resp: {}", resp);
        }
        return resp;
    }

    @Autowired
    private ChatTaskService taskService;
}
