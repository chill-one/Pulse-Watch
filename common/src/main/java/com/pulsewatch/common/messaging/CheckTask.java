package com.pulsewatch.common.messaging;

import java.time.Instant;
import java.util.UUID;


/**
 * 
 * CheckTask : Message object sent from Scheduler to Rabbit Message queue
 * @param taskId Identifies this queu task
 * @param monitorId tells Worker which Monitor to check
 * @param scheduleCheckAt identifies which scheduled check this task represents
 */
public record CheckTask(
    UUID taskId,
    UUID monitorId,
    Instant scheduleCheckAt
){

}
