package com.meiyinet.compass.customcompass.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume


fun Modifier.screenshot(screenshotManager: ScreenshotManager): Modifier =
    then(ScreenshotElement(screenshotManager))


class ScreenshotElement(private val screenshotManager: ScreenshotManager) :
    ModifierNodeElement<ScreenshotModifierNode>() {


    override fun create(): ScreenshotModifierNode {
        return ScreenshotModifierNode(screenshotManager as ScreenshotManagerImpl)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenshotElement) return false

        if (screenshotManager != other.screenshotManager) return false

        return true
    }

    override fun hashCode(): Int {
        return screenshotManager.hashCode()
    }

    override fun update(node: ScreenshotModifierNode) {
        node.managerImpl = screenshotManager as ScreenshotManagerImpl
    }
}

class ScreenshotModifierNode(var managerImpl: ScreenshotManagerImpl) :
    Modifier.Node(), DrawModifierNode {


    override fun ContentDrawScope.draw() {
        managerImpl.withTakeScreenshot(onRecord = {
            val graphicsContext = requireGraphicsContext()
            val layer = graphicsContext.createGraphicsLayer()
            val contentDrawScope: ContentDrawScope = this
            try {
                layer.record(contentDrawScope) {
                    drawContent()
                }
            } catch (e: Exception) {
                graphicsContext.releaseGraphicsLayer(layer)
            }
            graphicsContext to layer
        }) {
            try {
                second.toImageBitmap()
            } finally {
                first.releaseGraphicsLayer(second)
            }
        }
        drawContent()
    }


    private fun GraphicsLayer.record(
        contentDrawScope: ContentDrawScope,
        density: Density = contentDrawScope,
        layoutDirection: LayoutDirection = contentDrawScope.layoutDirection,
        size: IntSize = contentDrawScope.size.toIntSize(),
        block: ContentDrawScope.() -> Unit
    ) = record(density, layoutDirection, size) {
        drawIntoCanvas { canvas ->
            contentDrawScope.draw(
                density,
                layoutDirection,
                canvas,
                Size(size.width.toFloat(), size.height.toFloat())
            ) {
                block(contentDrawScope)
            }
        }
    }

}


interface ScreenshotManager {
    suspend fun takeScreenshot(): Result<ImageBitmap>
}

fun ScreenshotManager(): ScreenshotManager {
    return ScreenshotManagerImpl()
}


class ScreenshotManagerImpl : ScreenshotManager {

    private var requestScreenshot by mutableStateOf(false)


   internal fun <T> withTakeScreenshot(onRecord: () -> T, block: suspend T.() -> ImageBitmap) {
        if (!requestScreenshot) {
            return
        }
        requestScreenshot = false
        val screenshotSnapshotContinuation = screenshotSnapshotContinuation ?: return
        val t = try {
            onRecord()
        } catch (e: Exception) {
            screenshotSnapshotContinuation.resumeWith(Result.failure(e))
            this.screenshotSnapshotContinuation = null
            return
        }
        block.createCoroutine(t, screenshotSnapshotContinuation).resume(Unit)
        this.screenshotSnapshotContinuation = null
    }


    private var screenshotSnapshotContinuation: Continuation<ImageBitmap>? = null

    override suspend fun takeScreenshot(): Result<ImageBitmap> = withContext(Dispatchers.Main) {
        val oldContinuation = screenshotSnapshotContinuation
        val result = runCatching {
            suspendCancellableCoroutine {
                screenshotSnapshotContinuation = it
                requestScreenshot = true
                it.invokeOnCancellation {
                    requestScreenshot = false
                    screenshotSnapshotContinuation = null
                }
            }
        }
        oldContinuation?.resumeWith(result)
        result
    }
}
