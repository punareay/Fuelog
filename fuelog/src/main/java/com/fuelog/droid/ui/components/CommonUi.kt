package com.fuelog.droid.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AutoSelectTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Sync external value to internal state
    LaunchedEffect(value) {
        if (textFieldValueState.text != value) {
            textFieldValueState = textFieldValueState.copy(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValueState,
        onValueChange = {
            textFieldValueState = it
            if (value != it.text) {
                onValueChange(it.text)
            }
        },
        label = label,
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                textFieldValueState = textFieldValueState.copy(
                    selection = TextRange(0, textFieldValueState.text.length)
                )
            }
        },
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = true
    )
}

fun formatRiel(amount: Double): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return "${formatter.format(amount)}៛"
}
