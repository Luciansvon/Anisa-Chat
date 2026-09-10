package com.luciansvon.anisachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.ui.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen()
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Anisa",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = state.debugEvent ?: "local core v0.1",
            style = MaterialTheme.typography.labelSmall,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages) { message ->
                MessageRow(message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.input,
                onValueChange = viewModel::onInputChanged,
                label = { Text("Pesan") },
                enabled = !state.isGenerating,
            )
            Button(
                onClick = viewModel::send,
                enabled = state.input.isNotBlank() && !state.isGenerating,
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator()
                } else {
                    Text("Kirim")
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    val who = if (message.role == MessageRole.USER) "Kamu" else "Anisa"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = who,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
