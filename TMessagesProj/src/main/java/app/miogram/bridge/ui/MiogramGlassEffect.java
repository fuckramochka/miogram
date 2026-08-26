package app.miogram.bridge.ui;

import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;

import app.miogram.bridge.MiogramFlags;

/**
 * High-end AGSL Liquid Glass & Frosted Glassmorphism Shader Engine for Miogram.
 * Provides real refraction, chromatic dispersion highlights and deep frosted backdrop blur.
 */
public class MiogramGlassEffect {

    private static final String LIQUID_GLASS_AGSL = """
        uniform shader content;
        uniform float2 size;
        uniform float intensity;
        uniform float time;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / size;
            
            // Refraction distortion wave
            float dist = distance(uv, float2(0.5, 0.5));
            float disp = sin(dist * 14.0 - time * 0.8) * 0.02 * intensity;
            
            float2 uvR = uv + float2(disp * 1.15, disp * 0.85);
            float2 uvG = uv + float2(disp, disp);
            float2 uvB = uv + float2(disp * 0.85, disp * 1.15);
            
            half4 cR = content.eval(uvR * size);
            half4 cG = content.eval(uvG * size);
            half4 cB = content.eval(uvB * size);
            
            half4 color = half4(cR.r, cG.g, cB.b, (cR.a + cG.a + cB.a) / 3.0);
            
            // Specular border glow (glass edge rim)
            float edgeDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
            float edgeGlow = smoothstep(0.05, 0.0, edgeDist) * 0.35 * intensity;
            
            // Glass sheen reflection
            float sheen = smoothstep(0.75, 0.25, uv.y + uv.x * 0.35) * 0.15 * intensity;
            
            color.rgb += half3(edgeGlow + sheen);
            return color;
        }
    """;

    private static RuntimeShader glassShader;

    public static boolean isEnabled() {
        return MiogramFlags.isSpatialDecoration() ||
                MiogramVisualsPrefs.loadBool(ApplicationLoader.applicationContext, "agsl_enabled", false);
    }

    public static float getIntensityFactor() {
        int pct = MiogramVisualsPrefs.loadInt(ApplicationLoader.applicationContext, "liquid_glass_intensity", 60);
        return Math.max(0.1f, pct / 100.0f);
    }

    public static void applyGlass(View view) {
        applyGlass(view, getIntensityFactor());
    }

    public static void applyGlass(View view, float intensity) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (!isEnabled()) {
            view.setRenderEffect(null);
            return;
        }
        try {
            if (glassShader == null) {
                glassShader = new RuntimeShader(LIQUID_GLASS_AGSL);
            }
            float w = view.getWidth() > 0 ? view.getWidth() : 1080f;
            float h = view.getHeight() > 0 ? view.getHeight() : 1920f;
            glassShader.setFloatUniform("size", w, h);
            glassShader.setFloatUniform("intensity", intensity);
            glassShader.setFloatUniform("time", (float) (System.currentTimeMillis() % 100000) / 1000f);

            RenderEffect shaderEffect = RenderEffect.createRuntimeShaderEffect(glassShader, "content");
            RenderEffect blurEffect = RenderEffect.createBlurEffect(22f * intensity + 8f, 22f * intensity + 8f, Shader.TileMode.CLAMP);
            RenderEffect chain = RenderEffect.createChainEffect(shaderEffect, blurEffect);
            view.setRenderEffect(chain);
        } catch (Throwable e) {
            try {
                view.setRenderEffect(RenderEffect.createBlurEffect(25f * intensity + 5f, 25f * intensity + 5f, Shader.TileMode.CLAMP));
            } catch (Throwable ignored) {}
        }
    }
}
