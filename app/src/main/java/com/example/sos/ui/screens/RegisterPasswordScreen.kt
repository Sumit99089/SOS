package com.example.sos.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sos.viewmodel.AuthViewModel

@Composable
fun RegisterPasswordScreen(navController: NavController) {
    val vm: AuthViewModel = viewModel()
    val email = navController.previousBackStackEntry?.savedStateHandle?.get<String>("email") ?: ""
    val name = navController.previousBackStackEntry?.savedStateHandle?.get<String>("name") ?: ""
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            vm.register(email, name, password) { success -> if (success) navController.navigate("login") }
        }) { Text("Register") }

    }
}

