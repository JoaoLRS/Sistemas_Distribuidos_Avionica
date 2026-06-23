package avionica.kafkagateway.dto;

public record TopicMessage(
        String timestamp,
        String key,
        int partition,
        long offset,
        String payload
) {}