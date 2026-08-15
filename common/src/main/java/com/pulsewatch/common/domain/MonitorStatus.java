package com.pulsewatch.common.domain;

public enum MonitorStatus {
    //Still waiting on the Monitor data
    PENDING,
    //The server is running fine
    UP,
    //Having some issues connecting to the server
    DEGRADED,
    //The server is down
    DOWN
}
