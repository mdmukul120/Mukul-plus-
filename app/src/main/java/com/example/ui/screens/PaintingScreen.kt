package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class DrawnPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaintingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<DrawnPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(BrandRed) }
    var currentStrokeWidth by remember { mutableFloatStateOf(10f) }

    val paletteColors = listOf(
        BrandRed,
        BrandRedLight,
        GoldRating,
        CyanAccent,
        GreenSuccess,
        Color.White,
        Color(0xFFFF7043),
        Color(0xFFBA68C8),
        Color.Black
    )

    Scaffold(
        containerColor = CinemaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = CyanAccent)
                        Text("পেইন্টিং ও স্কেচ অপশন (Painting)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        paths.clear()
                        currentPath = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Canvas", tint = Color(0xFFFF5252))
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "স্কেচ সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = GreenSuccess)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Painting Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E2433))
                    .border(1.dp, CinemaBorder, RoundedCornerShape(14.dp))
                    .pointerInput(currentColor, currentStrokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val p = Path().apply { moveTo(offset.x, offset.y) }
                                currentPath = p
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // Force recomposition
                                currentPath = Path().apply {
                                    addPath(currentPath ?: Path())
                                }
                            },
                            onDragEnd = {
                                currentPath?.let { p ->
                                    paths.add(DrawnPath(p, currentColor, currentStrokeWidth))
                                }
                                currentPath = null
                            },
                            onDragCancel = {
                                currentPath = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { drawn ->
                        drawPath(
                            path = drawn.path,
                            color = drawn.color,
                            style = Stroke(
                                width = drawn.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    currentPath?.let { p ->
                        drawPath(
                            path = p,
                            color = currentColor,
                            style = Stroke(
                                width = currentStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Controls Toolbar at bottom
            Surface(
                color = CinemaSurface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Stroke Width selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ব্রাশ সাইজ (Brush Size): ${currentStrokeWidth.toInt()}px",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(6f, 12f, 20f, 32f).forEach { size ->
                                Surface(
                                    onClick = { currentStrokeWidth = size },
                                    shape = CircleShape,
                                    color = if (currentStrokeWidth == size) BrandRed else CinemaSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            color = Color.White,
                                            shape = CircleShape,
                                            modifier = Modifier.size((size * 0.4f).coerceAtLeast(4f).dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Color Palette selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(paletteColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentColor == color) 3.dp else 1.dp,
                                        color = if (currentColor == color) Color.White else CinemaBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { currentColor = color }
                            )
                        }
                    }
                }
            }
        }
    }
}
