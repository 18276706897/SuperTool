package dev.hasen.core.tools.keyboard

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView

//软键盘弹起检测工具
@Composable
fun KeyboardVisibilityListener(
    onKeyboardVisibilityChanged: (Boolean) -> Unit
) {
    val rootView = getTopMostParentView()
    var isKeyboardVisible by remember { mutableStateOf(false) }

    DisposableEffect(rootView) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val keyboardVisible = keypadHeight > screenHeight * 0.15 // Assume keyboard is visible if more than 15% of the screen is occupied by it

            if (keyboardVisible != isKeyboardVisible) {
                isKeyboardVisible = keyboardVisible
                onKeyboardVisibilityChanged(keyboardVisible)
            }
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
}



@Composable
fun getTopMostParentView(): View {
    val currentView = LocalView.current
    var parentView: View = currentView

    while (parentView.parent != null && parentView.parent is View) {
        parentView = parentView.parent as View
    }

    return parentView
}