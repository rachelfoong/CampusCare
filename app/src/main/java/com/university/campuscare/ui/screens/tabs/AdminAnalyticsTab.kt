package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.viewmodel.AdminViewModel

@Composable
fun AdminAnalyticsTab(viewModel: AdminViewModel) {
    val analyticsData by viewModel.analyticsData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Report Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Statistics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                AnalyticsRow("Total Reports This Month", analyticsData.totalReportsThisMonth.toString())
                AnalyticsRow("Average Resolution Time", analyticsData.averageResolutionTime)
                AnalyticsRow("Most Reported Issue", analyticsData.mostReportedIssue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Category Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (analyticsData.categoryBreakdown.isEmpty()) {
                    Text(
                        text = "No data available",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    val categories = analyticsData.categoryBreakdown.entries.sortedByDescending { it.value }
                    val colors = listOf(
                        Color(0xFFFF0000), // Primary Red
                        Color(0xFFD32F2F), // Dark Red
                        Color(0xFFFF5252), // Light Red
                        Color(0xFFFF8A80)  // Lighter Red
                    )

                    categories.forEachIndexed { index, (category, percentage) ->
                        val color = colors.getOrElse(index) { Color(0xFFFF0000) }
                        CategoryBar(category, percentage, color)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000)
        )
    }
}

@Composable
fun CategoryBar(label: String, percentage: Int, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp)
            Text(text = "$percentage%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = Color(0xFFFFEBEB)
        )
    }
}