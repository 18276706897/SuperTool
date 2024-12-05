package dev.hasen.supertool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.fastForEachIndexed
import androidx.constraintlayout.compose.ConstraintLayout
import dev.hasen.supertool.ui.theme.SuperToolTheme
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperToolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun GreetingPreview() {
    SuperToolTheme {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Ring(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(20.dp), data = listOf(
                    "A" to Color.Red,
                    "B" to Color.Blue,
                    "C" to Color.Green,
                    "D" to Color.Yellow,
                    "E" to Color.Magenta,
                    "F" to Color.Cyan,
                    "G" to Color.Gray,
                )
            )
        }
    }
}


@Composable
fun Ring(modifier: Modifier, data: List<Pair<String, Color>>) {
    val clipPath = remember { Path() }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val radius = with(LocalDensity.current) {
            min(constraints.maxWidth.toDp(), constraints.maxHeight.toDp()) / 2f
        }
        val angle = 360f / data.size
        val itemHeight = 50.dp
        val itemWidth = with(LocalDensity.current) {
            (2 * radius.toPx() * sin(Math.toRadians(angle / 2.0))).toFloat().toDp()
        }


        ConstraintLayout(modifier = Modifier
            .size(radius * 2)
            .drawWithContent {

                drawCircle(Color.Black)

                drawIntoCanvas { canvas: Canvas ->
                    val path = Path()
                    path.addArc(
                        Rect(
                            itemHeight.toPx(),
                            itemHeight.toPx(),
                            radius.toPx() * 2 - itemHeight.toPx(),
                            radius.toPx() * 2 - itemHeight.toPx()
                        ), 0f, 360f
                    )
                    canvas.save()
                    canvas.clipPath(path, ClipOp.Difference)
                    data.fastForEachIndexed { index, pair ->
                        canvas.save()
                        canvas.nativeCanvas.rotate(angle * index, center.x, center.y)

                        clipPath.addArc(size.toRect(), -90 - angle / 2, angle)
                        clipPath.lineTo(center.x, center.y)
                        clipPath.close()
                        canvas.clipPath(clipPath)
                        drawRect(color = pair.second)

                        canvas.restore()

                    }
                    canvas.restore()
                }

                drawContent()

            }) {

            data.fastForEachIndexed { index, pair ->
                val ref = createRef()
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                        .constrainAs(ref) {
                            circular(parent, angle * index, radius - itemHeight / 2)
                            rotationZ = angle * index
                        },
//                    .drawWithContent {
//                        drawRect(pair.second)
//                        drawContent()
//                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(pair.first)
                }

            }
        }

    }
}
