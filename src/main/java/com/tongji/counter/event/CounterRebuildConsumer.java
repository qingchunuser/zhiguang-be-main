package com.tongji.counter.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongji.counter.schema.CounterKeys;
import com.tongji.counter.schema.CounterSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 灾难场景下的计数重建消费者：基于 earliest 回放历史事件，直接折叠到 SDS。
 * 默认关闭，仅当 counter.rebuild.enabled=true 时启用。
 */
@Service
@ConditionalOnProperty(name = "counter.rebuild.enabled", havingValue = "true")
public class CounterRebuildConsumer {

    private final static String INCR_FIELD_LUA = "incr-field-counter.lua";
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> incrScript;

    public CounterRebuildConsumer(ObjectMapper objectMapper, StringRedisTemplate redis) {
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        //this.incrScript.setScriptText(INCR_FIELD_LUA); // 复用与聚合刷写一致的原子折叠脚本
        this.incrScript.setLocation(new ClassPathResource(INCR_FIELD_LUA));
    }

    @KafkaListener(
            topics = CounterTopics.EVENTS,
            groupId = "counter-rebuild",
            properties = {"auto.offset.reset=earliest"}
    )
    public void onMessage(String message, Acknowledgment ack) throws Exception {
        // 灾备场景：从最早位点回放历史事件，直接折叠到 SDS
        CounterEvent evt = objectMapper.readValue(message, CounterEvent.class);
        String cntKey = CounterKeys.sdsKey(evt.getEntityType(), evt.getEntityId());
        try {
            redis.execute(incrScript, List.of(cntKey),
                    String.valueOf(CounterSchema.SCHEMA_LEN),
                    String.valueOf(CounterSchema.FIELD_SIZE),
                    String.valueOf(evt.getIdx()),
                    String.valueOf(evt.getDelta()));
            ack.acknowledge(); // 写入成功后提交位点，避免重复回放
        } catch (Exception ex) {
            // 不提交位点以便重试
        }
    }


}