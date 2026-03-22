package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

        // Category Breakdown Card (Updated with Donut Chart)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Category Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (analyticsData.categoryBreakdown.isEmpty()) {
                    Text(
                        text = "No data available",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    // Define custom color palette for the chart
                    val chartColors = listOf(
                        Color(0xFFFF0000), // Primary Red
                        Color(0xFF2196F3), // Bright Blue
                        Color(0xFF4CAF50), // Green
                        Color(0xFFFF9800), // Orange
                        Color(0xFF9C27B0), // Purple
                        Color(0xFF00BCD4)  // Teal (Fallback for extra categories)
                    )

                    // Draw the Donut Chart
                    CategoryDonutChart(
                        categoryData = analyticsData.categoryBreakdown,
                        totalReports = analyticsData.categoryBreakdown.values.sum(),
                        colors = chartColors
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Draw the Legend in a responsive grid
                    val categories = analyticsData.categoryBreakdown.entries.toList()
                    categories.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowItems.forEachIndexed { _, entry ->
                                val globalIndex = categories.indexOf(entry)
                                val color = chartColors.getOrElse(globalIndex) { Color.Gray }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(color, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${entry.key} (${entry.value}%)",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                            // Fill empty space if there is an odd number of categories
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
fun CategoryDonutChart(
    categoryData: Map<String, Int>,
    totalReports: Int,
    colors: List<Color>
) {
    val total = categoryData.values.sum().coerceAtLeast(1) // Prevent divide by zero

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            var startAngle = -90f // Start at the top (12 o'clock)
            val strokeWidth = 32.dp.toPx()

            categoryData.values.forEachIndexed { index, amount ->
                val sweepAngle = (amount.toFloat() / total) * 360f
                val color = colors.getOrElse(index) { Color.Gray }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalReports.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Total",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}