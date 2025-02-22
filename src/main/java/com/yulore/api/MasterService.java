package com.yulore.api;

public interface MasterService {
    void updateAgentStatus(final String agentId, final int freeWorks, final long timestamp);
}
