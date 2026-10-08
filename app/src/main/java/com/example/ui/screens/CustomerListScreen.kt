package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.Customer
import com.example.models.CustomerStatus
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
fun CustomerListScreen(viewModel: MainViewModel) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val context = LocalContext.current

    val now = System.currentTimeMillis()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_list_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer Records",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${customers.size} records found (Indexed 15,000+ support)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Real-Time Filter",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name, ID, phone, business, city...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricBlue)
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterState.onlyOverdue,
                    onClick = { viewModel.toggleOverdueFilter() },
                    label = { Text("⏰ Overdue") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusError.copy(alpha = 0.2f),
                        selectedLabelColor = StatusError
                    ),
                    modifier = Modifier.testTag("chip_overdue")
                )

                FilterChip(
                    selected = filterState.onlyToday,
                    onClick = { viewModel.toggleTodayFilter() },
                    label = { Text("📅 Today's Follow-up") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusWarning.copy(alpha = 0.2f),
                        selectedLabelColor = StatusWarning
                    ),
                    modifier = Modifier.testTag("chip_today")
                )

                FilterChip(
                    selected = filterState.onlyPendingPayment,
                    onClick = { viewModel.togglePendingPaymentFilter() },
                    label = { Text("💰 Due Payment") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricBlue
                    ),
                    modifier = Modifier.testTag("chip_due_payment")
                )

                FilterChip(
                    selected = filterState.selectedStatus == CustomerStatus.NEW_INQUIRY,
                    onClick = {
                        val next = if (filterState.selectedStatus == CustomerStatus.NEW_INQUIRY) null else CustomerStatus.NEW_INQUIRY
                        viewModel.setStatusFilter(next)
                    },
                    label = { Text("New Inquiry") }
                )

                FilterChip(
                    selected = filterState.selectedStatus == CustomerStatus.IN_PROGRESS,
                    onClick = {
                        val next = if (filterState.selectedStatus == CustomerStatus.IN_PROGRESS) null else CustomerStatus.IN_PROGRESS
                        viewModel.setStatusFilter(next)
                    },
                    label = { Text("In Progress") }
                )

                FilterChip(
                    selected = filterState.selectedStatus == CustomerStatus.SUCCESS,
                    onClick = {
                        val next = if (filterState.selectedStatus == CustomerStatus.SUCCESS) null else CustomerStatus.SUCCESS
                        viewModel.setStatusFilter(next)
                    },
                    label = { Text("Success") }
                )

                if (filterState.searchQuery.isNotBlank() || filterState.selectedStatus != null ||
                    filterState.onlyOverdue || filterState.onlyToday || filterState.onlyPendingPayment
                ) {
                    IconButton(
                        onClick = { viewModel.clearAllFilters() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear all filters", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Records List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("customer_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No customer records found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search criteria or tap '+ New Customer'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("customer_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        val isOverdue = customer.followUpDate in 1 until now &&
                            customer.currentStatus != CustomerStatus.SUCCESS &&
                            customer.currentStatus != CustomerStatus.CANCELLED &&
                            customer.currentStatus != CustomerStatus.WEBSITE_COMPLETED

                        CustomerItemCard(
                            customer = customer,
                            isOverdue = isOverdue,
                            onCardClick = { viewModel.navigateTo(Screen.CustomerDetail(customer.id)) },
                            onQuickCall = {
                                viewModel.logCustomerCall(customer)
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${customer.mobileNumber}")
                                }
                                context.startActivity(dialIntent)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Floating Action Button to Add Customer
        FloatingActionButton(
            onClick = { viewModel.navigateTo(Screen.CustomerEdit(null)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp)
                .testTag("fab_add_customer"),
            containerColor = ElectricBlue,
            contentColor = androidx.compose.ui.graphics.Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    isOverdue: Boolean,
    onCardClick: () -> Unit,
    onQuickCall: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        border = BorderStroke(
            1.dp,
            if (isOverdue) StatusError.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: ID, Overdue Badge, Quick Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = customer.customerId,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }

                    if (isOverdue) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusError.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OVERDUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError
                                )
                            }
                        }
                    }
                }

                // Quick Call Button
                IconButton(
                    onClick = onQuickCall,
                    modifier = Modifier.size(36.dp).testTag("quick_call_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${customer.customerName}",
                        tint = StatusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Name & Business
            Text(
                text = customer.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${customer.businessName} • ${customer.city}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Requirement summary
            if (customer.websiteRequirement.isNotBlank()) {
                Text(
                    text = "🌐 ${customer.websiteRequirement}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial & Status Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (customer.currentStatus) {
                        CustomerStatus.SUCCESS -> StatusSuccess.copy(alpha = 0.15f)
                        CustomerStatus.IN_PROGRESS -> NeonPink.copy(alpha = 0.15f)
                        CustomerStatus.CONFIRMED -> ElectricBlue.copy(alpha = 0.15f)
                        CustomerStatus.FOLLOW_UP -> StatusWarning.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = customer.currentStatus.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (customer.currentStatus) {
                            CustomerStatus.SUCCESS -> StatusSuccess
                            CustomerStatus.IN_PROGRESS -> NeonPink
                            CustomerStatus.CONFIRMED -> ElectricBlue
                            CustomerStatus.FOLLOW_UP -> StatusWarning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Balance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.pendingAmount > 0) {
                        Text(
                            text = "Due: ₹${String.format("%,.0f", customer.pendingAmount)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    } else if (customer.confirmAmount > 0) {
                        Text(
                            text = "Paid: ₹${String.format("%,.0f", customer.paymentReceived)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                }
            }

            if (customer.followUpDate > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Follow-up: ${dateFormat.format(Date(customer.followUpDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
