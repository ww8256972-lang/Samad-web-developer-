package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.DashboardSummary
import com.example.models.PaymentStatus
import com.example.models.ProjectStatus
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val allCustomers by viewModel.allActiveCustomers.collectAsState()

    val overdueCustomers = allCustomers.filter {
        val now = System.currentTimeMillis()
        it.followUpDate in 1 until now &&
            it.currentStatus != CustomerStatus.SUCCESS &&
            it.currentStatus != CustomerStatus.CANCELLED &&
            it.currentStatus != CustomerStatus.WEBSITE_COMPLETED
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PK")).apply {
        maximumFractionDigits = 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome & System Title
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time Inquiry & Revenue Tracker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricBlue.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "15K+ Scalable DB",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overdue Alert Banner if any overdue
        if (summary.overdueFollowups > 0) {
            item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleOverdueFilter()
                                viewModel.navigateTo(Screen.CustomerList)
                            }
                            .testTag("overdue_alert_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusError.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Overdue Alert",
                                tint = StatusError,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ATTENTION: ${summary.overdueFollowups} OVERDUE Follow-ups!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusError
                            )
                            Text(
                                text = "Customer reply deadlines have passed. Tap to view and send instant WhatsApp reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Financial & Revenue Glass Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revenue_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.6f), NeonPink.copy(alpha = 0.6f)))
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Financial Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Auto-Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Payment Received",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%,.0f", summary.totalRevenue)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Pending Amount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%,.0f", summary.totalPendingRevenue)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Confirmed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%,.0f", summary.totalConfirmAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricBlue
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Success Delivered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%,.0f", summary.totalSuccessAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonPink
                            )
                        }
                    }
                }
            }
        }

        // Key Operations Grid
        item {
            Text(
                text = "Inquiry & Pipeline Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                MetricTile(
                    title = "Total Records",
                    value = "${summary.totalCustomers}",
                    subtitle = "Safe 15,000+ DB",
                    icon = Icons.Default.People,
                    color = ElectricBlue,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.clearAllFilters()
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )

                MetricTile(
                    title = "New Inquiries",
                    value = "${summary.newInquiries}",
                    subtitle = "Incoming leads",
                    icon = Icons.Default.Assignment,
                    color = Color(0xFF00B0FF),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setStatusFilter(CustomerStatus.NEW_INQUIRY)
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )

                MetricTile(
                    title = "Today's Follow-up",
                    value = "${summary.followupsToday}",
                    subtitle = "Action required today",
                    icon = Icons.Default.EventNote,
                    color = StatusWarning,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.toggleTodayFilter()
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )

                MetricTile(
                    title = "In Progress",
                    value = "${summary.inProgressProjects}",
                    subtitle = "Development active",
                    icon = Icons.Default.HourglassTop,
                    color = NeonPink,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setStatusFilter(CustomerStatus.IN_PROGRESS)
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )

                MetricTile(
                    title = "Success Completed",
                    value = "${summary.completedProjects}",
                    subtitle = "Delivered & Cleared",
                    icon = Icons.Default.CheckCircle,
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setStatusFilter(CustomerStatus.SUCCESS)
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )

                MetricTile(
                    title = "Pending Dues",
                    value = "${summary.pendingPaymentsCount}",
                    subtitle = "Customers with dues",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFFFF9100),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.togglePendingPaymentFilter()
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )
            }
        }

        // Quick Actions Row
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.CustomerEdit(null)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_add_customer_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Customer", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.AskAi) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_ask_ai_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeonPink)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI Guide", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Recent Inquiries Preview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Customer Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All (${allCustomers.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricBlue,
                    modifier = Modifier.clickable {
                        viewModel.clearAllFilters()
                        viewModel.navigateTo(Screen.CustomerList)
                    }
                )
            }
        }

        items(allCustomers.take(4)) { customer ->
            DashboardCustomerItemCard(
                customer = customer,
                onClick = { viewModel.navigateTo(Screen.CustomerDetail(customer.id)) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp)) // padding for bottom nav
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("metric_tile_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DashboardCustomerItemCard(
    customer: Customer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dashboard_cust_card_${customer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricBlue.copy(alpha = 0.2f), NeonPink.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.customerName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = customer.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customer.customerId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = customer.businessName.ifBlank { customer.websiteRequirement },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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

                    if (customer.pendingAmount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Due: ₹${String.format("%,.0f", customer.pendingAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
