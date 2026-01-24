package xyz.zt.mindbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

data class Node(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color
)

@Composable
fun MindGraphBackground(modifier: Modifier = Modifier, nodeCount: Int = 18) {
    // Usamos el color primario del tema para los nodos
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outline

    val nodes = remember {
        List(nodeCount) {
            Node(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.003f, // Velocidad un poco más fluida
                vy = (Random.nextFloat() - 0.5f) * 0.003f,
                color = primaryColor.copy(alpha = Random.nextFloat().coerceAtLeast(0.4f))
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "graphAnimation")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "frameUpdate"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Forzar recomposición con el progreso de la animación
        animProgress

        // 1. Actualizar posiciones y rebotar en los bordes
        nodes.forEach { node ->
            node.x += node.vx
            node.y += node.vy

            if (node.x <= 0f || node.x >= 1f) node.vx *= -1f
            if (node.y <= 0f || node.y >= 1f) node.vy *= -1f
        }

        // 2. Dibujar Conexiones (Bucle corregido)
        for (i in 0 until nodes.size) {
            for (j in i + 1 until nodes.size) {
                val nodeA = nodes[i]
                val nodeB = nodes[j]

                val dx = (nodeA.x - nodeB.x) * width
                val dy = (nodeA.y - nodeB.y) * height
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                // Solo dibujar línea si los puntos están cerca (efecto de red)
                if (distance < 400f) {
                    val opacity = (1f - (distance / 400f)).coerceIn(0f, 1f)
                    drawLine(
                        color = lineColor.copy(alpha = opacity * 0.15f),
                        start = Offset(nodeA.x * width, nodeA.y * height),
                        end = Offset(nodeB.x * width, nodeB.y * height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // 3. Dibujar Nodos (Puntos)
        nodes.forEach { node ->
            drawCircle(
                color = node.color,
                radius = 5.dp.toPx(),
                center = Offset(node.x * width, node.y * height)
            )
        }
    }
}