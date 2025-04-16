package com.example.sos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun RegisterEmailScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(email, { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.currentBackStackEntry?.savedStateHandle?.set("email", email); navController.navigate("register_name") }) {
            Text("Next")
        }
    }
}