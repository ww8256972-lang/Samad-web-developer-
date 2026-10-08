package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.ActivityLog
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.PaymentRecord
import com.example.models.PaymentStatus
import com.example.network.WhatsAppService
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerDetailScreen(
    customerId: String,
    viewModel: MainViewModel
) {
    BackHandler { viewModel.navigateBack() }

    val customer by viewModel.getCustomerStream(customerId).collectAsState(initial = null)
    val logs by viewModel.getLogsStream(customerId).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsStream(customerId).collectAsState(initial = emptyList())
    val appSettings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Dialog States
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentMethodInput by remember { mutableStateOf("Online Bank Transfer") }
    var paymentNoteInput by remember { mutableStateOf("") }

    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var whatsappMessageInput by remember { mutableStateOf("") }

    var showArchiveDialog by remember { mutableStateOf(false) }
    var showMarkSuccessDialog by remember { mutableStateOf(false) }

    val c = customer

    if (c == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Customer record not found or loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val now = System.currentTimeMillis()
    val isOverdue = c.followUpDate in 1 until now &&
        c.currentStatus != CustomerStatus.SUCCESS &&
        c.currentStatus != CustomerStatus.CANCELLED &&
        c.currentStatus != CustomerStatus.WEBSITE_COMPLETED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("customer_detail_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar & Back Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.CustomerEdit(c.id)) },
                        modifier = Modifier.testTag("detail_edit_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = ElectricBlue)
                    }

                    IconButton(
                        onClick = { showArchiveDialog = true },
                        modifier = Modifier.testTag("detail_archive_btn")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive Customer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Hero Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("detail_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                border = if (isOverdue) {
                    BorderStroke(1.dp, StatusError.copy(alpha = 0.6f))
                } else {
                    BorderStroke(1.dp, Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.5f), NeonPink.copy(alpha = 0.5f))))
                }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = c.customerId,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }

                        if (isOverdue) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusError.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OVERDUE", color = StatusError, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = c.currentStatus.displayName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = c.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "${c.businessName} • ${c.businessType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "📍 ${c.city}, ${c.state} ${if (c.address.isNotBlank()) "• " + c.address else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Communication Buttons: Call & WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.logCustomerCall(c)
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${c.mobileNumber}")
                                }
                                context.startActivity(dialIntent)
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_call_customer_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSuccess,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Customer", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val template = appSettings?.whatsappTemplate ?: "Hello {customerName},\nThis is a reminder regarding your website inquiry ({projectName}).\nThank you,\nWeb Record by Waqar"
                                whatsappMessageInput = WhatsAppService.formatTemplate(template, c)
                                showWhatsAppDialog = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_send_whatsapp_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Financial & Project Settlements Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("detail_financial_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment & Financial Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (c.paymentStatus) {
                                PaymentStatus.RECEIVED -> StatusSuccess.copy(alpha = 0.15f)
                                PaymentStatus.PARTIAL -> StatusWarning.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = c.paymentStatus.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (c.paymentStatus) {
                                    PaymentStatus.RECEIVED -> StatusSuccess
                                    PaymentStatus.PARTIAL -> StatusWarning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Confirm Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%,.0f", c.confirmAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Received", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%,.0f", c.paymentReceived)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%,.0f", c.pendingAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusWarning)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                paymentAmountInput = if (c.pendingAmount > 0) "${c.pendingAmount.toInt()}" else ""
                                showPaymentDialog = true
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("record_payment_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Payment", fontWeight = FontWeight.Bold)
                        }

                        if (c.currentStatus != CustomerStatus.SUCCESS) {
                            OutlinedButton(
                                onClick = { showMarkSuccessDialog = true },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("mark_success_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Success", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Website Requirement & Scope Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Website Scope & Requirements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Website Type: ${c.websiteType}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = c.websiteRequirement.ifBlank { "No detailed requirements specified." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (c.requiredFeatures.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Required Features:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = c.requiredFeatures,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Inquiry Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dayFormat.format(Date(c.inquiryDate)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Follow-up Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (c.followUpDate > 0) dayFormat.format(Date(c.followUpDate)) else "Not Scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverdue) StatusError else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Activity Timeline Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Activity Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${logs.size} Events",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(logs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (log.category) {
                                    "PAYMENT" -> StatusSuccess.copy(alpha = 0.2f)
                                    "CALL" -> Color(0xFF00B0FF).copy(alpha = 0.2f)
                                    "WHATSAPP" -> Color(0xFF25D366).copy(alpha = 0.2f)
                                    else -> ElectricBlue.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (log.category) {
                                "PAYMENT" -> Icons.Default.AttachMoney
                                "CALL" -> Icons.Default.Call
                                "WHATSAPP" -> Icons.Default.Message
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (log.category) {
                                "PAYMENT" -> StatusSuccess
                                "CALL" -> Color(0xFF00B0FF)
                                "WHATSAPP" -> Color(0xFF25D366)
                                else -> ElectricBlue
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Record Payment Dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Customer Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Total Pending: ₹${String.format("%,.0f", c.pendingAmount)}", fontWeight = FontWeight.SemiBold, color = StatusWarning)

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Payment Received Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = paymentMethodInput,
                        onValueChange = { paymentMethodInput = it },
                        label = { Text("Payment Method (e.g. Bank Wire / Cash)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = paymentNoteInput,
                        onValueChange = { paymentNoteInput = it },
                        label = { Text("Reference / Notes (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordCustomerPayment(c, amount, paymentMethodInput, paymentNoteInput)
                            showPaymentDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_payment_btn")
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // WhatsApp Message Dialog
    if (showWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            title = { Text("Send WhatsApp Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Target: ${c.whatsappNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = whatsappMessageInput,
                        onValueChange = { whatsappMessageInput = it },
                        label = { Text("Message Preview") },
                        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("whatsapp_message_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            viewModel.sendWhatsappApi(c, whatsappMessageInput)
                            showWhatsAppDialog = false
                        }
                    ) {
                        Text("Send via API")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.logCustomerWhatsapp(c, whatsappMessageInput)
                            val cleanNumber = c.whatsappNumber.replace("+", "").replace(" ", "").replace("-", "")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(whatsappMessageInput)}")
                            }
                            context.startActivity(intent)
                            showWhatsAppDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("Open WhatsApp")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Mark Project Success Confirmation Dialog
    if (showMarkSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showMarkSuccessDialog = false },
            title = { Text("Finalize Project as SUCCESS?") },
            text = {
                Text("This will mark Project Status as 'Success', set Payment Status to 'Payment Received', settle all dues, and update revenue analytics.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markProjectSuccess(c)
                        showMarkSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = Color.Black)
                ) {
                    Text("Yes, Mark Success")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkSuccessDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Archive Confirmation Dialog
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Archive Customer Record?") },
            text = { Text("This inquiry will be moved to Archives. You can restore it anytime without data loss.") },
            confirmButton = {
                Button(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.archiveCustomer(c.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
