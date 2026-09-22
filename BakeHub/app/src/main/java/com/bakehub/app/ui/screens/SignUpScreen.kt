package com.bakehub.app.ui.screens

import android.R
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
 * Sign Up / Register screen.
 * Creates a Firebase Authentication account plus a matching Firestore
 * "users/{uid}" profile document (see AuthRepository.signUp).
 * Navigates to: Home on successful registration (Firebase signs the user
 * in immediately after account creation).
 */
@Composable
fun SignUpScreen(
    onSignup: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
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
            "Sign up to BakeHub",
            color = DarkBrown,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )

        Text(
            "Create your account",
            color = GreyText,
            fontSize = 14.sp,
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 28.dp
            ),
        )

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = {
                Text("Enter your name")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )

        // EMAIL
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {
                Text("Email address")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )


        // PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },

            label = {
                Text("Password")
            },

            singleLine = true,

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

        // SIGN UP BUTTON
        Button(
            onClick = {
                if (name.isBlank()) {
                    errorMessage = "Please enter your name."
                }
                else if (email.isBlank()) {
                    errorMessage = "Please enter your email."
                }
                else if (password.isBlank()) {
                    errorMessage = "Please enter your password."
                }
                else if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters."
                }
                else {
                    errorMessage = ""
                    isLoading = true
                    scope.launch {
                        try {
                            AuthRepository.signUp(name, email, password)
                            isLoading = false
                            onSignup()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.message ?: "Couldn't create your account."
                        }
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
                    "Sign Up",
                    fontWeight = FontWeight.Bold
                )
            }
        }



        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // BACK TO LOGIN BUTTON
        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                contentColor = White
            ),
        ) {
            Text("Already have an account? Log In")
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
