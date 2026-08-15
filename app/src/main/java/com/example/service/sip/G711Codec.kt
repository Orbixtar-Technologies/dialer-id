package com.example.service.sip

/**
 * ITU-T G.711 Telephony Audio Codec standard implementation:
 * - PCMU (µ-law): 8000 Hz, 8-bit companded, standard in North America & Japan (RTP payload type 0)
 * - PCMA (A-law): 8000 Hz, 8-bit companded, standard in Europe & International telephony (RTP payload type 8)
 *
 * Provides bidirectional conversion between 16-bit linear PCM and 8-bit companded samples.
 */
enum class G711CodecType(
    val rtpPayloadType: Int,
    val sdpName: String,
    val displayName: String,
    val standardRegion: String,
    val bitRateKbps: Int = 64
) {
    PCMU(0, "PCMU", "G.711u (PCMU)", "North America & Japan"),
    PCMA(8, "PCMA", "G.711a (PCMA)", "Europe & International");

    companion object {
        fun fromPayloadType(type: Int): G711CodecType {
            return when (type) {
                8 -> PCMA
                0 -> PCMU
                else -> PCMU
            }
        }

        fun fromSdpName(name: String): G711CodecType {
            return when {
                name.contains("PCMA", ignoreCase = true) || name.contains("ALAW", ignoreCase = true) -> PCMA
                else -> PCMU
            }
        }
    }
}

object G711Codec {

    const val SAMPLE_RATE = 8000
    const val FRAME_SIZE_20MS = 160 // 20ms frame at 8kHz
    const val BIT_RATE_KBPS = 64

    // --- G.711 u-law (PCMU) Encoding & Decoding ---

    /**
     * Converts a 16-bit linear PCM sample to an 8-bit G.711 u-law byte.
     */
    fun linearToUlaw(pcmSample: Short): Byte {
        var sample = pcmSample.toInt()
        val sign = if (sample < 0) {
            sample = -sample
            0x80
        } else {
            0
        }
        sample += 132
        if (sample > 32767) sample = 32767

        val exponent = when {
            sample >= 16384 -> 7
            sample >= 8192 -> 6
            sample >= 4096 -> 5
            sample >= 2048 -> 4
            sample >= 1024 -> 3
            sample >= 512 -> 2
            sample >= 256 -> 1
            else -> 0
        }
        val mantissa = (sample shr (exponent + 3)) and 0x0F
        val ulawByte = sign or (exponent shl 4) or mantissa
        return (ulawByte xor 0xFF).toByte()
    }

    /**
     * Converts an 8-bit G.711 u-law byte back to a 16-bit linear PCM sample.
     */
    fun ulawToLinear(ulawByte: Byte): Short {
        val u = (ulawByte.toInt() xor 0xFF) and 0xFF
        val sign = u and 0x80
        val exponent = (u shr 4) and 0x07
        val mantissa = u and 0x0F
        var sample = (mantissa shl 3) + 132
        sample = sample shl exponent
        sample -= 132
        return (if (sign != 0) -sample else sample).toShort()
    }

    // --- G.711 A-law (PCMA) Encoding & Decoding (ITU-T Rec. G.711) ---

    /**
     * Converts a 16-bit linear PCM sample to an 8-bit G.711 A-law byte.
     * Uses standard ITU-T G.711 even bit inversion mask (0x55).
     */
    fun linearToAlaw(pcmSample: Short): Byte {
        var pcm = pcmSample.toInt()
        val sign = if (pcm < 0) {
            pcm = -pcm - 1
            0x00
        } else {
            0x80
        }
        if (pcm > 32767) pcm = 32767

        val exponent: Int
        val mantissa: Int

        if (pcm >= 256) {
            exponent = when {
                pcm >= 16384 -> 7
                pcm >= 8192 -> 6
                pcm >= 4096 -> 5
                pcm >= 2048 -> 4
                pcm >= 1024 -> 3
                pcm >= 512 -> 2
                else -> 1
            }
            mantissa = (pcm shr (exponent + 3)) and 0x0F
        } else {
            exponent = 0
            mantissa = (pcm shr 4) and 0x0F
        }

        val alawByte = sign or (exponent shl 4) or mantissa
        return (alawByte xor 0x55).toByte()
    }

    /**
     * Converts an 8-bit G.711 A-law byte back to a 16-bit linear PCM sample.
     */
    fun alawToLinear(alawByte: Byte): Short {
        val a = (alawByte.toInt() xor 0x55) and 0xFF
        val sign = a and 0x80
        val exponent = (a shr 4) and 0x07
        val mantissa = a and 0x0F
        var sample = if (exponent == 0) {
            (mantissa shl 4) + 8
        } else {
            ((mantissa shl 4) + 264) shl (exponent - 1)
        }
        if (sign == 0) sample = -sample
        return sample.coerceIn(-32768, 32767).toShort()
    }

    // --- High-Performance Batch Processing ---

    /**
     * Encodes 16-bit linear PCM buffer into G.711 payload (PCMU or PCMA).
     */
    fun encode(pcmBuffer: ShortArray, length: Int, codec: G711CodecType): ByteArray {
        val count = length.coerceAtMost(pcmBuffer.size)
        val payload = ByteArray(count)
        if (codec == G711CodecType.PCMA) {
            for (i in 0 until count) {
                payload[i] = linearToAlaw(pcmBuffer[i])
            }
        } else {
            for (i in 0 until count) {
                payload[i] = linearToUlaw(pcmBuffer[i])
            }
        }
        return payload
    }

    /**
     * Decodes G.711 payload byte array into 16-bit linear PCM buffer.
     * Automatically handles both PCMU (payload 0) and PCMA (payload 8).
     */
    fun decode(payload: ByteArray, offset: Int, length: Int, payloadType: Int, outPcm: ShortArray): Int {
        val count = length.coerceAtMost(outPcm.size)
        if (payloadType == 8) { // PCMA
            for (i in 0 until count) {
                outPcm[i] = alawToLinear(payload[offset + i])
            }
        } else { // PCMU (0 or fallback)
            for (i in 0 until count) {
                outPcm[i] = ulawToLinear(payload[offset + i])
            }
        }
        return count
    }
}
