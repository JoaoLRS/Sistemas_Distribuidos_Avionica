package avionica.kafkagateway.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import avionica.kafkagateway.dto.TopicInfo;
import avionica.kafkagateway.service.MessageStreamService;
import avionica.kafkagateway.service.TopicService;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final MessageStreamService messageStreamService;

    public TopicController(TopicService topicService, MessageStreamService messageStreamService) {
        this.topicService = topicService;
        this.messageStreamService = messageStreamService;
    }

    /** CDU-01: Lista todos os tópicos com estatísticas. */
    @GetMapping
    public ResponseEntity<List<TopicInfo>> listTopics() {
        return ResponseEntity.ok(topicService.listAllTopics());
    }

    /**
     * CDU-02: Stream SSE de mensagens em tempo real de um tópico específico.
     * O path usa {name:.+} para aceitar nomes com pontos (ex: avionica.telemetry.speed).
     */
    @GetMapping("/{name:.+}/stream")
    public SseEmitter streamMessages(@PathVariable String name) {
        SseEmitter emitter = new SseEmitter(1_800_000L); // 30 minutos
        messageStreamService.subscribe(name, emitter);
        return emitter;
    }
}