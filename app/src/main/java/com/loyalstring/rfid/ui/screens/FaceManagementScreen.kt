package com.loyalstring.rfid.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.loyalstring.rfid.navigation.Screens

@Composable
fun FaceManagement(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { navController.navigate(Screens.AddFaceScreen.route) }) {
            Text("Add Face")
        }

        Button(onClick = { navController.navigate(Screens.RecogniseFaceLogin.route) }) {
            Text("Test Face Login")
        }
    }
}