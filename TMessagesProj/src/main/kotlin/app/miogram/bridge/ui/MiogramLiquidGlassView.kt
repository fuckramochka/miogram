package app.miogram.bridge.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import app.miogram.bridge.MiogramFlags

/**
 * Liquid-glass decoration layer (Этап 4, first self-contained piece).
 *
 * On API 33+ renders an AGSL RuntimeShader with refraction-style distortion,
 * edge chromatic aberration and specular highlights; below that — a plain
 * translucent rounded rect with identical geometry, so hosts can stack it
 * unconditionally and get graceful degradation.
 *
 * Off by default via [MiogramFlags.SPATIAL_DECORATION]; when the flag is off
 * the view draws nothing at all (zero cost in onDraw).
 *
 * Deliberately standalone: no dependency on chat views. Integration as a
 * bubble decorator happens through the standard ViewOverlay mechanism.
 */
class MiogramLiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(0x66, 0xFF, 0xFF, 0xFF)
    }
    private val boundsRect = RectF()

    /** 0..1 visual strength of the effect. */
    var intensity: Float = 0.6f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var shader: Any? = null // RuntimeShader, kept untyped for pre-33 compile

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (Build.VERSION.SDK_INT >= 33 && w > 0 && h > 0) {
            shader = createRuntimeShader(w.toFloat(), h.toFloat())
        } else {
            shader = null
        }
    }

    private fun createRuntimeShader(w: Float, h: Float): Any? = try {
        val cls = Class.forName("android.graphics.RuntimeShader")
        val ctor = cls.getConstructor(String::class.java)
        val instance = ctor.newInstance(SHADER_SOURCE)
        cls.getMethod("setFloatUniform", String::class.java, Float::class.java)
            .invoke(instance, "iIntensity", intensity)
        cls.getMethod("setFloatUniform", String::class.java, Float::class.java, Float::class.java)
            .invoke(instance, "iResolution", w, h)
        instance
    } catch (e: Throwable) {
        null // AGSL unavailable despite API level; degrade silently
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!MiogramFlags.SPATIAL_DECORATION) return

        boundsRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val active = shader ?: run {
            canvas.drawRoundRect(boundsRect, CORNER_RADIUS_PX, CORNER_RADIUS_PX, fallbackPaint)
            return
        }

        if (Build.VERSION.SDK_INT >= 33) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            @Suppress("UNCHECKED_CAST")
            paint.shader = shader as android.graphics.Shader?
            canvas.drawRoundRect(boundsRect, CORNER_RADIUS_PX, CORNER_RADIUS_PX, paint)
        }
    }

    private companion object {
        const val CORNER_RADIUS_PX = 48f

        /**
         * AGSL source: frosted glass with pseudo-refraction along edges and
         * subtle chromatic aberration. Kept procedural (no child shader) so
         * the layer composites over any content cheaply.
         */
        const val SHADER_SOURCE = """
uniform float2 iResolution;
uniform float iIntensity;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 center = uv - 0.5;
    float dist = length(center) * 2.0;

    // Edge-only refraction: interior stays clean.
    float rim = smoothstep(0.55, 1.0, dist);

    // Chromatic aberration grows towards the border.
    float aberration = 0.004 * rim * iIntensity;

    half3 tint = half3(0.92 + rim * 0.05 - aberration * 40.0,
                       0.94 + rim * 0.03,
                       0.97 + rim * 0.06 + aberration * 40.0);

    // Specular streak: diagonal highlight like real glass.
    float spec = pow(max(0.0, dot(normalize(float2(-0.6, -0.8)), center)), 3.0);
    float highlight = spec * 0.25 * iIntensity;

    float alpha = 0.18 + rim * 0.35 * iIntensity + highlight;
    return half4(tint * alpha, alpha);
}
"""
    }
}
