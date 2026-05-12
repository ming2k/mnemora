package com.hihusky.mnemora.domain.service

import android.content.Context
import android.net.Uri
import com.hihusky.mnemora.data.model.ImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookImporter: BookImporter
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importPackage(uri: Uri, onProgress: (String, Float?) -> Unit): ImportResult {
        return withContext(Dispatchers.IO) {
            var destDir: File? = null
            var tempPackage: File? = null
            try {
                var lastStatus: String? = null
                var lastProgressBucket: Int? = null

                fun progress(status: String, value: Float?) {
                    val clamped = value?.coerceIn(0f, 1f)
                    val bucket = clamped?.let { (it * 100).toInt() }
                    if (status != lastStatus || bucket != lastProgressBucket || clamped == null || clamped >= 1f) {
                        lastStatus = status
                        lastProgressBucket = bucket
                        onProgress(status, clamped)
                    }
                }

                progress("Preparing package...", 0.02f)
                val contentResolver = context.contentResolver
                val fileName = getFileName(uri) ?: "unknown"

                if (!fileName.lowercase().endsWith(".zip") &&
                    !fileName.lowercase().endsWith(".quizpkg") &&
                    !fileName.lowercase().endsWith(".mnemorapkg")
                ) {
                    return@withContext ImportResult.Error("Invalid file type. Please select a .zip, .quizpkg, or .mnemorapkg file.")
                }

                val packagesDir = File(context.filesDir, "packages").apply { mkdirs() }
                val packageName = fileName.substringBeforeLast(".")
                val packageId = "${packageName}_${System.currentTimeMillis()}"
                destDir = File(packagesDir, packageId).apply { mkdirs() }
                tempPackage = File.createTempFile("import-", ".mnemorapkg", context.cacheDir)

                progress("Reading package...", 0.05f)
                val sourceSize = getFileSize(uri)
                contentResolver.openInputStream(uri)?.use { stream ->
                    copyToFile(stream, tempPackage) { copiedBytes ->
                        val readProgress = if (sourceSize > 0L) {
                            copiedBytes.toFloat() / sourceSize.toFloat()
                        } else null
                        val mapped = readProgress?.let { 0.05f + it.coerceIn(0f, 1f) * 0.15f }
                        progress("Reading package...", mapped ?: 0.12f)
                    }
                } ?: return@withContext ImportResult.Error("Failed to read package file.")

                progress("Extracting resources...", 0.2f)
                extractZip(tempPackage, destDir) { extractProgress ->
                    progress(
                        "Extracting resources... ${(extractProgress * 100).toInt()}%",
                        0.2f + extractProgress * 0.25f
                    )
                }

                progress("Validating package...", 0.48f)
                val dataFile = findDataJson(destDir)
                    ?: return@withContext ImportResult.Error("Invalid package structure: No data.json found.")

                progress("Parsing questions...", 0.52f)
                val content = dataFile.readText()
                val dataMap = try {
                    json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(content)
                } catch (e: Exception) {
                    return@withContext ImportResult.Error("Invalid JSON format in data file.")
                }

                // Convert JsonElement values to plain types
                val plainMap = jsonElementMapToPlain(dataMap)
                progress("Importing questions...", 0.6f)
                val error = bookImporter.importData(plainMap, packageId) { importProgress ->
                    val mapped = 0.6f + importProgress.coerceIn(0f, 1f) * 0.35f
                    progress("Importing questions... ${(importProgress * 100).toInt()}%", mapped)
                }
                if (error != null) {
                    destDir.deleteRecursively()
                    return@withContext ImportResult.Error(error)
                }

                progress("Complete!", 1.0f)
                ImportResult.Success(packageName)
            } catch (e: Exception) {
                destDir?.deleteRecursively()
                ImportResult.Error("Unexpected error: ${e.message}")
            } finally {
                tempPackage?.delete()
            }
        }
    }

    suspend fun importBuiltInPackage(assetPath: String): ImportResult {
        return withContext(Dispatchers.IO) {
            var tempPackage: File? = null
            try {
                val packagesDir = File(context.filesDir, "packages").apply { mkdirs() }
                val packageName = assetPath.substringAfterLast("/").substringBeforeLast(".")
                val packageId = "${packageName}_builtin"
                val destDir = File(packagesDir, packageId)
                if (destDir.exists()) destDir.deleteRecursively()
                destDir.mkdirs()

                tempPackage = File.createTempFile("builtin-import-", ".mnemorapkg", context.cacheDir)
                context.assets.open(assetPath).use { stream ->
                    copyToFile(stream, tempPackage)
                }
                extractZip(tempPackage, destDir)

                val dataFile = findDataJson(destDir)
                    ?: return@withContext ImportResult.Error("Built-in package has no data.json.")

                val content = dataFile.readText()
                @Suppress("UNCHECKED_CAST")
                val dataMap = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(content)
                val plainMap = jsonElementMapToPlain(dataMap)
                val error = bookImporter.importData(plainMap, packageId)
                if (error != null) {
                    destDir.deleteRecursively()
                    return@withContext ImportResult.Error(error)
                }

                ImportResult.Success(packageName)
            } catch (e: Exception) {
                ImportResult.Error("Failed to load built-in package: ${e.message}")
            } finally {
                tempPackage?.delete()
            }
        }
    }

    fun getPackageImagePath(packageId: String): String? {
        val dir = File(context.filesDir, "packages/$packageId")
        return if (dir.exists()) dir.absolutePath else null
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }

    private fun getFileSize(uri: Uri): Long {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (index >= 0) return cursor.getLong(index)
                }
            }
        }
        return uri.path?.let { File(it).takeIf { file -> file.exists() }?.length() } ?: -1L
    }

    private fun copyToFile(
        input: InputStream,
        outputFile: File,
        onBytesCopied: ((Long) -> Unit)? = null
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        FileOutputStream(outputFile).use { output ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                copied += read
                onBytesCopied?.invoke(copied)
            }
        }
    }

    private fun extractZip(zipFile: File, destDir: File, onProgress: ((Float) -> Unit)? = null) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().asSequence().toList()
            val files = entries.filterNot { it.isDirectory }
            val totalWork = files.size.coerceAtLeast(1)
            var completed = 0

            for (entry in entries) {
                val outFile = resolveZipEntry(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    completed++
                    onProgress?.invoke((completed.toFloat() / totalWork).coerceIn(0f, 1f))
                }
            }
        }
    }

    private fun resolveZipEntry(destDir: File, entryName: String): File {
        val target = File(destDir, entryName)
        val destPath = destDir.canonicalPath + File.separator
        val targetPath = target.canonicalPath
        if (!targetPath.startsWith(destPath)) {
            throw IllegalArgumentException("Invalid zip entry path: $entryName")
        }
        return target
    }

    private fun findDataJson(dir: File): File? {
        val direct = File(dir, "data.json")
        if (direct.exists()) return direct
        dir.listFiles { file -> file.isDirectory }?.forEach { subDir ->
            val nested = File(subDir, "data.json")
            if (nested.exists()) {
                // Flatten
                subDir.listFiles()?.forEach { it.renameTo(File(dir, it.name)) }
                subDir.deleteRecursively()
                return File(dir, "data.json")
            }
        }
        // Try any json
        dir.listFiles { it.extension == "json" && !it.name.contains("manifest") }?.firstOrNull()?.let {
            return it
        }
        return null
    }

    private fun jsonElementMapToPlain(map: Map<String, kotlinx.serialization.json.JsonElement>): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((k, v) in map) {
            result[k] = when (v) {
                is kotlinx.serialization.json.JsonObject -> jsonElementMapToPlain(v)
                is kotlinx.serialization.json.JsonArray -> v.map { element ->
                    when (element) {
                        is kotlinx.serialization.json.JsonObject -> jsonElementMapToPlain(element)
                        is kotlinx.serialization.json.JsonPrimitive -> element.content
                        else -> null
                    }
                }
                is kotlinx.serialization.json.JsonPrimitive -> {
                    if (v.isString) v.content
                    else v.content.toIntOrNull() ?: v.content.toDoubleOrNull() ?: v.content
                }
                else -> null
            }
        }
        return result
    }
}
