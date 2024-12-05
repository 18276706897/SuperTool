package dev.gtmi.supertools.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * 空的点击效果
 */
fun Modifier.emptyClickable(
    intervalMillis: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > intervalMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    )
}