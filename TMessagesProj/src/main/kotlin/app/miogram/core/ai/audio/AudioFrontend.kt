package app.miogram.core.ai.audio

/**
 * Deterministic audio front-end for on-device transcription.
 *
 * Contract with the model layer: Whisper-family exports that embed the mel
 * frontend consume 16 kHz mono float32 PCM in [-1, 1]; [prepareModelInput]
 * produces exactly that, chunked to the fixed 30-second window the models
 * were trained on (zero-padded tail). Keeping this pure Kotlin makes it
 * unit-testable without a device or native runtime.
 */
object AudioFrontend {

    const val TARGET_SAMPLE_RATE = 16_000
    const val CHUNK_SECONDS = 30
    const val CHUNK_SAMPLES = CHUNK_SECONDS * TARGET_SAMPLE_RATE

    /** Minimal sane audio length; shorter inputs are padded, not rejected. */
    const val MIN_INPUT_BYTES = 64

    class AudioException(message: String) : Exception(message)

    /** Decodes signed 16-bit little-endian PCM into normalized floats. */
    fun pcm16ToFloat(bytes: ByteArray): FloatArray {
        if (bytes.size < 2) throw AudioException("pcm16 payload too short")
        if (bytes.size % 2 != 0) throw AudioException("odd pcm16 byte count")
        val out = FloatArray(bytes.size / 2)
        var o = 0
        var i = 0
        while (i < bytes.size) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            out[o++] = sample / 32768f
            i += 2
        }
        return out
    }

    /**
     * Linear-interpolation resampler. Quality is sufficient for speech after
     * Telegram's own codecs; anti-aliasing beyond linear is out of scope for
     * voice-band content already band-limited to <8 kHz.
     */
    fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (input.isEmpty()) return input
        if (fromRate == toRate) return input.copyOf()
        require(fromRate > 0 && toRate > 0) { "invalid rates $fromRate -> $toRate" }

        val ratio = fromRate.toDouble() / toRate
        val outLength = Math.round(input.size / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outLength)
        for (o in out.indices) {
            val srcPos = o * ratio
            val i0 = srcPos.toInt()
            val i1 = (i0 + 1).coerceAtMost(input.size - 1)
            val frac = (srcPos - i0).toFloat()
            out[o] = input[i0] * (1f - frac) + input[i1] * frac
        }
        return out
    }

    /** Converts any input to the model rate. */
    fun ensureSampleRate(samples: FloatArray, actualRate: Int): FloatArray =
        if (actualRate == TARGET_SAMPLE_RATE) samples
        else resample(samples, actualRate, TARGET_SAMPLE_RATE)

    /**
     * Produces one fixed-size model window: exactly [CHUNK_SAMPLES] floats at
     * 16 kHz. Longer input is truncated (caller may chunk before); shorter is
     * zero-padded at the end.
     */
    fun prepareModelInput(samples: FloatArray): FloatArray {
        val window = FloatArray(CHUNK_SAMPLES)
        System.arraycopy(samples, 0, window, 0, samples.size.coerceAtMost(CHUNK_SAMPLES))
        return window
    }

    /**
     * Peak normalization to [-1, 1] when the source is quiet or clipped.
     * Purely gain staging: never changes waveform shape above the noise floor.
     */
    fun normalizePeak(samples: FloatArray): FloatArray {
        var peak = 0f
        for (s in samples) peak = maxOf(peak, Math.abs(s))
        if (peak <= 1e-6f || peak == 1f) return samples
        val gain = 1f / peak
        return FloatArray(samples.size) { samples[it] * gain }
    }
}
