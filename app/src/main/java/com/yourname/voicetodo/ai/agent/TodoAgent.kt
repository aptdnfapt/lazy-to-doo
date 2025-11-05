package com.yourname.voicetodo.ai.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.core.tools.reflect.tools
import com.yourname.voicetodo.ai.tools.TodoTools
import com.yourname.voicetodo.data.preferences.UserPreferences
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLMCapability
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoAgent @Inject constructor(
    private val todoTools: TodoTools,
    private val userPreferences: UserPreferences
) {

    private val systemPrompt = """
        You are a helpful voice-controlled todo assistant. You help users manage their todos through voice commands.
        
        Your capabilities include:
        - Adding new todos with titles and descriptions
        - Editing existing todos
        - Marking todos as complete, in progress, or do later
        - Removing todos
        - Setting reminders for todos
        - Listing todos by section
        - Reading text out loud when requested
        
        When users speak commands, interpret them naturally and take appropriate actions. Always be helpful and confirm actions taken.
        
        Available sections for todos are:
        - TODO (new tasks)
        - IN_PROGRESS (currently working on)
        - DONE (completed tasks)
        - DO_LATER (deferred tasks)
        
        When adding todos, if no section is specified, use TODO by default.
        Always confirm what you've done in a friendly, conversational way.
        
        If you need clarification about a todo request, use the AskUser tool to ask for more information.
    """.trimIndent()

    suspend fun createAgent(): AIAgent<String, String> {
        val llmApiKey = userPreferences.getLlmApiKey().first()
        val llmBaseUrl = userPreferences.getLlmBaseUrl().first()
        val llmModelName = userPreferences.getLlmModelName().first()
        
        if (llmApiKey.isEmpty()) {
            throw IllegalStateException("API key not set. Please configure the API key in settings.")
        }

        // Create OpenAI client with custom base URL if provided
        val client = if (llmBaseUrl.isNotEmpty()) {
            OpenAILLMClient(
                apiKey = llmApiKey,
                settings = OpenAIClientSettings(
                    baseUrl = llmBaseUrl
                )
            )
        } else {
            OpenAILLMClient(apiKey = llmApiKey)
        }

        val executor = SingleLLMPromptExecutor(client)
        
        // Use custom model name if provided, otherwise use default GPT-4o
        val model: LLModel = if (llmModelName.isNotEmpty()) {
            // Create custom LLModel for user-provided model name
            // IMPORTANT: Must include BOTH Completion AND OpenAIEndpoint.Completions capabilities
            LLModel(
                provider = LLMProvider.OpenRouter,
                id = llmModelName,
                capabilities = listOf(
                    LLMCapability.Completion,                  // Required! Base completion capability
                    LLMCapability.OpenAIEndpoint.Completions,  // Required for OpenAI client
                    LLMCapability.Tools,                        // Required for function calling
                    LLMCapability.Temperature,
                    LLMCapability.MultipleChoices
                ),
                contextLength = 200000,  // Large context for modern models
                maxOutputTokens = 32768  // Large output for modern models
            )
        } else {
            OpenAIModels.Chat.GPT4o
        }

        return AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = systemPrompt,
            toolRegistry = ToolRegistry {
                tool(SayToUser)
                tool(AskUser)
                tools(todoTools)
            }
        )
    }

    suspend fun runAgent(userMessage: String): String {
        val agent = createAgent()
        return try {
            agent.run(userMessage)
        } catch (e: Exception) {
            "Sorry, I encountered an error: ${e.message}"
        }
    }
}