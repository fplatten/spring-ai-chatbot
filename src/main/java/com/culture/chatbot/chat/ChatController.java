package com.culture.chatbot.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
public class ChatController {

    private final ChatClient inMemorychatClient;
    private final InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
    private final int MAX_MESSAGES = 100;
    private final MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(MAX_MESSAGES)
            .build();

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(ChatClient.Builder builder) {

        this.inMemorychatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                                .build()
                )
                .build();

    }

    // Define a simple request DTO for the prompt
    public static class PromptRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    @PostMapping(value = "/stream", produces = "text/event-stream") // Changed to PostMapping
    public Flux<Map<String, String>> stream(@RequestBody PromptRequest request){ // Accepts request body
        String userPrompt = request.getPrompt();
        logger.info("Received POST request for streaming with prompt: {}", userPrompt);
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        logger.info("Session ID: {}", httpRequest.getSession().getId());
        String sessionId = httpRequest.getSession().getId();


        return inMemorychatClient.prompt()
                .advisors(
                        a -> a.param(CONVERSATION_ID, sessionId)
                )
                .user(userPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    logger.info("Backend Raw AI Chunk: [{}] (Length: {})", chunk, chunk.length());
                })
                .map(chunk -> {
                    return Collections.singletonMap("text", chunk);
                })
                .doOnError(e -> logger.error("Error during streaming: {}", e.getMessage(), e))
                .doOnComplete(() -> logger.info("Streaming completed."));
    }
}

