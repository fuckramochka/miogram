package app.miogram.core.vault

/**
 * Opaque encrypted-blob persistence. Core never sees where or how the blob
 * lives; bridge supplies file-backed implementations, tests supply in-memory.
 */
interface BlobRepository {
    fun read(): ByteArray?
    fun write(blob: ByteArray)
    fun delete()
}
