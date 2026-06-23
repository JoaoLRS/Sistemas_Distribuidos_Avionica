package avionica.kafkagateway.dto;

import java.util.Set;

public record TopicInfo(
        String name,
        long messageCount,
        String status,
        int partitions,
        Set<String> consumerGroups
) {}