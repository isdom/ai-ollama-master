package com.yulore.ollama;


import com.yulore.api.MasterService;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRemoteService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class MasterMain {
    @PostConstruct
    public void start() {
        log.info("Ollama-Master: Init: redisson: {}", redisson.getConfig().useSingleServer().getDatabase());

        serviceExecutor = Executors.newFixedThreadPool(_service_master_executors, new DefaultThreadFactory("masterExecutor"));

        final RRemoteService rs2 = redisson.getRemoteService(_service_master);
        rs2.register(MasterService.class, masterService, _service_master_executors, serviceExecutor);
    }

    @PreDestroy
    public void stop() {
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
}