package com.github.saintedlittle.bunnyviewer.ui.auth


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.saintedlittle.bunnyviewer.data.Account


@Composable
fun RegisterScreen(onDone: (Account) -> Unit, onAlreadyHave: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    AuthScaffold(title = "Создать аккаунт") {
        OutlinedTextField(login, { login = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { if (login.isNotBlank() && password.isNotBlank()) onDone(Account(login, password)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Зарегистрироваться")
        }
        TextButton(onClick = onAlreadyHave, modifier = Modifier.align(Alignment.End)) { Text("У меня уже есть аккаунт") }
    }
}


@Composable
fun LoginScreen(onLogin: (Account) -> Unit, onNoAccount: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    AuthScaffold(title = "Войти") {
        OutlinedTextField(login, { login = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { if (login.isNotBlank() && password.isNotBlank()) onLogin(Account(login, password)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Войти")
        }
        TextButton(onClick = onNoAccount, modifier = Modifier.align(Alignment.End)) { Text("Создать аккаунт") }
    }
}


@Composable
private fun AuthScaffold(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().widthIn(max = 520.dp)) { content() }
    }
}