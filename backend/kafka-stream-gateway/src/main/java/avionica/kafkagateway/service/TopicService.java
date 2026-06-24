package avionica.kafkagateway.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import avionica.kafkagateway.dto.TopicInfoDto;

@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private final AdminClient adminClient;

    public TopicService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public List<TopicInfoDto> listAllTopics() {
        try {

            Set<String> topicNames = adminClient.listTopics()
                    .names().get(10, TimeUnit.SECONDS)
                    .stream()
                    .filter(t -> !t.startsWith("__"))
                    .collect(Collectors.toSet());

            if (topicNames.isEmpty()) {
                return List.of();
            }


            Map<String, TopicDescription> descriptions = adminClient
                    .describeTopics(topicNames)
                    .allTopicNames().get(10, TimeUnit.SECONDS);


            Map<TopicPartition, OffsetSpec> latestReq = new HashMap<>();
            Map<TopicPartition, OffsetSpec> earliestReq = new HashMap<>();

            for (TopicDescription desc : descriptions.values()) {
                for (TopicPartitionInfo pInfo : desc.partitions()) {
                    TopicPartition tp = new TopicPartition(desc.name(), pInfo.partition());
                    latestReq.put(tp, OffsetSpec.latest());
                    earliestReq.put(tp, OffsetSpec.earliest());
                }
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets =
                    adminClient.listOffsets(latestReq).all().get(10, TimeUnit.SECONDS);
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets =
                    adminClient.listOffsets(earliestReq).all().get(10, TimeUnit.SECONDS);


            Map<String, Set<String>> groupsByTopic = getConsumerGroupsByTopic();


            List<TopicInfoDto> result = new ArrayList<>();
            for (String name : topicNames) {
                TopicDescription desc = descriptions.get(name);
                long totalMessages = 0;

                for (TopicPartitionInfo pInfo : desc.partitions()) {
                    TopicPartition tp = new TopicPartition(name, pInfo.partition());
                    long latest = latestOffsets.containsKey(tp) ? latestOffsets.get(tp).offset() : 0;
                    long earliest = earliestOffsets.containsKey(tp) ? earliestOffsets.get(tp).offset() : 0;
                    totalMessages += (latest - earliest);
                }

                String status = totalMessages > 0 ? "Ativo" : "Sem Atividade";
                Set<String> groups = groupsByTopic.getOrDefault(name, Set.of());

                result.add(new TopicInfoDto(name, totalMessages, status, desc.partitions().size(), groups));
            }

            result.sort((a, b) -> a.name().compareTo(b.name()));
            return result;

        } catch (Exception e) {
            log.warn("Não foi possível listar tópicos do Kafka: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Set<String>> getConsumerGroupsByTopic() {
        Map<String, Set<String>> result = new HashMap<>();
        try {
            Collection<ConsumerGroupListing> groups = adminClient
                    .listConsumerGroups().all().get(5, TimeUnit.SECONDS);

            for (ConsumerGroupListing group : groups) {
                try {
                    Map<TopicPartition, OffsetAndMetadata> offsets = adminClient
                            .listConsumerGroupOffsets(group.groupId())
                            .partitionsToOffsetAndMetadata()
                            .get(5, TimeUnit.SECONDS);

                    for (TopicPartition tp : offsets.keySet()) {
                        result.computeIfAbsent(tp.topic(), k -> new LinkedHashSet<>())
                                .add(group.groupId());
                    }
                } catch (Exception ignored) {

                }
            }
        } catch (Exception e) {
            log.warn("Erro ao listar consumer groups: {}", e.getMessage());
        }
        return result;
    }
}