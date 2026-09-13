package com.example.calculator

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import com.example.calculator.api.GeminiManager
import com.example.calculator.tools.FlashlightTool
import kotlinx.coroutines.Job

class AppState {
    val messages: MutableList<ChatMessage> = mutableStateListOf()
    val inputText: MutableState<String> = mutableStateOf("")
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val flashlightOn: MutableState<Boolean> = mutableStateOf(false)
    val geminiManager = GeminiManager()
    val flashlightTool = FlashlightTool()

    fun performSend(text: String) {
        messages.add(ChatMessage("user", text))
        inputText.value = ""
        isLoading.value = true

        CoroutineHelper.runInIO {
            try {
                val tools = listOf<Map<String, Any>>(
                    mapOf(
                        "name" to "FLASHLIGHT_TOGGLE",
                        "description" to "Turns the flashlight on or off. State must be 'on' or 'off'.",
                        "parameters" to mapOf(
                            "type" to "OBJECT",
                            "properties" to mapOf(
                                "state" to mapOf("type" to "STRING", "description" to "Must be 'on' or 'off'")
                            ),
                            "required" to listOf("state")
                        )
                    )
                )

                val response = geminiManager.processMessage(text, tools)

                if (response != null && response.startsWith("FUNCTION_CALL:")) {
                    val parts = response.substring("FUNCTION_CALL:".length).split("\\|".toRegex(), 2)
                    val funcName = parts[0]
                    val argsJson = if (parts.size > 1) parts[1] else "{}"

                    val toolResult = executeTool(funcName, argsJson)

                    val finalResponse = geminiManager.processMessage(
                        "Based on the tool result: $toolResult. Now respond to the user.",
                        tools
                    )
                    finalResponse?.let { fr ->
                        messages.add(ChatMessage("assistant", fr))
                    }
                } else {
                    response?.let { res ->
                        messages.add(ChatMessage("assistant", res))
                    }
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("assistant", "Error: ${e.message}"))
            }
            isLoading.value = false
        }
    }

    private fun executeTool(funcName: String, argsJson: String): String {
        if ("FLASHLIGHT_TOGGLE" == funcName) {
            try {
                val args = com.google.gson.JsonParser.parseString(argsJson).getAsJsonObject()
                val state = args.get("state").asString
                val success = flashlightTool.toggle(state)
                if (success) {
                    flashlightOn.value = "on".equals(state, ignoreCase = true)
                    return "Flashlight turned $state"
                }
                return "Failed to toggle flashlight"
            } catch (e: Exception) {
                return "Error: ${e.message}"
            }
        }
        return "Unknown tool: $funcName"
    }
}

data class ChatMessage(val sender: String, val text: String)
