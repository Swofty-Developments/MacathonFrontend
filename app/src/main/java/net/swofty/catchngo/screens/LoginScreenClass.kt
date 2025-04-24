package net.swofty.catchngo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.swofty.catchngo.models.AuthViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset

class LoginScreenClass {
    // Define custom colors
    private val darkBackground = Color(0xFF15202B) // Dark blue background like X
    private val darkSurface = Color(0xFF1E2732) // Slightly lighter for surfaces
    private val accentBlue = Color(0xFF1DA1F2) // Twitter/X blue
    private val textWhite = Color(0xFFE7E9EA) // Off-white text
    private val textSecondary = Color(0xFF8899A6) // Secondary text color

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo or App Title
                Text(
                    text = "Catch N Go",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Username field with animation that actually works
                val usernameInteractionSource = remember { MutableInteractionSource() }
                val usernameIsFocused by usernameInteractionSource.collectIsFocusedAsState()

                // Create animated gradient rotation for username field
                val infiniteTransition = rememberInfiniteTransition(label = "gradientAnimation")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "angleAnimation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (usernameIsFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            accentBlue.copy(alpha = 0.2f),
                                            accentBlue.copy(alpha = 0.5f),
                                            accentBlue.copy(alpha = 0.8f),
                                            accentBlue,
                                            accentBlue.copy(alpha = 0.8f),
                                            accentBlue.copy(alpha = 0.5f),
                                            accentBlue.copy(alpha = 0.2f)
                                        ),
                                        center = Offset.Zero
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Username", color = textSecondary) },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = darkSurface,
                            cursorColor = accentBlue,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Username",
                                tint = if (usernameIsFocused) accentBlue else textSecondary
                            )
                        },
                        interactionSource = usernameInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password field with working animation
                val passwordInteractionSource = remember { MutableInteractionSource() }
                val passwordIsFocused by passwordInteractionSource.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (passwordIsFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            accentBlue.copy(alpha = 0.2f),
                                            accentBlue.copy(alpha = 0.5f),
                                            accentBlue.copy(alpha = 0.8f),
                                            accentBlue,
                                            accentBlue.copy(alpha = 0.8f),
                                            accentBlue.copy(alpha = 0.5f),
                                            accentBlue.copy(alpha = 0.2f)
                                        ),
                                        center = Offset.Zero
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = textSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = darkSurface,
                            cursorColor = accentBlue,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = if (passwordIsFocused) accentBlue else textSecondary
                            )
                        },
                        interactionSource = passwordInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                if (loginState is AuthViewModel.LoginState.Error) {
                    Text(
                        text = (loginState as AuthViewModel.LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Login button with animation
                Button(
                    onClick = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            authViewModel.login(username, password)
                        }
                    },
                    enabled = loginState !is AuthViewModel.LoginState.Loading,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = accentBlue.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                center = Offset.Zero
                            ),
                            shape = RoundedCornerShape(50.dp)
                        )
                ) {
                    if (loginState is AuthViewModel.LoginState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Don't have an account? Sign up",
                        color = accentBlue
                    )
                }
            }
        }
    }
}