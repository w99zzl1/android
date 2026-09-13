package com.example.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.AppState
import com.example.calculator.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF16213E),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "AI Assistant",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            state.messages.forEachIndexed { index, msg ->
                item {
                    MessageBubble(message = msg)
                }
            }
            if (state.isLoading.value) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFF2A2A4A)
                        ) {
                            Text(
                                text = "Thinking...",
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFF8888AA),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = if (state.flashlightOn.value) "🔦 Flashlight: ON" else "🔦 Flashlight: OFF",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (state.flashlightOn.value) Color(0xFFFFEB3B) else Color.Gray,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputText.value,
                onValueChange = { state.inputText.value = it },
                placeholder = { Text("Ask me anything...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF6200EE),
                    unfocusedBorderColor = Color(0xFF444466),
                    cursorColor = Color(0xFF6200EE)
                ),
                textStyle = TextStyle(color = Color.White),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = state.inputText.value.trim()
                    if (text.isNotEmpty()) {
                        state.performSend(text)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                modifier = Modifier.size(48.dp)
            ) {
                Text("→", fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val backgroundColor = if (message.sender == "user") Color(0xFF6200EE) else Color(0xFF2A2A4A)
    val alignment = if (message.sender == "user") Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = backgroundColor
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}
