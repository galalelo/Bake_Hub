package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.AuthRepository
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White
import kotlinx.coroutines.launch

/**
 * 3.2 Login / Register — authenticate an existing user or begin registration.
 * Navigates to: Home/Discover on success.
 *
 * Backed by Firebase Authentication (email/password) via AuthRepository.
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignUp: () -> Unit
)
{
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
    ) {

        Text(
            "Welcome back",
            color = DarkBrown,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )

        Text(
            "Log in to continue baking",
            color = GreyText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            //text animation
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = fieldColors(),
        )

        // ERROR MESSAGE
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }


        // LOGIN BUTTON
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter your email and password."
                    return@Button
                }
                errorMessage = ""
                isLoading = true
                scope.launch {
                    try {
                        AuthRepository.login(email, password)
                        isLoading = false
                        onLoggedIn()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.message ?: "Incorrect email or password."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                contentColor = White
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = White)
            } else {
                Text(
                    "Log In",
                    fontWeight = FontWeight.Bold
                )
            }
        }




        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            "New here? Create an account",
            color = Terracotta,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp),
        )

        // SIGN-UP BUTTON
        Button(
            onClick = onSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                contentColor = White
            ),
        ) {
            Text("Sign up")
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Terracotta,
    unfocusedBorderColor = GreyText,
    focusedContainerColor = White,
    unfocusedContainerColor = White,
)