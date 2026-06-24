package avionica.kafkagateway.dto;

public record TopicMessageDto(
        String timestamp,
        String key,
        int partition,
        long offset,
        String payload
) {}