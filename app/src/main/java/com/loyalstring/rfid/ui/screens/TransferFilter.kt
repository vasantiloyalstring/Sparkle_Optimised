package com.loyalstring.rfid.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.ui.screens.GradientDropdownButton
@Composable
fun TransferFilter(
    selectedTransferType: String,
    selectedFrom: String,
    selectedTo: String,
    onTransferClick: () -> Unit,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GradientDropdownButton(
            modifier = Modifier.weight(1f),
            label = "Transfer Type",
            selectedOption = selectedTransferType,
            onClick = onTransferClick
        )

        GradientDropdownButton(
            modifier = Modifier.weight(1f),
            label = "From",
            selectedOption = selectedFrom,
            onClick = onFromClick
        )

        GradientDropdownButton(
            modifier = Modifier.weight(1f),
            label = "To",
            selectedOption = selectedTo,
            onClick = onToClick
        )
    }
}