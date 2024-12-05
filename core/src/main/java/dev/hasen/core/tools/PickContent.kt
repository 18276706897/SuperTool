package dev.hasen.core.tools

/**
 * 截取组件 temp
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

interface PickContentRequest {
    suspend fun requestContent(): ImageBitmap
}

fun PickContentRequest(): PickContentRequest = PickContentRequestImpl()

fun Modifier.pickContent(request: PickContentRequest) =
    pickContentImpl(request as PickContentRequestImpl)


private fun Modifier.pickContentImpl(request: PickContentRequestImpl) = drawWithContent {

    if (request.hasPickContentRequest()) {
        val bitmap = request.withPickDrawContext(this) {
            drawContent()
        }
        request.setPickContent(bitmap)
    }
    drawContent()
}

private class PickContentRequestImpl : PickContentRequest {

    private var pickContinuation: Continuation<ImageBitmap>? by mutableStateOf(null)


    fun hasPickContentRequest() = pickContinuation != null


    fun setPickContent(bitmap: ImageBitmap?) {
        val continuation = pickContinuation ?: return
        pickContinuation = null
        if (bitmap != null) {
            continuation.resume(bitmap)
        } else {
            continuation.resumeWith(Result.failure(Exception("Failed to pick content")))
        }
    }

    override suspend fun requestContent(): ImageBitmap {
        val old = pickContinuation
        val result = runCatching {
            suspendCoroutine { bitmapContinuation ->
                pickContinuation = bitmapContinuation
            }
        }
        old?.resumeWith(result)
        return result.getOrThrow()
    }


    inline fun withPickDrawContext(drawScope: DrawScope, block: () -> Unit): ImageBitmap? {
        if (drawScope.size.isEmpty()) {
            return null
        }
        val pickBitmap = ImageBitmap(
            drawScope.size.width.roundToInt(),
            drawScope.size.height.roundToInt(),
        )
        val pickCanvas = Canvas(pickBitmap)
        val oldCanvas = drawScope.drawContext.canvas
        try {
            drawScope.drawContext.canvas = pickCanvas
            block()
        } finally {
            drawScope.drawContext.canvas = oldCanvas
        }
        return pickBitmap

    }
}