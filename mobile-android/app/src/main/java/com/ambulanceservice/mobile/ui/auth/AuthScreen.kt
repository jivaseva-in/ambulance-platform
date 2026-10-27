package com.ambulanceservice.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ambulanceservice.mobile.data.auth.model.AuthRole
import com.ambulanceservice.mobile.data.auth.model.AuthResponse
import com.ambulanceservice.mobile.data.auth.model.LoginRequest
import com.ambulanceservice.mobile.data.auth.model.RegisterRequest
import com.ambulanceservice.mobile.data.auth.model.backendValue
import com.ambulanceservice.mobile.data.auth.repository.AuthRepository
import com.ambulanceservice.mobile.data.auth.repository.AuthResult
import com.ambulanceservice.mobile.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: (AuthResponse) -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val authRepository: AuthRepository = remember { AppContainer.authRepository }
    val scope = rememberCoroutineScope()

    var isLogin by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf(AuthRole.USER) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var ambulanceRegistrationNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var loginIdentifier by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }

    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showApprovalDialog = false },
            title = { Text("Driver registration pending") },
            text = { Text("Your driver account has been submitted for admin approval. After it is approved, you can sign in normally.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApprovalDialog = false
                        isLogin = true
                        password = ""
                        confirmPassword = ""
                        ambulanceRegistrationNumber = ""
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isLogin) "Welcome back" else "Create account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isLogin) "This area is for registered drivers only. Please log in or register as a driver to serve ambulance requests." else "This area is for registered drivers only. Please register and log in as a driver to serve ambulance requests.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onCancel !== {}) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to booking")
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isLogin = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Login")
                    }
                    OutlinedButton(
                        onClick = { isLogin = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Register")
                    }
                }

                if (!isLogin) {
                    Text(
                        text = "Account type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == AuthRole.USER,
                                onClick = { selectedRole = AuthRole.USER }
                            )
                            Text("Patient")
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == AuthRole.DRIVER,
                                onClick = { selectedRole = AuthRole.DRIVER }
                            )
                            Text("Driver")
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }

                if (isLogin) {
                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = { loginIdentifier = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email or phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        )
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Password
                    )
                )

                if (!isLogin) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Password
                        )
                    )

                    if (selectedRole == AuthRole.DRIVER) {
                        OutlinedTextField(
                            value = ambulanceRegistrationNumber,
                            onValueChange = { ambulanceRegistrationNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ambulance registration number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text
                            )
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null

                            try {
                                val result = if (isLogin) {
                                    authRepository.login(
                                        LoginRequest(
                                            username = loginIdentifier.trim(),
                                            password = password.trim()
                                        )
                                    )
                                } else {
                                    if (password != confirmPassword) {
                                        errorMessage = "Passwords do not match"
                                        return@launch
                                    }

                                    authRepository.register(
                                        RegisterRequest(
                                            fullName = fullName.trim(),
                                            email = email.trim(),
                                            phone = phone.trim(),
                                            password = password.trim(),
                                            confirmPassword = confirmPassword.trim(),
                                            role = selectedRole.backendValue(),
                                            ambulanceRegistrationNumber = ambulanceRegistrationNumber.trim().ifBlank { null },
                                            address = address.trim().ifBlank { null }
                                        )
                                    )
                                }

                                when (result) {
                                    is AuthResult.Success -> {
                                        val auth = result.auth
                                        if (auth.token.isNullOrBlank()) {
                                            if (!isLogin && selectedRole == AuthRole.DRIVER) {
                                                showApprovalDialog = true
                                                isLogin = true
                                                fullName = ""
                                                email = ""
                                                phone = ""
                                                password = ""
                                                confirmPassword = ""
                                                address = ""
                                                ambulanceRegistrationNumber = ""
                                            } else {
                                                errorMessage = "This account is not active yet. Please contact support."
                                            }
                                        } else {
                                            onAuthenticated(auth)
                                        }
                                    }
                                    is AuthResult.Error -> errorMessage = result.message
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (isLogin) "Login" else "Create account")
                }
            }
        }
    }
}
