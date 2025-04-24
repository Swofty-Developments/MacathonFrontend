package net.swofty.catchngo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.swofty.catchngo.models.AuthViewModel

class LoginScreenClass {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen(
        onLoginSuccess: () -> Unit,
        onNavigateToRegister: () -> Unit,
        authViewModel: AuthViewModel
    ) {
        val loginState by authViewModel.loginState.observeAsState()

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        // Check for successful login
        LaunchedEffect(loginState) {
            if (loginState is AuthViewModel.LoginState.Success) {
                onLoginSuccess()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Catch N Go",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (loginState is AuthViewModel.LoginState.Error) {
                Text(
                    text = (loginState as AuthViewModel.LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        authViewModel.login(username, password)
                    }
                },
                enabled = loginState !is AuthViewModel.LoginState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loginState is AuthViewModel.LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Don't have an account? Register")
            }
        }
    }
}