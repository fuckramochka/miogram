package app.miogram.core.ai.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFrontendTest {

    @Test
    fun `pcm16 little endian decodes to normalized range`() {
        // 0, 16384, -32768 (LE bytes)
        val bytes = byteArrayOf(0, 0, 0x00, 0x40, 0x00, 0x80.toByte())
        val samples = AudioFrontend.pcm16ToFloat(bytes)
        assertEquals(3, samples.size)
        assertEquals(0f, samples[0], 1e-7f)
        assertEquals(0.5f, samples[1], 1e-4f)
        assertEquals(-1f, samples[2], 1e-7f)
    }

    @Test(expected = AudioFrontend.AudioException::class)
    fun `odd byte count rejected`() {
        AudioFrontend.pcm16ToFloat(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `same rate resample is identity`() {
        val input = floatArrayOf(0.1f, -0.5f, 0.9f)
        assertArrayEquals(input, AudioFrontend.resample(input, 16_000, 16_000), 0f)
    }

    @Test
    fun `upsample doubles length and preserves monotonic ramp`() {
        val input = FloatArray(101) { it / 100f } // 0..1 over 100 steps
        val up = AudioFrontend.resample(input, 8_000, 16_000)
        assertEquals(202, Math.round(input.size * 16.0 / 8.0))
        assertEquals(202, up.size)
        assertTrue(up.first() < 0.01f && up.last() > 0.99f)
        for (i in 1 until up.size) {
            assertTrue("ramp must be non-decreasing", up[i] >= up[i - 1] - 1e-6f)
        }
    }

    @Test
    fun `downsample then upsample keeps length contract`() {
        val input = FloatArray(16_000) { kotlin.math.sin(it.toDouble() * 0.05).toFloat() }
        val down = AudioFrontend.resample(input, 16_000, 8_000)
        assertEquals(8000, down.size)
        val back = AudioFrontend.resample(down, 8_000, 16_000)
        assertEquals(16_000, back.size)
    }

    @Test
    fun `model window is fixed size with zero padding`() {
        val short = floatArrayOf(0.5f, -0.5f)
        val window = AudioFrontend.prepareModelInput(short)
        assertEquals(AudioFrontend.CHUNK_SAMPLES, window.size)
        assertEquals(0.5f, window[0], 0f)
        assertEquals(-0.5f, window[1], 0f)
        assertEquals(0f, window[AudioFrontend.CHUNK_SAMPLES - 1], 0f)

        val long = FloatArray(AudioFrontend.CHUNK_SAMPLES + 100) { 0.25f }
        assertEquals(AudioFrontend.CHUNK_SAMPLES, AudioFrontend.prepareModelInput(long).size)
    }

    @Test
    fun `peak normalization scales without clipping or flipping shape`() {
        val quiet = floatArrayOf(0.01f, -0.02f, 0.005f)
        val normalized = AudioFrontend.normalizePeak(quiet)
        // Peak magnitude is the negative sample; it maps to full scale.
        assertEquals(1f, normalized.map(Math::abs).max(), 1e-6f)
        assertEquals(-1f, normalized[1], 1e-6f)
        assertEquals(0.5f, normalized[0], 1e-6f)

        val silence = FloatArray(10)
        assertArrayEquals(silence, AudioFrontend.normalizePeak(silence), 0f)
    }

    @Test
    fun `ensureSampleRate passthrough at target rate`() {
        val input = FloatArray(160)
        assertTrue(AudioFrontend.ensureSampleRate(input, 16_000) === input)
    }
}
