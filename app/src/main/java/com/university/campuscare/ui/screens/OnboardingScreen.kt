package com.university.campuscare.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Report Issues Instantly",
            description = "Take photos and report campus facility problems with just a few taps"
        ),
        OnboardingPage(
            title = "Location Tracking",
            description = "Automatically tag issue locations to help maintenance teams respond faster"
        ),
        OnboardingPage(
            title = "Stay Updated",
            description = "Get real-time notifications about your reports and campus updates"
        ),
        OnboardingPage(
            title = "Direct Communication",
            description = "Chat directly with facilities admins for quick resolutions"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Content column (pager, indicators, navigation)
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    OnboardingPageContent(pages[page])
                }

                // Page indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        val color = if (pagerState.currentPage == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button
                    TextButton(
                        onClick = onNavigateToLogin
                    ) {
                        Text("Skip", color = androidx.compose.ui.graphics.Color.Black)
                    }

                    // Next/Get Started button
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onNavigateToLogin()
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFF0000)
                        ),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            // Skip button overlayed at top-right so it is always clickable above the pager
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Container - 128dp × 128dp with light pink background (#FEF2F2)
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color(0xFFFEF2F2)),
            contentAlignment = Alignment.Center
        ) {
            // Icon - 63.99dp × 63.99dp with clip
            Box(
                modifier = Modifier
                    .size(63.99.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (page.title) {
                    "Report Issues Instantly" -> Icons.Default.CameraAlt
                    "Location Tracking" -> Icons.Default.LocationOn
                    "Stay Updated" -> Icons.Default.Notifications
                    "Direct Communication" -> Icons.Default.Chat
                    else -> Icons.Default.CameraAlt
                }
                Icon(
                    imageVector = icon,
                    contentDescription = page.title,
                    modifier = Modifier.size(40.dp),
                    tint = androidx.compose.ui.graphics.Color(0xFFFF0000)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}