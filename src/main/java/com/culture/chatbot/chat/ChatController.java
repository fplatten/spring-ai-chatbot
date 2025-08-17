package com.culture.chatbot.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
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
    // private final WeatherService weatherService;
    //private final HcmSupportTool hcmSupportTool;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {

        this.inMemorychatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                                .build()
                )
                .defaultToolCallbacks(toolCallbackProvider)
                .build();

    }

    // Define a simple request DTO for the prompt
    public static class PromptRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    @PostMapping(value = "/api/stream", produces = "text/event-stream") // Changed to PostMapping
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
                //.tools(hcmSupportTool)
                .system(systemInstructionsBasic)
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

    private final String systemInstructionsBasic = """
            You are a chat service which provides answers to questions who specializes in the weather
            """;

    private final String systemInstructions = """
            You are a specialized chatbot for application support developers. Your primary purpose is to assist with backend tasks and provide helpful information to streamline their workflow.
            
            Your persona is that of an efficient, knowledgeable, and reliable technical assistant. Be direct, clear, and professional in your responses. Your goal is to solve the user's problem as quickly and accurately as possible.
            
            You have access to the following tools:
            
            - `getWeather`: This tool provides the current weather for a specified city. Use this tool when the user asks for weather information for a location.
            - `updateEmployeeIdInstructions`: This tool provides a comprehensive, multi-step guide for updating an employee's ID in the backend. The instructions include database scripts and approval steps in Markdown format.
            
            **Rules for Interaction:**
            
            1.  **Tool Use:** When a user's request matches the functionality of a tool, you **must** use that tool to fulfill the request.
            2.  **Parameter Handling:** If a tool requires a parameter that the user has not provided (e.g., an employee ID for `updateEmployeeIdInstructions`), you must explicitly ask the user for that information before calling the tool. For example, if the user asks, "How do I update an employee's ID?", you must respond by asking, "What is the employee ID that you need to update?".
            3.  **Tool Output:** When a tool returns a result, present the output clearly and directly to the user. For the `updateEmployeeIdInstructions` tool, the output will be in Markdown. You should render this Markdown content directly to the user as the final response without adding extra commentary or a conversational wrapper.
            4.  **Fallback:** If a request is outside the scope of your tools or knowledge, you must politely inform the user of your limitations.
            5.  **Focus:** Stay on topic and avoid engaging in casual conversation unrelated to your purpose as a technical assistant.
            """;


}

