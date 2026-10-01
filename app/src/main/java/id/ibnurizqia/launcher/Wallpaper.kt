package id.ibnurizqia.launcher

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Home screen backgrounds the launcher draws itself. All mostly-black, so AMOLED pixels stay off. */
enum class Wallpaper { Black, Lines, Waves, Dots, Rings, Contours, Mesh, Glow }

// dim enough that grey text crossing a line keeps 4.5:1 (TextMuted on #262626)
private val LineInk = Color(0xFF262626)
private val DotInk = Color(0xFF333333) // dots are tiny, so they can be a touch brighter

/**
 * Draws [wallpaper] behind the content. Pattern sizes are in dp times [scale] (thumbnails pass < 1 to show
 * more of it); placement is relative to the size, so a thumbnail reads like a small copy of the screen.
 * Geometry is built once per size; a redraw just replays it.
 * ponytail: paths re-rasterise whenever the screen redraws (no measurable cost on the emulator); if drawer
 * scrolling ever janks on a phone, cache it as a texture with graphicsLayer(compositingStrategy = Offscreen).
 */
fun Modifier.wallpaper(wallpaper: Wallpaper, scale: Float = 1f) = drawWithCache {
    val u = density * scale
    val (w, h) = size
    when (wallpaper) {
        Wallpaper.Black -> onDrawBehind {}
        Wallpaper.Dots -> {
            val gap = 20 * u
            val points = buildList {
                var y = gap / 2
                while (y < h) {
                    var x = gap / 2
                    while (x < w) { add(Offset(x, y)); x += gap }
                    y += gap
                }
            }
            onDrawBehind { drawPoints(points, PointMode.Points, DotInk, strokeWidth = max(2 * u, 1f), cap = StrokeCap.Round) }
        }
        Wallpaper.Glow -> {
            // two faint pools of deep blue and plum fading into black
            val blue = Brush.radialGradient(listOf(Color(0xFF0E2236), Color.Transparent), Offset(w * 0.9f, h * 0.1f), w)
            val plum = Brush.radialGradient(listOf(Color(0xFF1E1030), Color.Transparent), Offset(w * 0.1f, h * 0.9f), w)
            onDrawBehind { drawRect(blue); drawRect(plum) }
        }
        else -> {
            val path = Path()
            when (wallpaper) {
                Wallpaper.Lines -> path.lines(w, h, u)
                Wallpaper.Waves -> path.waves(w, h, u)
                Wallpaper.Rings -> path.rings(w, h, u)
                Wallpaper.Contours -> path.contours(w, h, u)
                else -> path.mesh(w, h, u)
            }
            val stroke = Stroke(width = max(u, 1f))
            onDrawBehind { drawPath(path, LineInk, style = stroke) }
        }
    }
}

// 45° hairlines
private fun Path.lines(w: Float, h: Float, u: Float) {
    val gap = 18 * u
    var x = 0f
    while (x < w + h) {
        moveTo(x, 0f)
        lineTo(x - h, h)
        x += gap
    }
}

// stacked sine waves, each row shifted a little so the crests drift diagonally
private fun Path.waves(w: Float, h: Float, u: Float) {
    val gap = 18 * u
    val amp = 10 * u
    val k = (2 * PI / (240 * u)).toFloat()
    val step = 6 * u
    var y = -amp
    var row = 0
    while (y < h + amp) {
        val phase = row * 0.45f
        moveTo(0f, y + amp * sin(phase))
        var x = step
        while (x < w + step) {
            lineTo(x, y + amp * sin(x * k + phase))
            x += step
        }
        y += gap
        row++
    }
}

// ripples spreading from the lower right, behind the favorites
private fun Path.rings(w: Float, h: Float, u: Float) {
    val center = Offset(w * 0.9f, h * 0.72f)
    val gap = 16 * u
    val reach = hypot(max(center.x, w - center.x), max(center.y, h - center.y))
    var r = gap
    while (r < reach) {
        addOval(Rect(center, r))
        r += gap
    }
}

// Topographic lines: marching squares over a height map of warped sines (smooth, no grid artefacts).
private fun Path.contours(w: Float, h: Float, u: Float) {
    val cell = 8 * u
    val nx = ceil(w / cell).toInt() + 1
    val ny = ceil(h / cell).toInt() + 1
    val height = FloatArray(nx * ny) { i ->
        val x = (i % nx) * cell / u // in dp, so the terrain keeps its scale on any screen
        val y = (i / nx) * cell / u
        sin(x * 0.012f + 1.6f * sin(y * 0.008f)) + sin(y * 0.010f + 1.4f * sin(x * 0.007f + 2f)) + 0.5f * sin((x + y) * 0.02f)
    }
    val cross = FloatArray(8)
    val corners = FloatArray(4)
    for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
        val x0 = i * cell
        val y0 = j * cell
        corners[0] = height[j * nx + i]
        corners[1] = height[j * nx + i + 1]
        corners[2] = height[(j + 1) * nx + i + 1]
        corners[3] = height[(j + 1) * nx + i]
        for (step in 0..18) { // levels -2.25..2.25, every 0.25
            val level = -2.25f + step * 0.25f
            var n = 0
            // walk the cell's edges (top, right, bottom, left) and note where the level crosses each
            for (e in 0..3) {
                val a = corners[e]
                val b = corners[(e + 1) % 4]
                if ((a > level) == (b > level)) continue
                val t = (level - a) / (b - a)
                val (ax, ay) = CORNERS[e]
                val (bx, by) = CORNERS[(e + 1) % 4]
                cross[n++] = x0 + (ax + (bx - ax) * t) * cell
                cross[n++] = y0 + (ay + (by - ay) * t) * cell
            }
            // 2 crossings = one segment; 4 (a saddle) = two, paired in order
            var k = 0
            while (k + 3 < n) {
                moveTo(cross[k], cross[k + 1])
                lineTo(cross[k + 2], cross[k + 3])
                k += 4
            }
        }
    }
}

private val CORNERS = arrayOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)

// low-poly triangles over a jittered grid
private fun Path.mesh(w: Float, h: Float, u: Float) {
    val gap = 72 * u
    val jitter = 24 * u
    fun point(i: Int, j: Int) = Offset(
        (i - 0.5f) * gap + (hash(i, j) - 0.5f) * 2 * jitter,
        (j - 0.5f) * gap + (hash(j + 101, i) - 0.5f) * 2 * jitter,
    )
    fun line(a: Offset, b: Offset) { moveTo(a.x, a.y); lineTo(b.x, b.y) }
    for (j in 0..ceil(h / gap).toInt() + 1) for (i in 0..ceil(w / gap).toInt() + 1) {
        val p = point(i, j)
        line(p, point(i + 1, j))
        line(p, point(i, j + 1))
        // each quad splits along a random diagonal
        if (hash(i + 7, j + 3) > 0.5f) line(p, point(i + 1, j + 1)) else line(point(i + 1, j), point(i, j + 1))
    }
}

// deterministic 0..1 noise per lattice point, so the mesh looks the same every time
private fun hash(x: Int, y: Int): Float {
    var n = x * 374_761_393 + y * 668_265_263
    n = (n xor (n ushr 13)) * 1_274_126_177
    return ((n xor (n ushr 16)) and 0xFFFFFF) / 16_777_216f
}
