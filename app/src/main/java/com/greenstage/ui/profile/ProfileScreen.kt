package com.greenstage.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenstage.app.ui.theme.OnSoftCoral
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.OnSurfaceVariant
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.OutlineVariant
import com.greenstage.app.ui.theme.PowderBlue
import com.greenstage.app.ui.theme.SoftCoral
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.home.ScreenHeading

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> LoadingIndicator(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            is ProfileUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.loadProfile() },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            is ProfileUiState.Success -> {
                val user = state.user
                val initials = user.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "U" }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ScreenHeading(title = "Student Profile")

                    val shape = RoundedCornerShape(8.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, shape)
                            .border(2.dp, Outline, shape),
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(PowderBlue, RoundedCornerShape(8.dp))
                                        .border(2.dp, Outline, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        initials,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = OnSurfaceCard,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        user.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = OnSurfaceCard,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        modifier = Modifier.border(2.dp, Outline, RoundedCornerShape(50)),
                                        color = com.greenstage.app.ui.theme.SoftYellow,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            user.accountType.name,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OnSurfaceCard
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                thickness = 2.dp,
                                color = OutlineVariant
                            )
                            ProfileField("Full Name", user.name)
                            ProfileField("Email Address", user.email)
                            ProfileField("Account Type", user.accountType.name)
                        }
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(6.dp))
                            .border(2.dp, Outline, RoundedCornerShape(6.dp)),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoral,
                            contentColor = OnSoftCoral
                        )
                    ) {
                        Text("↪  Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceCard)
    }
}
