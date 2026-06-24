package avionica.kafkagateway.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import avionica.kafkagateway.dto.TopicMessageDto;
import jakarta.annotation.PreDestroy;

@Service
public class MessageStreamService {

    private static final Logger log = LoggerFactory.getLogger(MessageStreamService.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Inicia um stream SSE que consome mensagens de um tópico Kafka em tempo real.
     * Cada chamada cria um consumer exclusivo com group.id único.
     */
    public void subscribe(String topicName, SseEmitter emitter) {
        var running = new AtomicBoolean(true);

        Runnable cleanup = () -> running.set(false);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        executor.submit(() -> {
            var props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-panel-" + UUID.randomUUID());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            try (var consumer = new KafkaConsumer<String, String>(props)) {
                consumer.subscribe(List.of(topicName));
                long lastHeartbeat = System.currentTimeMillis();

                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                    for (ConsumerRecord<String, String> record : records) {
                        var msg = new TopicMessageDto(
                                Instant.ofEpochMilli(record.timestamp()).toString(),
                                record.key(),
                                record.partition(),
                                record.offset(),
                                record.value()
                        );
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(msg, MediaType.APPLICATION_JSON));
                        } catch (IOException | IllegalStateException e) {
                            running.set(false);
                            break;
                        }
                    }

                    if (System.currentTimeMillis() - lastHeartbeat > 15_000) {
                        try {
                            emitter.send(SseEmitter.event().comment("heartbeat"));
                            lastHeartbeat = System.currentTimeMillis();
                        } catch (IOException | IllegalStateException e) {
                            running.set(false);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Stream encerrado para tópico {}: {}", topicName, e.getMessage());
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}