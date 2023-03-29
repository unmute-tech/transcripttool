package io.reitmaier.transcripttool.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.roundToInt

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun PlayPauseButton(
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  IconToggleButton(
    checked = isPlaying,
    onCheckedChange = { onClick() },
    modifier = modifier,
  ) {
    val transition = updateTransition(isPlaying, label = "Play/Pause indicator")

    val tint by transition.animateColor(
      label = "Tint",
    ) { isChecked ->
      if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    }

    val size by transition.animateDp(
      transitionSpec = {
        if (false isTransitioningTo true) {
          keyframes {
            durationMillis = 250
            30.dp at 0 with LinearOutSlowInEasing // for 0-15 ms
            35.dp at 15 with FastOutLinearInEasing // for 15-75 ms
            40.dp at 75 // ms
            35.dp at 150 // ms
          }
        } else {
          spring(stiffness = Spring.StiffnessVeryLow)
        }
      },
      label = "Size",
    ) { 40.dp }

    Icon(
      imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.Replay,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(size),
    )
  }
}
fun Float.roundTo(numFractionDigits: Int): Float {
  val factor = 10.0.pow(numFractionDigits.toDouble())
  return ((this * factor).roundToInt() / factor).toFloat()
}
