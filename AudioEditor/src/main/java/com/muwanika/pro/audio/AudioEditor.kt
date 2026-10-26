package com.muwanika.pro.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AudioPCM(
    val sampleRate: Int,
    val channelCount: Int,
    val samples: FloatArray
)

data class TrimRange(
    val startSample: Int,
    val endSample: Int
)

data class CutResult(
    val before: FloatArray,
    val after: FloatArray
)

class AudioEditor(private val context: Context) {

    fun readPcmFromUri(uri: Uri): AudioPCM {
        val resolver = context.contentResolver
        val descriptor = resolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Could not open file: $uri")

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)

            val trackIndex = findAudioTrackIndex(extractor)
                ?: throw IllegalStateException("No audio track found in file")

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Missing MIME type")

            val codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val outputChunks = mutableListOf<ByteArray>()

            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                        if (sampleSize >= 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                extractor.sampleFlags
                            )
                            extractor.advance()
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEOS = true
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10000)
                when {
                    outIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outIndex)
                        if (outputBuffer != null && info.size > 0) {
                            val bytes = ByteArray(info.size)
                            outputBuffer.position(info.offset)
                            outputBuffer.get(bytes)
                            outputChunks.add(bytes)
                        }
                        codec.releaseOutputBuffer(outIndex, false)

                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true
                        }
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    }
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val raw = concatenate(outputChunks)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)

            val monoSamples = pcmToMonoFloat(raw, channelCount)
            val normalized = normalize(monoSamples)

            return AudioPCM(sampleRate, channelCount, normalized)
        } finally {
            try {
                descriptor.close()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun readPcmFromFile(path: String, sampleRate: Int = 44100, channelCount: Int = 2): AudioPCM {
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("File does not exist: $path")

        val bytes = file.readBytes()
        val mono = pcmToMonoFloat(bytes, channelCount)
        return AudioPCM(sampleRate, channelCount, normalize(mono))
    }

    fun trim(samples: FloatArray, startSample: Int, endSample: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        val start = max(0, startSample)
        val end = min(samples.size - 1, endSample)
        if (start >= end) return FloatArray(0)
        return samples.copyOfRange(start, end + 1)
    }

    fun trimSilence(samples: FloatArray, threshold: Float = 0.01f, paddingSamples: Int = 1000): TrimRange {
        if (samples.isEmpty()) return TrimRange(0, 0)

        var start = 0
        while (start < samples.size && abs(samples[start]) <= threshold) {
            start++
        }

        var end = samples.size - 1
        while (end >= 0 && abs(samples[end]) <= threshold) {
            end--
        }

        start = max(0, start - paddingSamples)
        end = min(samples.size - 1, end + paddingSamples)

        if (start >= end) {
            return TrimRange(0, 0)
        }

        return TrimRange(start, end)
    }

    fun split(samples: FloatArray, splitSample: Int): CutResult {
        val index = splitSample.coerceIn(0, samples.size)
        val left = samples.copyOfRange(0, index)
        val right = samples.copyOfRange(index, samples.size)
        return CutResult(left, right)
    }

    fun fadeIn(samples: FloatArray, fadeMs: Long, sampleRate: Int): FloatArray {
        if (fadeMs <= 0L || samples.isEmpty()) return samples.copyOf()
        val fadeSamples = (fadeMs * sampleRate / 1000L).toInt()
        val out = samples.copyOf()
        val len = min(fadeSamples, out.size)

        for (i in 0 until len) {
            val t = i.toFloat() / len.toFloat()
            out[i] = out[i] * t
        }
        return out
    }

    fun fadeOut(samples: FloatArray, fadeMs: Long, sampleRate: Int): FloatArray {
        if (fadeMs <= 0L || samples.isEmpty()) return samples.copyOf()
        val fadeSamples = (fadeMs * sampleRate / 1000L).toInt()
        val out = samples.copyOf()
        val len = min(fadeSamples, out.size)

        for (i in 0 until len) {
            val offset = len - i
            val t = offset.toFloat() / len.toFloat()
            val index = out.size - len + i
            out[index] = out[index] * t
        }
        return out
    }

    fun applyGain(samples: FloatArray, gainDb: Float): FloatArray {
        if (samples.isEmpty()) return samples
        val gain = Math.pow(10.0, (gainDb / 20.0)).toFloat()
        val output = FloatArray(samples.size)
        for (i in samples.indices) {
            output[i] = samples[i] * gain
        }
        return output
    }

    fun normalize(samples: FloatArray, peak: Float = 0.98f): FloatArray {
        if (samples.isEmpty()) return samples

        var maxAbs = 0f
        for (value in samples) {
            maxAbs = max(maxAbs, abs(value))
        }

        if (maxAbs <= 0f) return samples

        val target = peak.coerceIn(0.01f, 1.0f)
        val scale = target / maxAbs
        val output = FloatArray(samples.size)
        for (i in samples.indices) {
            output[i] = samples[i] * scale
        }
        return output
    }

    fun reverse(samples: FloatArray): FloatArray {
        val out = FloatArray(samples.size)
        for (i in samples.indices) {
            out[i] = samples[samples.size - 1 - i]
        }
        return out
    }

    fun mix(samplesA: FloatArray, samplesB: FloatArray): FloatArray {
        val length = max(samplesA.size, samplesB.size)
        val out = FloatArray(length)

        for (i in 0 until length) {
            val a = if (i < samplesA.size) samplesA[i] else 0f
            val b = if (i < samplesB.size) samplesB[i] else 0f
            out[i] = a + b
        }

        return normalize(out)
    }

    fun computeEnergyCurve(samples: FloatArray, frameSize: Int = 2048): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        require(frameSize > 0) { "frameSize must be > 0" }

        val frameCount = max(1, samples.size / frameSize)
        val curve = FloatArray(frameCount)

        for (i in 0 until frameCount) {
            val start = i * frameSize
            val end = min(start + frameSize, samples.size)
            var sum = 0.0

            for (j in start until end) {
                val value = samples[j]
                sum += value * value
            }

            val rms = sqrt(sum / max(1, end - start).toDouble()).toFloat()
            curve[i] = rms
        }

        return normalize(curve, peak = 1f)
    }

    fun smoothEnergyCurve(curve: FloatArray, windowSize: Int = 5): FloatArray {
        if (curve.isEmpty()) return curve
        require(windowSize > 0) { "windowSize must be > 0" }

        val output = FloatArray(curve.size)
        val half = windowSize / 2

        for (i in curve.indices) {
            var total = 0f
            var count = 0

            for (j in (i - half)..(i + half)) {
                if (j in curve.indices) {
                    total += curve[j]
                    count++
                }
            }

            output[i] = if (count > 0) total / count.toFloat() else curve[i]
        }

        return output
    }

    fun writeWavFile(samples: FloatArray, sampleRate: Int, outputFile: File) {
        val pcmBytes = floatArrayTo16BitPcm(samples)
        val header = createWavHeader(pcmBytes.size.toLong(), sampleRate.toLong(), 1, 16)

        FileOutputStream(outputFile).use { stream ->
            stream.write(header)
            stream.write(pcmBytes)
        }
    }

    private fun findAudioTrackIndex(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                return i
            }
        }
        return null
    }

    private fun concatenate(chunks: List<ByteArray>): ByteArray {
        val total = chunks.sumOf { it.size }
        val out = ByteArray(total)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.size)
            offset += chunk.size
        }
        return out
    }

    private fun pcmToMonoFloat(raw: ByteArray, channelCount: Int): FloatArray {
        if (raw.isEmpty()) return FloatArray(0)

        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = buffer.asShortBuffer()

        if (channelCount <= 1) {
            val out = FloatArray(shorts.remaining())
            for (i in 0 until shorts.remaining()) {
                out[i] = shorts.get(i).toFloat() / Short.MAX_VALUE.toFloat()
            }
            return out
        }

        val out = FloatArray(shorts.remaining() / channelCount)
        var outIndex = 0

        for (i in 0 until shorts.remaining() step channelCount) {
            var sum = 0f
            val frameCount = min(channelCount, shorts.remaining() - i)
            for (j in 0 until frameCount) {
                sum += shorts.get(i + j).toFloat()
            }
            out[outIndex++] = (sum / frameCount.toFloat()) / Short.MAX_VALUE.toFloat()
        }

        return out
    }

    private fun floatArrayTo16BitPcm(samples: FloatArray): ByteArray {
        val pcm = ByteArray(samples.size * 2)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)

        for (sample in samples) {
            val clamped = sample.coerceIn(-1f, 1f)
            val value = (clamped * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer.putShort(value.toShort())
        }

        return pcm
    }

    private fun createWavHeader(dataSize: Long, sampleRate: Long, channels: Int, bitsPerSample: Int): ByteArray {
        val blockAlign = channels * (bitsPerSample / 8)
        val byteRate = sampleRate * blockAlign
        val totalSize = 36 + dataSize

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalSize.toInt())
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate.toInt())
        buffer.putInt(byteRate.toInt())
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize.toInt())

        return header
    }
}
