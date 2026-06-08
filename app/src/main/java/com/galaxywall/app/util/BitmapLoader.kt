package com.galaxywall.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Loads bitmaps through Coil from remote image URLs ("https://...") and content URIs. Returns
 * software bitmaps (hardware disabled) so they can be drawn on a Canvas and used with
 * WallpaperManager.
 */
object BitmapLoader {

    suspend fun load(context: Context, uri: String, size: Size = Size.ORIGINAL): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .size(size)
            .build()
        val drawable = context.imageLoader.execute(request).drawable ?: return null
        return (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
    }

    /**
     * Orders a 2-layer parallax stack for rendering: background first (moving), foreground last
     * (still). The renderer draws bottom -> top and keeps the top layer still over the moving
     * background, so depth only reads when the FOREGROUND is a transparent cut-out — an opaque top
     * would simply hide the background.
     *
     * The catalog delivers that foreground inconsistently, so we inspect ACTUAL pixels (never
     * [Bitmap.hasAlpha]: some opaque images are stored in an RGBA format with an all-opaque alpha
     * channel, so hasAlpha() lies):
     *  - a true transparent cut-out (e.g. Sport3's sport32) -> used as the foreground as-is;
     *  - a subject on a SOLID BLACK rectangle with no transparency (e.g. Anime9's Anime92 = Naruto
     *    on black) -> cut into a transparent foreground so the other layer (the scene) shows behind
     *    it, giving a real 2-layer parallax. We prefer a DIFFERENCE MATTE against [composite] (the
     *    pre-composed full image, e.g. Anime93) because it preserves the subject's own black details
     *    (they match the composite, only the background differs); we fall back to a flood-fill key
     *    when no aligned composite is available.
     *
     * [composite] is the pre-composed full image (the wallpaper's thumbUri / layer 3), loaded at the
     * SAME size as [bitmaps] so it aligns pixel-for-pixel with the subject layer. Pass null if not
     * available.
     *
     * Returns:
     *  - the input unchanged when it isn't a 2-layer stack (e.g. a single pre-composed image);
     *  - the two layers ordered background -> foreground when we can identify a foreground;
     *  - null when neither layer is a cut-out nor a subject-on-black (can't tell the layers apart) —
     *    the caller should fall back to the pre-composed full image.
     */
    fun orderParallaxLayers(bitmaps: List<Bitmap>, composite: Bitmap? = null): List<Bitmap>? {
        if (bitmaps.size != 2) return bitmaps
        val (a, b) = bitmaps
        val aAlpha = hasRealTransparency(a)
        val bAlpha = hasRealTransparency(b)
        return when {
            aAlpha && !bAlpha -> listOf(b, a) // a is the cut-out -> foreground (still, on top)
            bAlpha && !aAlpha -> listOf(a, b) // b is the cut-out -> foreground (still, on top)
            aAlpha && bAlpha -> bitmaps       // both cut-outs -> keep the given order
            else -> {
                // Neither layer is a real cut-out. If exactly one is a subject on a solid black
                // background, cut the black away so it becomes the transparent foreground over the
                // other (scene) layer -> a true 2-layer parallax (subject in front, scene behind).
                val aBlack = isSubjectOnBlack(a)
                val bBlack = isSubjectOnBlack(b)
                when {
                    bBlack && !aBlack -> listOf(a, cutSubject(b, composite))
                    aBlack && !bBlack -> listOf(b, cutSubject(a, composite))
                    else -> null // can't tell layers apart -> caller uses the pre-composed image
                }
            }
        }
    }

    /**
     * Turns a subject-on-solid-black image into a transparent foreground. Prefers a difference matte
     * against the aligned pre-composed image (best quality — keeps the subject's dark details); only
     * falls back to the flood-fill key when no matching composite is available.
     */
    private fun cutSubject(subjectOnBlack: Bitmap, composite: Bitmap?): Bitmap {
        if (composite != null &&
            composite.width == subjectOnBlack.width &&
            composite.height == subjectOnBlack.height
        ) {
            return differenceMatte(subjectOnBlack, composite)
        }
        return cutSubjectFromBlack(subjectOnBlack)
    }

    /**
     * Builds a transparent foreground from a subject-on-black image [subject] and the aligned
     * pre-composed full image [composite] (same subject over the real scene). Where the two AGREE
     * the pixel is the subject (the subject's own colours, including its black details, are identical
     * in both) -> opaque; where they DISAGREE the pixel is background (black vs. scene) -> transparent.
     * Alpha ramps with the per-pixel colour difference so edges stay soft. The subject's colours come
     * from [subject] (over black), which is exactly the un-premultiplied subject.
     */
    private fun differenceMatte(subject: Bitmap, composite: Bitmap): Bitmap {
        val w = subject.width
        val h = subject.height
        val n = w * h
        val s = IntArray(n)
        val c = IntArray(n)
        subject.getPixels(s, 0, w, 0, 0, w, h)
        composite.getPixels(c, 0, w, 0, 0, w, h)
        val lo = 60   // total RGB difference at/below this -> fully subject (opaque)
        val hi = 170  // at/above this -> fully background (transparent)
        val range = hi - lo
        val out = IntArray(n)
        for (i in 0 until n) {
            val sc = s[i]
            val cc = c[i]
            val diff = abs(((sc ushr 16) and 0xFF) - ((cc ushr 16) and 0xFF)) +
                abs(((sc ushr 8) and 0xFF) - ((cc ushr 8) and 0xFF)) +
                abs((sc and 0xFF) - (cc and 0xFF))
            val a = when {
                diff <= lo -> 255
                diff >= hi -> 0
                else -> (hi - diff) * 255 / range
            }
            out[i] = (a shl 24) or (sc and 0x00FFFFFF)
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    /** True if a meaningful fraction of sampled pixels are (semi-)transparent — a real cut-out. */
    private fun hasRealTransparency(bmp: Bitmap, threshold: Int = 24): Boolean {
        if (!bmp.hasAlpha()) return false // no alpha channel at all -> definitely opaque
        val w = bmp.width
        val h = bmp.height
        if (w == 0 || h == 0) return false
        val stepX = max(1, w / 48)
        val stepY = max(1, h / 48)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val alpha = (bmp.getPixel(x, y) ushr 24) and 0xFF
                if (alpha < threshold) return true
                x += stepX
            }
            y += stepY
        }
        return false
    }

