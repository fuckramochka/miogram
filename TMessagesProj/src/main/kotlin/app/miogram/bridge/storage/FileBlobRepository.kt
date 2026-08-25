package app.miogram.bridge.storage

import android.content.Context
import app.miogram.core.vault.BlobRepository
import java.io.File

/**
 * Private-files backed BlobRepository with atomic replace (write-to-temp +
 * rename) so a crash mid-write can never leave a half-written vault blob.
 */
class FileBlobRepository(context: Context, private val fileName: String) : BlobRepository {

    private val baseFile: File = File(context.applicationInfo.dataDir, fileName)

    override fun read(): ByteArray? {
        if (!baseFile.exists()) return null
        return baseFile.readBytes()
    }

    override fun write(blob: ByteArray) {
        val parent = baseFile.parentFile
        check(parent != null && (parent.isDirectory || parent.mkdirs())) {
            "cannot create vault directory: ${parent?.absolutePath}"
        }
        val temp = File(parent, "$fileName.tmp")
        temp.writeBytes(blob)
        if (!temp.renameTo(baseFile)) {
            baseFile.delete()
            if (!temp.renameTo(baseFile)) {
                temp.delete()
                error("cannot persist vault blob to ${baseFile.absolutePath}")
            }
        }
    }

    override fun delete() {
        baseFile.delete()
    }
}
