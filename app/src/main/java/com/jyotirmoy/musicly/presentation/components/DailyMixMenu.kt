package com.jyotirmoy.musicly.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMixMenu(
    onDismiss: () -> Unit,
    onApplyPrompt: (String) -> Unit,
    isLoading: Boolean
) {
    val sheetState = rememberModalBottomSheetState()
    var prompt by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "How your Daily Mix is created",
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your Daily Mix is created based on your favorite and most listened songs. We also add tracks from artists and genres you like so you can discover new music.",
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Tell the AI what you want to hear today") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("We use a small sample to keep costs low") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onApplyPrompt(prompt)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = prompt.isNotBlank() && !isLoading
            ) {
                Text(if (isLoading) "Updating..." else "Update Daily Mix")
            }
        }
    }
}
