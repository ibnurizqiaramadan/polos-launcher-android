package id.ibnurizqia.launcher

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Home screen backgrounds the launcher draws itself. All mostly-black, so AMOLED pixels stay off. */
enum class Wallpaper {
    Black,
    Lines, Diamonds, Grid, Dots, Stars, // straight lines and points
    Waves, Chevron, Hexagons, Arcs, Rings, Spiral, Rays, // repeating shapes
    Contours, Ridges, Flow, Mesh, // abstract
    Glow, Ember, Aurora, // faint colour, no lines
}

// dim enough that grey text crossing a line keeps 4.5:1 (TextMuted on #262626)
private val LineInk = Color(0xFF262626)
private val DotInk = Color(0xFF333333) // dots are tiny, so they can be a touch brighter
private val StarInks = listOf(Color(0xFF262626), Color(0xFF3A3A3A), Color(0xFF5A5A5A)) // faint, dim, the odd bright one

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
        Wallpaper.Stars -> {
            // one star per cell at most, scattered; most faint, a few brighter
            val gap = 34 * u
            val tiers = List(3) { mutableListOf<Offset>() }
            for (j in 0..(h / gap).toInt()) for (i in 0..(w / gap).toInt()) {
                if (hash(i, j) > 0.5f) continue
                val star = Offset((i + hash(i + 31, j)) * gap, (j + hash(j + 17, i)) * gap)
                val shine = hash(i + 7, j + 3)
                tiers[if (shine < 0.7f) 0 else if (shine < 0.95f) 1 else 2] += star
            }
            onDrawBehind {
                tiers.forEachIndexed { t, stars ->
                    drawPoints(stars, PointMode.Points, StarInks[t], strokeWidth = max((1.5f + t * 0.5f) * u, 1f), cap = StrokeCap.Round)
                }
            }
        }
        // two faint pools of deep colour fading into black, top right and bottom left
        Wallpaper.Glow -> glow(Color(0xFF0E2236), Color(0xFF1E1030)) // blue, plum
        Wallpaper.Ember -> glow(Color(0xFF2A1206), Color(0xFF2A0710)) // amber, crimson
        Wallpaper.Aurora -> glow(Color(0xFF07261F), Color(0xFF0B1630)) // teal, night blue
        Wallpaper.Ridges -> {
            // each ridge fills black below its line first, hiding the ridges behind its peaks
            val ridges = ridges(w, h, u)
            val stroke = Stroke(width = max(u, 1f))
            onDrawBehind {
                ridges.forEach { (line, fill) ->
                    drawPath(fill, Color.Black)
                    drawPath(line, LineInk, style = stroke)
                }
            }
        }
        else -> {
            val path = Path()
            when (wallpaper) {
                Wallpaper.Lines -> path.lines(w, h, u)
                Wallpaper.Diamonds -> path.diamonds(w, h, u)
                Wallpaper.Grid -> path.grid(w, h, u)
                Wallpaper.Waves -> path.waves(w, h, u)
                Wallpaper.Chevron -> path.chevron(w, h, u)
                Wallpaper.Hexagons -> path.hexagons(w, h, u)
                Wallpaper.Arcs -> path.arcs(w, h, u)
                Wallpaper.Rings -> path.rings(w, h, u)
                Wallpaper.Spiral -> path.spiral(w, h, u)
                Wallpaper.Rays -> path.rays(w, h, u)
                Wallpaper.Contours -> path.contours(w, h, u)
                Wallpaper.Flow -> path.flow(w, h, u)
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

private fun CacheDrawScope.glow(topRight: Color, bottomLeft: Color): DrawResult {
    val (w, h) = size
    val a = Brush.radialGradient(listOf(topRight, Color.Transparent), Offset(w * 0.9f, h * 0.1f), w)
    val b = Brush.radialGradient(listOf(bottomLeft, Color.Transparent), Offset(w * 0.1f, h * 0.9f), w)
    return onDrawBehind { drawRect(a); drawRect(b) }
}

// hairlines both ways at 45°, making a diamond lattice
private fun Path.diamonds(w: Float, h: Float, u: Float) {
    val gap = 28 * u
    var x = 0f
    while (x < w + h) {
        moveTo(x, 0f)
        lineTo(x - h, h)
        moveTo(x - h, 0f)
        lineTo(x, h)
        x += gap
    }
}

// graph paper, centred so the margins match
private fun Path.grid(w: Float, h: Float, u: Float) {
    val gap = 32 * u
    var x = (w % gap) / 2
    while (x < w) { moveTo(x, 0f); lineTo(x, h); x += gap }
    var y = (h % gap) / 2
    while (y < h) { moveTo(0f, y); lineTo(w, y); y += gap }
}

// rows of zigzags
private fun Path.chevron(w: Float, h: Float, u: Float) {
    val gap = 24 * u
    val half = 16 * u
    val amp = 10 * u
    var y = -amp
    while (y < h + amp) {
        moveTo(0f, y)
        var x = 0f
        var down = true
        while (x < w) {
            x += half
            lineTo(x, if (down) y + amp else y)
            down = !down
        }
        y += gap
    }
}

// honeycomb of flat-topped hexagons (shared edges are simply drawn twice)
private fun Path.hexagons(w: Float, h: Float, u: Float) {
    val r = 22 * u
    val rowGap = sqrt(3f) * r
    var col = 0
    var cx = 0f
    while (cx < w + r) {
        var cy = if (col % 2 == 0) 0f else rowGap / 2
        while (cy < h + rowGap) {
            moveTo(cx + r, cy)
            for (k in 1..6) {
                val a = k * PI / 3
                lineTo(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
            }
            cy += rowGap
        }
        cx += 1.5f * r
        col++
    }
}

// Truchet tiles: each square joins its edge midpoints with two quarter arcs, turned at random, so the arcs
// run on into each other as winding paths
private fun Path.arcs(w: Float, h: Float, u: Float) {
    val s = 36 * u
    val r = s / 2
    for (j in 0..(h / s).toInt()) for (i in 0..(w / s).toInt()) {
        val x = i * s
        val y = j * s
        if (hash(i, j) > 0.5f) {
            arcTo(Rect(Offset(x, y), r), 0f, 90f, forceMoveTo = true)
            arcTo(Rect(Offset(x + s, y + s), r), 180f, 90f, forceMoveTo = true)
        } else {
            arcTo(Rect(Offset(x + s, y), r), 90f, 90f, forceMoveTo = true)
            arcTo(Rect(Offset(x, y + s), r), 270f, 90f, forceMoveTo = true)
        }
    }
}

// one Archimedean spiral unwinding from the middle until it leaves the screen
private fun Path.spiral(w: Float, h: Float, u: Float) {
    val center = Offset(w * 0.5f, h * 0.55f)
    val b = (20 * u / (2 * PI)).toFloat() // 20dp between turns
    val reach = hypot(max(center.x, w - center.x), max(center.y, h - center.y))
    var t = 0f
    moveTo(center.x, center.y)
    while (b * t < reach) {
        t += 4 * u / max(b * t, 4 * u) // ~4dp steps along the curve
        lineTo(center.x + b * t * cos(t), center.y + b * t * sin(t))
    }
}

// sunbeams fanning in from beyond the top left corner
private fun Path.rays(w: Float, h: Float, u: Float) {
    val origin = Offset(-w * 0.25f, -h * 0.08f)
    val length = hypot(w, h) * 1.6f
    val count = (length / (40 * u)).roundToInt().coerceAtLeast(8)
    for (k in 0..count) {
        val a = k * (PI / 2).toFloat() / count
        moveTo(origin.x, origin.y)
        lineTo(origin.x + length * cos(a), origin.y + length * sin(a))
    }
}

// Ridgelines like the "Unknown Pleasures" cover: flat at the edges, noisy peaks in the middle.
// Each comes with a fill reaching down to the bottom, so a nearer ridge hides what's behind its peaks.
private fun ridges(w: Float, h: Float, u: Float): List<Pair<Path, Path>> {
    val gap = 12 * u
    val amp = 46 * u
    val step = 4 * u
    val ridges = mutableListOf<Pair<Path, Path>>()
    var y = h * 0.2f
    var row = 0
    while (y < h * 0.9f) {
        val line = Path()
        val fill = Path()
        var x = 0f
        while (x <= w + step) {
            val spread = (x - w * 0.5f) / (w * 0.2f)
            val envelope = 0.08f + 0.92f * exp(-spread * spread)
            val bumps = 0.6f * noise(x / (18 * u), row * 7) + 0.4f * noise(x / (7 * u), row * 13 + 3)
            val py = y - amp * envelope * bumps * bumps
            if (x == 0f) { line.moveTo(x, py); fill.moveTo(x, py) } else { line.lineTo(x, py); fill.lineTo(x, py) }
            x += step
        }
        fill.lineTo(w + step, h)
        fill.lineTo(0f, h)
        fill.close()
        ridges += line to fill
        y += gap
        row++
    }
    return ridges
}

// Streamlines: short strokes that each follow a smooth, swirling direction field, like iron filings.
private fun Path.flow(w: Float, h: Float, u: Float) {
    val gap = 26 * u
    val step = 5 * u
    for (j in 0..(h / gap).toInt()) for (i in 0..(w / gap).toInt()) {
        var x = (i + hash(i, j + 50)) * gap
        var y = (j + hash(j, i + 50)) * gap
        moveTo(x, y)
        repeat(28) {
            val px = x / u // the field is in dp, so it keeps its scale on any screen
            val py = y / u
            val a = (sin(px * 0.008f + 1.3f * sin(py * 0.006f)) + sin(py * 0.007f + 1.1f * cos(px * 0.005f))) * (PI / 2).toFloat()
            x += cos(a) * step
            y += sin(a) * step
            lineTo(x, y)
        }
    }
}

// smooth 0..1 noise along a line: eased steps between random values at whole numbers
private fun noise(x: Float, seed: Int): Float {
    val i = floor(x).toInt()
    val f = x - i
    val s = f * f * (3 - 2 * f)
    return hash(i, seed) + (hash(i + 1, seed) - hash(i, seed)) * s
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
