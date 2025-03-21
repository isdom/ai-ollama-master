package com.yulore.metric;

import com.yulore.util.NetworkUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
public class AsyncTaskMetrics {
    private static final String HOSTNAME = NetworkUtil.getHostname();
    private static final String LOCAL_IP = NetworkUtil.getLocalIpv4AsString();

    private final Timer asyncTaskTimer;

    public AsyncTaskMetrics(final MeterRegistry registry, final String name, final String desc, final String[] tags) {
        // 定义指标名称、标签、分位数
        asyncTaskTimer = Timer.builder(name)
                .description(desc)
                .tags("hostname", HOSTNAME)
                .tags("ip", LOCAL_IP)
                .tags("ns", System.getenv("NACOS_NAMESPACE"))
                .tags("srv", System.getenv("NACOS_DATAID"))
                .tags(Tags.of(tags))
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofSeconds(1))
                .maximumExpectedValue(Duration.ofSeconds(100))
                .register(registry);
        log.info("Timer: create {} with tags:{}", name, Arrays.toString(tags));
    }

    public Timer getTimer() {
        return asyncTaskTimer;
    }
}