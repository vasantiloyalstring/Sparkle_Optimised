package com.loyalstring.rfid.ui.screens


import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import com.loyalstring.rfid.worker.LocaleHelper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse

@Composable
fun SampleOutInputRowData(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onScanClicked: () -> Unit,
    onClearClicked: () -> Unit,
    filteredList: List<SampleInResponse>,
    isLoading: Boolean,
    onItemSelected: (ItemCodeResponse) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val query = itemCode.text.trim()


}