    /** True if the image's border is predominantly near-black — a subject on a black background. */
    private fun isSubjectOnBlack(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w < 4 || h < 4) return false
        var sum = 0L
        var count = 0
        var bright = 0
        val stepX = max(1, w / 40)
        var x = 0
        while (x < w) {
            val top = luma(bmp.getPixel(x, 0))
            val bot = luma(bmp.getPixel(x, h - 1))
            sum += top + bot; count += 2
            if (top > 80) bright++
            if (bot > 80) bright++
            x += stepX
        }
        val stepY = max(1, h / 40)
        var y = 0
        while (y < h) {
            val left = luma(bmp.getPixel(0, y))
            val right = luma(bmp.getPixel(w - 1, y))
            sum += left + right; count += 2
            if (left > 80) bright++
            if (right > 80) bright++
            y += stepY
        }
        if (count == 0) return false
        // Mostly-dark border with only a little bright bleed (glow/effects) -> subject on black.
        return sum / count < 45 && bright.toFloat() / count < 0.20f
    }

    /**
     * Cuts a subject off its solid-black background into a transparent foreground.
     *
     * Unlike a plain luminance key (which also punches holes through the subject's own dark areas —
     * black outlines, shadows, dark clothing), this FLOOD-FILLS the black that is CONNECTED to the
     * image border. Only that connected background becomes transparent; dark pixels enclosed by the
     * subject stay opaque, so there are no holes. The 1px edge ring bordering the background is
     * dropped (it's the dark anti-aliased fringe) and the alpha is box-blurred for a soft edge.
     */
    private fun cutSubjectFromBlack(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val n = w * h
        if (n == 0) return src
        val px = IntArray(n)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // 1) Flood-fill the background black (pixels connected to the border that are both DARK and
        //    COLORLESS). The colorless test (low chroma) is what keeps the subject's own dark-but-
        //    coloured details — e.g. Naruto's navy-blue suit trim — from being eaten: pure black has
        //    chroma ~0, navy has high chroma, so the fill stops at the suit instead of leaking in.
        val bgThreshold = 60
        val chromaMax = 28
        val bg = BooleanArray(n)
        val queue = IntArray(n)
        var tail = 0
        fun consider(idx: Int) {
            if (bg[idx]) return
            val c = px[idx]
            if (luma(c) > bgThreshold) return
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val chroma = max(r, max(g, b)) - min(r, min(g, b))
            if (chroma > chromaMax) return // dark but coloured (navy suit) -> part of the subject
            bg[idx] = true
            queue[tail++] = idx
        }
        for (i in 0 until w) { consider(i); consider((h - 1) * w + i) }
        for (j in 0 until h) { consider(j * w); consider(j * w + w - 1) }
        var headIdx = 0
        while (headIdx < tail) {
            val idx = queue[headIdx++]
            val x = idx % w
            val y = idx / w
            if (x > 0) consider(idx - 1)
            if (x < w - 1) consider(idx + 1)
            if (y > 0) consider(idx - w)
            if (y < h - 1) consider(idx + w)
        }

        // 2) Alpha mask: background -> 0, subject -> 255. Erode the subject by 1px (drop the dark
        //    anti-aliased fringe that touches the background, so no grey outline shows).
        val mask = IntArray(n)
        for (i in 0 until n) mask[i] = if (bg[i]) 0 else 255
        val eroded = mask.copyOf()
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (mask[i] == 0) continue
                val edge = (x > 0 && bg[i - 1]) || (x < w - 1 && bg[i + 1]) ||
                    (y > 0 && bg[i - w]) || (y < h - 1 && bg[i + w])
                if (edge) eroded[i] = 0
            }
        }

        // 3) 3x3 box blur on the alpha only -> soft edge. Keep the original RGB.
        val out = IntArray(n)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var cnt = 0
                var dy = -1
                while (dy <= 1) {
                    val ny = y + dy
                    if (ny in 0 until h) {
                        var dx = -1
                        while (dx <= 1) {
                            val nx = x + dx
                            if (nx in 0 until w) { sum += eroded[ny * w + nx]; cnt++ }
                            dx++
                        }
                    }
                    dy++
                }
                val a = if (cnt == 0) 0 else sum / cnt
                val i = y * w + x
                out[i] = (a shl 24) or (px[i] and 0x00FFFFFF)
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    /** Rec.601 luminance (0..255) of an ARGB color, ignoring alpha. */
    private fun luma(c: Int): Int {
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        return (r * 77 + g * 151 + b * 28) ushr 8
    }
}
