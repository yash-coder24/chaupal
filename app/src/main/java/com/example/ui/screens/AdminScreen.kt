package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminMetrics
import com.example.data.model.ReportEntity
import com.example.data.model.UserSummary
import com.example.ui.components.UserAvatar

enum class AdminTab { METRICS, REPORTS, USERS }

@Composable
fun AdminScreen(
    metrics: AdminMetrics,
    reports: List<ReportEntity>,
    allUsers: List<UserSummary>,
    onToggleBanUser: (String) -> Unit,
    onUpdateReportStatus: (reportId: String, status: String) -> Unit,
    onDeletePost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AdminTab.METRICS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_screen")
    ) {
        // Admin Header
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chaupal Admin Console",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == AdminTab.METRICS,
                        onClick = { selectedTab = AdminTab.METRICS },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = selectedTab == AdminTab.REPORTS,
                        onClick = { selectedTab = AdminTab.REPORTS },
                        text = { Text("Reports (${reports.count { it.status == "PENDING" }})") }
                    )
                    Tab(
                        selected = selectedTab == AdminTab.USERS,
                        onClick = { selectedTab = AdminTab.USERS },
                        text = { Text("Users (${allUsers.size})") }
                    )
                }
            }
        }

        when (selectedTab) {
            AdminTab.METRICS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Platform Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(title = "Total Users", value = "${metrics.totalUsers}", modifier = Modifier.weight(1f))
                            MetricCard(title = "Active Accounts", value = "${metrics.activeUsers}", modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(title = "Total Posts", value = "${metrics.totalPosts}", modifier = Modifier.weight(1f))
                            MetricCard(title = "Discussions", value = "${metrics.totalComments}", modifier = Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(title = "Reactions", value = "${metrics.totalLikes}", modifier = Modifier.weight(1f))
                            MetricCard(
                                title = "Pending Flags",
                                value = "${metrics.pendingReportsCount}",
                                isWarning = metrics.pendingReportsCount > 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            AdminTab.REPORTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (reports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No reports pending. Community is safe!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(reports, key = { it.id }) { r ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = r.targetType,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "Status: ${r.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Reason: ${r.reason}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Target ID: ${r.targetId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (r.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (r.targetType == "POST") {
                                                        onDeletePost(r.targetId)
                                                    }
                                                    onUpdateReportStatus(r.id, "RESOLVED")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Resolve & Delete", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { onUpdateReportStatus(r.id, "DISMISSED") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Dismiss", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.USERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers, key = { it.user.id }) { u ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    avatarUrl = u.profile.avatarUrl,
                                    displayName = u.profile.fullName,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = u.profile.fullName, fontWeight = FontWeight.Bold)
                                        if (u.user.isAdmin) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(Admin)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(text = "@${u.user.username} • ${u.user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (!u.user.isAdmin) {
                                    Button(
                                        onClick = { onToggleBanUser(u.user.id) },
                                        colors = if (u.user.isBanned) ButtonDefaults.buttonColors()
                                        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (u.user.isBanned) "Unban" else "Ban", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
