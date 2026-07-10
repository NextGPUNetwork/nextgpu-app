package ai.nextgpu.agent.service

import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.abs

@Service
class AudioRecorderService {
    private var targetDataLine: TargetDataLine? = null
    private var isRecording = false
    private var outputStream: ByteArrayOutputStream? = null

    // Keep track of the format the hardware actually accepted
    private var activeFormat: AudioFormat? = null

    /**
     * Starts recording audio.
     * @param onAmplitude Update callback for your Compose visualizer (yields 0.0f to 1.0f)
     */
    fun startRecording(onAmplitude: (Float) -> Unit) {
        // Try STT ideal (16kHz), fallback to standard hardware rates (44.1kHz, 48kHz)
        val sampleRates = floatArrayOf(16000f, 44100f, 48000f)
        var info: DataLine.Info? = null

        for (rate in sampleRates) {
            val format = AudioFormat(rate, 16, 1, true, false)
            val testInfo = DataLine.Info(TargetDataLine::class.java, format)

            if (AudioSystem.isLineSupported(testInfo)) {
                activeFormat = format
                info = testInfo
                break // Found a supported format, exit loop
            }
        }

        if (info == null || activeFormat == null) {
            throw Exception("Microphone not supported. Check OS permissions, hardware, or WSL/Docker limitations.")
        }

        targetDataLine = AudioSystem.getLine(info) as TargetDataLine
        targetDataLine?.open(activeFormat)
        targetDataLine?.start()

        isRecording = true
        outputStream = ByteArrayOutputStream()

        thread(name = "MicRecordingThread") {
            // 1024 bytes is a good chunk size for ~30fps UI updates
            val buffer = ByteArray(1024)

            while (isRecording) {
                val bytesRead = targetDataLine?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    outputStream?.write(buffer, 0, bytesRead)

                    // --- Amplitude Calculation ---
                    var maxAmplitude = 0
                    for (i in 0 until bytesRead step 2) {
                        val low = buffer[i].toInt() and 0xFF
                        val high = buffer[i + 1].toInt() shl 8
                        val sample = (high or low).toShort()
                        maxAmplitude = maxOf(maxAmplitude, abs(sample.toInt()))
                    }

                    // Normalize the 16-bit value (max 32767) to a 0.0 -> 1.0 float
                    val normalized = maxAmplitude / 32767f
                    onAmplitude(normalized)
                }
            }
        }
    }

    /**
     * Stops the recording and packages the captured bytes into a valid WAV file.
     */
    fun stopRecording(): File {
        isRecording = false
        targetDataLine?.stop()
        targetDataLine?.close()

        val audioData = outputStream?.toByteArray() ?: ByteArray(0)

        // Use the format that was successfully negotiated during startRecording()
        val format = activeFormat ?: AudioFormat(16000f, 16, 1, true, false)

        val inputStream = AudioInputStream(
            ByteArrayInputStream(audioData),
            format,
            (audioData.size / 2).toLong()
        )

        val tempWavFile = File.createTempFile("nextgpu_prompt_", ".wav")
        AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, tempWavFile)

        return tempWavFile
    }
}