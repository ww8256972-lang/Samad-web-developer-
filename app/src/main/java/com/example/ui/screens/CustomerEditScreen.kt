package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.PaymentStatus
import com.example.models.PriorityLevel
import com.example.models.ProjectStatus
import com.example.ui.theme.ElectricBlue
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditScreen(
    customerId: String?,
    viewModel: MainViewModel
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    val isEdit = customerId != null
    val existingCustomer by if (isEdit) {
        viewModel.getCustomerStream(customerId!!).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<Customer?>(null) }
    }

    // Form fields
    var customId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    var websiteRequirement by remember { mutableStateOf("") }
    var websiteType by remember { mutableStateOf("Corporate / Business") }
    var requiredFeatures by remember { mutableStateOf("") }

    var estimatedAmount by remember { mutableStateOf("") }
    var confirmAmount by remember { mutableStateOf("") }
    var pendingAmount by remember { mutableStateOf("") }

    var followUpDateMillis by remember { mutableLongStateOf(0L) }
    var selectedStatus by remember { mutableStateOf(CustomerStatus.NEW_INQUIRY) }
    var selectedPriority by remember { mutableStateOf(PriorityLevel.MEDIUM) }
    var notes by remember { mutableStateOf("") }

    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Populate for editing
    LaunchedEffect(existingCustomer) {
        if (existingCustomer != null) {
            val c = existingCustomer!!
            customId = c.customerId
            customerName = c.customerName
            mobileNumber = c.mobileNumber
            whatsappNumber = c.whatsappNumber
            businessName = c.businessName
            businessType = c.businessType
            address = c.address
            city = c.city
            state = c.state
            websiteRequirement = c.websiteRequirement
            websiteType = c.websiteType
            requiredFeatures = c.requiredFeatures
            estimatedAmount = if (c.estimatedProjectAmount > 0) c.estimatedProjectAmount.toInt().toString() else ""
            confirmAmount = if (c.confirmAmount > 0) c.confirmAmount.toInt().toString() else ""
            pendingAmount = if (c.pendingAmount > 0) c.pendingAmount.toInt().toString() else ""
            followUpDateMillis = c.followUpDate
            selectedStatus = c.currentStatus
            selectedPriority = c.priority
            notes = c.notes
        } else if (!isEdit && customId.isEmpty()) {
            customId = viewModel.getNextSuggestedId()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("customer_edit_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = if (isEdit) "Edit Customer Record" else "New Customer Inquiry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val est = estimatedAmount.toDoubleOrNull() ?: 0.0
                        val conf = confirmAmount.toDoubleOrNull() ?: est
                        val pend = pendingAmount.toDoubleOrNull() ?: (conf - (existingCustomer?.paymentReceived ?: 0.0)).coerceAtLeast(0.0)

                        val record = existingCustomer?.copy(
                            customerId = customId.trim(),
                            customerName = customerName.trim(),
                            mobileNumber = mobileNumber.trim(),
                            whatsappNumber = whatsappNumber.ifBlank { mobileNumber }.trim(),
                            businessName = businessName.trim(),
                            businessType = businessType.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            state = state.trim(),
                            websiteRequirement = websiteRequirement.trim(),
                            websiteType = websiteType.trim(),
                            requiredFeatures = requiredFeatures.trim(),
                            estimatedProjectAmount = est,
                            confirmAmount = conf,
                            pendingAmount = pend,
                            followUpDate = followUpDateMillis,
                            currentStatus = selectedStatus,
                            priority = selectedPriority,
                            notes = notes.trim()
                        ) ?: Customer(
                            id = UUID.randomUUID().toString(),
                            customerId = customId.trim().ifBlank { "CUST-${System.currentTimeMillis() % 10000}" },
                            customerName = customerName.trim(),
                            mobileNumber = mobileNumber.trim(),
                            whatsappNumber = whatsappNumber.ifBlank { mobileNumber }.trim(),
                            businessName = businessName.trim(),
                            businessType = businessType.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            state = state.trim(),
                            websiteRequirement = websiteRequirement.trim(),
                            websiteType = websiteType.trim(),
                            requiredFeatures = requiredFeatures.trim(),
                            estimatedProjectAmount = est,
                            confirmAmount = conf,
                            pendingAmount = conf,
                            followUpDate = followUpDateMillis,
                            currentStatus = selectedStatus,
                            priority = selectedPriority,
                            notes = notes.trim()
                        )

                        viewModel.saveCustomer(record, isEdit) { success ->
                            if (success) viewModel.navigateBack()
                        }
                    },
                    enabled = customerName.isNotBlank() && mobileNumber.isNotBlank(),
                    modifier = Modifier.testTag("save_customer_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Customer Identity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Customer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customId,
                            onValueChange = { customId = it },
                            label = { Text("Customer ID *") },
                            modifier = Modifier.weight(1f).testTag("input_customer_id"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name *") },
                            modifier = Modifier.weight(1.5f).testTag("input_customer_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = {
                                mobileNumber = it
                                if (whatsappNumber.isBlank()) whatsappNumber = it
                            },
                            label = { Text("Mobile Phone *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f).testTag("input_mobile_number"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it },
                            label = { Text("WhatsApp Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f).testTag("input_whatsapp_number"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business / Company Name") },
                            modifier = Modifier.weight(1.2f).testTag("input_business_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = businessType,
                            onValueChange = { businessType = it },
                            label = { Text("Business Type / Niche") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f).testTag("input_city"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State / Province") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section 2: Website Scope & Requirements
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Website Inquiry Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = websiteType,
                        onValueChange = { websiteType = it },
                        label = { Text("Website Type (E-Commerce, Corporate, Blog, Portal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = websiteRequirement,
                        onValueChange = { websiteRequirement = it },
                        label = { Text("Website Requirement Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = requiredFeatures,
                        onValueChange = { requiredFeatures = it },
                        label = { Text("Required Features (e.g. Booking, Payment, Admin)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section 3: Financials & Project Budget
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Financial Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = estimatedAmount,
                            onValueChange = {
                                estimatedAmount = it
                                if (confirmAmount.isBlank()) confirmAmount = it
                            },
                            label = { Text("Estimated (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_est_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmAmount,
                            onValueChange = { confirmAmount = it },
                            label = { Text("Confirm Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_confirm_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = pendingAmount,
                            onValueChange = { pendingAmount = it },
                            label = { Text("Pending Due (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Section 4: Follow-up & Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Status & Follow-up Scheduling", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Follow-up Date Picker Trigger
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            if (followUpDateMillis > 0) cal.timeInMillis = followUpDateMillis
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 12, 0, 0)
                                    }
                                    followUpDateMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("select_followup_date_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (followUpDateMillis > 0)
                                "Follow-up Date: ${dateFormat.format(Date(followUpDateMillis))}"
                            else "Set Follow-up Date",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Status Dropdown
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            CustomerStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.displayName) },
                                    onClick = {
                                        selectedStatus = status
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority Dropdown
                    ExposedDropdownMenuBox(
                        expanded = priorityExpanded,
                        onExpandedChange = { priorityExpanded = !priorityExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPriority.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            PriorityLevel.entries.forEach { priority ->
                                DropdownMenuItem(
                                    text = { Text(priority.displayName) },
                                    onClick = {
                                        selectedPriority = priority
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Internal Admin Remarks / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
