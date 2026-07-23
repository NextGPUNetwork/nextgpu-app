package ai.nextgpu.agent

import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

object SingleInstance {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    fun lock(): Boolean {
        return try {
            val file = java.io.File(
                System.getProperty("java.io.tmpdir"),
                "nextgpu.lock"
            )

            channel = RandomAccessFile(file, "rw").channel
            lock = channel!!.tryLock()

            lock != null
        } catch (e: Exception) {
            false
        }
    }
}