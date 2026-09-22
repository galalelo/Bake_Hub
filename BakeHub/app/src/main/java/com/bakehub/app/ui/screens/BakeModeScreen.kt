package com.bakehub.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.Recipe
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.WarmGrey
import com.bakehub.app.ui.theme.White
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * 3.6 Bake Mode — guides the user hands-free through a recipe's steps and awards
 * gamification progress on completion (design document, Section 2.1.4 and 2.3.1).
 * Navigates to: back to Recipe Detail on exit/completion.
 */
@Composable
fun BakeModeScreen(
    recipe: Recipe,
    onExit: () -> Unit,
    onCompleted: () -> Unit,
) {
    // 2.1.4 — "The device screen is prevented from auto-locking for as long as
    // Bake Mode is open." Implemented with FLAG_KEEP_SCREEN_ON on the host Activity.
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var stepIndex by remember { mutableIntStateOf(0) }
    val step = recipe.steps[stepIndex]

    var secondsLeft by remember(stepIndex) { mutableIntStateOf(step.timerSeconds ?: 0) }
    var running by remember(stepIndex) { mutableStateOf(step.timerSeconds != null) }

    LaunchedEffect(stepIndex, running) {
        while (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    )
    {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bake Mode", color = GreyText, fontWeight = FontWeight.Medium)
            Text("Step ${stepIndex + 1}/${recipe.steps.size}", color = GreyText)
        }

        //progress bar
        LinearProgressIndicator(
            progress = { (stepIndex + 1f) / recipe.steps.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 28.dp)
                .height(6.dp),
            color = Terracotta,
            trackColor = WarmGrey,
        )

        Text(
            step.title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = DarkBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            step.instruction,
            color = GreyText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 32.dp),
        )

        if (step.timerSeconds != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formatTime(secondsLeft), color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                }
                Text(
                    //add button here for pause timer and resume
                    if (running) " Pause Timer" else " Resume Timer",
                    color = DarkBrown,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .then(Modifier),
                )
            }
        }

        ///////////////////////////////////////////////////////////////////
        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        )
        {
            OutlinedButton(
                onClick = {
                    if (stepIndex > 0) {
                        stepIndex--
                    } else {
                        onExit()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
            )
            {
                Text(
                    if (stepIndex > 0) "◀ Back" else "Exit",
                    color = DarkBrown,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    if (stepIndex < recipe.steps.size - 1) {
                        stepIndex++
                    } else {
                        onCompleted()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = White
                ),
            )
            {
                Text(
                    if (stepIndex < recipe.steps.size - 1) {
                        "Next ▶"
                    } else {
                        "Finish ✓"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Screen awake message
        Text(
            text = "Screen will stay awake during this bake",
            color = GreyText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
            ////////////////////////////////////////////////////////////////////

    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
