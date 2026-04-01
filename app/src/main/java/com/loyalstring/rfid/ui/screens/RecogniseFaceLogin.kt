package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.navigation.NavController
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.FaceLoginViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel

@Composable
fun RecogniseFaceLogin(
    navController: NavController,
    viewModel: FaceLoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val permissionViewModel: UserPermissionViewModel = hiltViewModel()

    val matchedFace by viewModel.matchedFace.observeAsState()

    LaunchedEffect(matchedFace) {
        matchedFace?.let { face ->
            userPrefs.saveUserName(face.username ?: "")
            userPrefs.setLoggedIn(true)
            userPrefs.saveBranchId(face.branchId ?: 0)

            face.clientCode?.let { clientCode ->
                face.employeeId?.let { empId ->
                    permissionViewModel.loadPermissions(clientCode, empId)
                }
            }

            navController.navigate(Screens.HomeScreen.route) {
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Scanning face...")
    }
}