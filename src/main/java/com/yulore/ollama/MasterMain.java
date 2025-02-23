package com.yulore.ollama;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yulore.api.MasterService;
import com.yulore.ollama.service.ChatTaskService;
import com.yulore.ollama.vo.ChatResponse;
import com.yulore.ollama.vo.ChatTask;
import com.yulore.ollama.vo.WSCommandVO;
import com.yulore.ollama.vo.WSEventVO;
import com.yulore.util.ExceptionUtil;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.redisson.api.RRemoteService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.*;

@Slf4j
@Component
public class MasterMain {

    // 为了正确解析泛型类 WSCommandVO<PAYLOAD> 的 payload 字段类型，需通过 TypeReference 或 JavaType 明确告知 Jackson 泛型参数的具体类型
    // 方法1: 使用 TypeReference
    public static final TypeReference<WSCommandVO<Void>> WSCMD_VOID = new TypeReference<>() {};
    public static final TypeReference<WSCommandVO<ChatTask>> WSCMD_CHAT_TASK = new TypeReference<>() {};

    @PostConstruct
    public void start() {
        log.info("Ollama-Master: Init: redisson: {}", redisson.getConfig().useSingleServer().getDatabase());

        serviceExecutor = Executors.newFixedThreadPool(_service_master_executors, new DefaultThreadFactory("masterExecutor"));

        final RRemoteService rs2 = redisson.getRemoteService(_service_master);
        rs2.register(MasterService.class, masterService, _service_master_executors, serviceExecutor);

        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(()->{
            for (WebSocket conn : webSockets) {
                if (conn.isOpen()) {
                    conn.sendPing();
                    log.info("send ping for {}", conn.getRemoteSocketAddress());
                }
            }
        }, _ws_heartbeat / 2, _ws_heartbeat / 2, TimeUnit.SECONDS);

        _wsServer = new WebSocketServer(new InetSocketAddress(_ws_host, _ws_port), NettyRuntime.availableProcessors() * 2) {
            @Override
            public void onOpen(final WebSocket conn, final ClientHandshake handshake) {
                webSockets.add(conn);
            }

            @Override
            public void onClose(final WebSocket conn, int code, String reason, boolean remote) {
                webSockets.remove(conn);
            }

            @Override
            public void onMessage(final WebSocket webSocket, final String message) {
                log.info("received text message from {}: {}", webSocket.getRemoteSocketAddress(), message);
                try {
                    // _sessionExecutor.submit(()-> handleWSCommand(cmd, webSocket, message));
                    handleWSCommand(new ObjectMapper().readValue(message, WSCMD_VOID), webSocket, message);
                } catch (JsonProcessingException ex) {
                    log.error("handleWSCommand {}: {}, an error occurred when parseAsJson: {}",
                            webSocket.getRemoteSocketAddress(), message, ex.toString());
                }
            }

            @Override
            public void onError(final WebSocket conn, Exception ex) {

            }

            @Override
            public void onStart() {
            }
        };
        // _wsServer.setConnectionLostTimeout(_ws_heartbeat);
        _wsServer.start();
    }

    private void handleWSCommand(final WSCommandVO<Void> cmd, final WebSocket webSocket, final String message) {
        try {
            if ("chat".equals(cmd.header.get("name"))) {
                final var cmdChatTask = new ObjectMapper().readValue(message, WSCMD_CHAT_TASK);
                log.info("cmd: {}", cmdChatTask);
                taskService.commitChatTask(cmdChatTask.payload,
                        (result)-> WSEventVO.sendEvent(webSocket, "chat_response", ChatResponse.builder()
                                .task_id(cmdChatTask.payload.task_id)
                                .result(result).build()));
            } else if ("worker_status".equals(cmd.header.get("name"))) {
                WSEventVO.sendEvent(webSocket, "worker_status", taskService.queryWorkerStatus());
            }
        } catch (final JsonProcessingException ex) {
            log.warn("handleWSCommand failed: {}", ExceptionUtil.exception2detail(ex));
            // throw new RuntimeException(ex);
        }
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        heartbeatExecutor.shutdownNow();
        _wsServer.stop();

        serviceExecutor.shutdownNow();

        log.info("Ollama-Master: shutdown");
    }

    @Value("${service.master.name}")
    private String _service_master;

    @Value("${service.master.executors}")
    private int _service_master_executors;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private MasterService masterService;

    private ExecutorService serviceExecutor;

    @Value("${ws_server.host}")
    private String _ws_host;

    @Value("${ws_server.port}")
    private int _ws_port;

    @Value("${ws_server.heartbeat:30}")
    private int _ws_heartbeat;

    private WebSocketServer _wsServer;

    private ScheduledExecutorService heartbeatExecutor;
    private final Collection<WebSocket> webSockets = new CopyOnWriteArrayList<>();

    @Autowired
    private ChatTaskService taskService;
}