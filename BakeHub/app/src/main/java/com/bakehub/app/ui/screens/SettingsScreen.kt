package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.MockData
import com.bakehub.app.data.UserRepository
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White
import kotlinx.coroutines.launch

/**
 * Settings — reached from Profile & Achievements. Lets the user adjust the
 * preferences captured at registration (design document, Section 2.1.1) and
 * notification preferences (Section 2.1.9).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onLogOut: () -> Unit) {
    val liveProfile by UserRepository.profile.collectAsState()
    var unitPreference by remember(liveProfile) {
        mutableStateOf(liveProfile?.unitPreference ?: MockData.currentUser.unitPreference)
    }
    var timerNotifications by remember { mutableStateOf(true) }
    var streakReminders by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DarkBrown)
            }
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkBrown)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        SettingsRow(label = "Unit preference: $unitPreference") {
            Switch(
                checked = unitPreference == "Imperial",
                onCheckedChange = {
                    val newPref = if (it) "Imperial" else "Metric"
                    unitPreference = newPref
                    scope.launch { UserRepository.setUnitPreference(newPref) }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Terracotta),
            )
        }
        SettingsRow(label = "Bake timer notifications") {
            Switch(
                checked = timerNotifications,
                onCheckedChange = { timerNotifications = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Terracotta),
            )
        }
        SettingsRow(label = "Streak reminder notifications") {
            Switch(
                checked = streakReminders,
                onCheckedChange = { streakReminders = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Terracotta),
            )
        }

        Button(
            onClick = onLogOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = White),
        ) {
            Text("Log Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DarkBrown)
        control()
    }
}
