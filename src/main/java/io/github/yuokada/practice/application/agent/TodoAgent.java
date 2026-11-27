package io.github.yuokada.practice.application.agent;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = TodoAgentTools.class, maxSequentialToolInvocations = 5)
@ApplicationScoped
@SystemMessage("""
        You are a Todo management assistant. Always answer in Japanese.
        Use the available tools when the user asks you to inspect or change Todo tasks.
        Never claim that a change succeeded unless the corresponding tool succeeded.
        Before deleting a task, show its ID and title and ask for explicit confirmation.
        Only call the delete tool after the user confirms the deletion in a later message.
        Keep answers concise and summarize any changes you made.
        """)
public interface TodoAgent {

    String chat(@MemoryId String sessionId, @UserMessage String message);
}
