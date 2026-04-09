package com.tongji.counter.event;

import com.alibaba.google.common.util.concurrent.ListenableFuture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 计数事件生产者。
 *
 * <p>职责：将业务产生的计数增量事件异步发送到 Kafka 主题，供聚合消费者处理。</p>
 */
@Service
@Slf4j
public class CounterEventProducer {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public CounterEventProducer(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布计数事件到 Kafka。
     * @param event 计数事件（实体类型、ID、指标、delta 等）
     */
    public void publish(CounterEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);//将CounterEvent对象转换为JSON字符串
            //发送kafka：counter-events
            CompletableFuture<SendResult<String, String>> result = kafka.send(CounterTopics.EVENTS, payload);//将CounterEvent对象转换为JSON字符串并
            // 监听发送结果
            result.whenComplete((sendResult, throwable) -> {
                if (throwable != null) {
                    // 发送失败 - 记录日志并触发告警
                    log.error("Kafka消息发送失败 - 事件: {}, 错误: {}",
                            payload, throwable.getMessage(), throwable);
                } else {
                    // 发送成功 - 记录详细信息
                    RecordMetadata metadata = sendResult.getRecordMetadata();
                    log.info("Kafka消息发送成功 - Topic: {}, Partition: {}, Offset: {}, Event: {}",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            payload);
                }
            });
        } catch (JsonProcessingException e) {
            // 生产异常不抛出影响主流程；可接入告警
        }
    }
}