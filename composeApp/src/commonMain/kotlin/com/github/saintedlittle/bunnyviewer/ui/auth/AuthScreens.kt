package com.github.saintedlittle.bunnyviewer.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.github.saintedlittle.bunnyviewer.data.Account

@Composable
fun RegisterScreen(
    onDone: (Account) -> Unit,
    onAlreadyHave: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwdVisible by rememberSaveable { mutableStateOf(false) }

    val canSubmit = login.isNotBlank() && password.isNotBlank()

    AuthScaffold(title = "Создать аккаунт") {
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { pwdVisible = !pwdVisible }) {
                    Icon(
                        imageVector = if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (pwdVisible) "Скрыть" else "Показать"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (canSubmit) onDone(Account(login, password))
                }
            )
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDone(Account(login, password)) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Зарегистрироваться") }

        TextButton(
            onClick = onAlreadyHave,
            modifier = Modifier.align(Alignment.End)
        ) { Text("У меня уже есть аккаунт") }
    }
}

@Composable
fun LoginScreen(
    onLogin: (Account) -> Unit,
    onNoAccount: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwdVisible by rememberSaveable { mutableStateOf(false) }

    val canSubmit = login.isNotBlank() && password.isNotBlank()

    AuthScaffold(title = "Войти") {
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { pwdVisible = !pwdVisible }) {
                    Icon(
                        imageVector = if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (pwdVisible) "Скрыть" else "Показать"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (canSubmit) onLogin(Account(login, password))
                }
            )
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(Account(login, password)) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Войти") }

        TextButton(
            onClick = onNoAccount,
            modifier = Modifier.align(Alignment.End)
        ) { Text("Создать аккаунт") }
    }
}

@Composable
private fun AuthScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) { content() }
    }
}
