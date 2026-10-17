package com.aljaz.weighttracker.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Talks to Nextcloud over WebDAV (HTTP with extra methods for files).
 *
 * User files live under `<server>/remote.php/dav/files/<username>/`.
 *
 * @param httpClient Shared OkHttp client (timeouts are configured there).
 */
class NextcloudClient(
    private val httpClient: OkHttpClient,
) {

    /**
     * Checks that the server is reachable and the credentials are accepted,
     * by asking for the properties of the user's root folder (PROPFIND).
     */
    suspend fun testConnection(config: NextcloudConfig): NextcloudResult {
        val url = userRootUrl(config) ?: return failure(BackupError.INVALID_URL)
        val request = authorizedRequest(config)
            .url(url)
            .method("PROPFIND", null)
            .header("Depth", "0")  // only the folder itself, not its contents
            .build()
        return execute(request) { code -> code == HTTP_MULTI_STATUS }
    }

    /**
     * Uploads [content] as [fileName] into [folder], creating the folder
     * (and any missing parent folders) if needed. An existing file with
     * the same name is overwritten.
     *
     * @param folder Path relative to the user's root, e.g. "myapps/weighter/backups".
     */
    suspend fun upload(
        config: NextcloudConfig,
        folder: String,
        fileName: String,
        content: ByteArray,
    ): NextcloudResult {
        val folderUrl = folderUrl(config, folder) ?: return failure(BackupError.INVALID_URL)

        // 1) Make sure every folder level exists.
        val folderResult = createFolders(config, folder)
        if (folderResult is NextcloudResult.Failure) return folderResult

        // 2) Upload the file (PUT). 201 = new file, 204 = replaced existing file.
        val fileUrl = folderUrl.newBuilder().addPathSegment(fileName).build()
        val upload = authorizedRequest(config)
            .url(fileUrl)
            .put(content.toRequestBody(CSV_MEDIA_TYPE))
            .build()
        return execute(upload) { code -> code == HTTP_CREATED || code == HTTP_NO_CONTENT }
    }

    /**
     * Creates [folder] level by level ("myapps", then "myapps/weighter", …),
     * because WebDAV's MKCOL cannot create several levels at once.
     * 201 = created, 405 = already exists: both count as success.
     */
    private suspend fun createFolders(config: NextcloudConfig, folder: String): NextcloudResult {
        val segments = pathSegments(folder)
        for (depth in 1..segments.size) {
            val levelUrl = folderUrl(config, segments.take(depth).joinToString("/"))
                ?: return failure(BackupError.INVALID_URL)
            val request = authorizedRequest(config).url(levelUrl).method("MKCOL", null).build()
            val result = execute(request) { code ->
                code == HTTP_CREATED || code == HTTP_METHOD_NOT_ALLOWED
            }
            if (result is NextcloudResult.Failure) return result
        }
        return NextcloudResult.Success
    }

    /** Starts a request with HTTP Basic authentication. */
    private fun authorizedRequest(config: NextcloudConfig): Request.Builder =
        Request.Builder().header(
            "Authorization",
            Credentials.basic(config.username, config.appPassword),
        )

    /**
     * Sends [request] on a background thread and maps the response code.
     * @param isSuccess Decides which response codes count as success.
     */
    private suspend fun execute(
        request: Request,
        isSuccess: (Int) -> Boolean,
    ): NextcloudResult = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    isSuccess(response.code) -> NextcloudResult.Success
                    response.code == HTTP_UNAUTHORIZED -> failure(BackupError.UNAUTHORIZED)
                    response.code == HTTP_NOT_FOUND -> failure(BackupError.NOT_FOUND)
                    else -> failure(BackupError.SERVER)
                }
            }
        } catch (e: IOException) {
            failure(BackupError.NETWORK)
        }
    }

    private fun failure(error: BackupError) = NextcloudResult.Failure(error)

    companion object {
        private val CSV_MEDIA_TYPE = "text/csv; charset=utf-8".toMediaType()

        private const val HTTP_CREATED = 201
        private const val HTTP_NO_CONTENT = 204
        private const val HTTP_MULTI_STATUS = 207
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_METHOD_NOT_ALLOWED = 405

        /**
         * Cleans up a typed server address: trims spaces and trailing slashes
         * and adds "https://" if no scheme was typed.
         * @return The normalised URL, or null if it is invalid or not https.
         */
        fun normalizeServerUrl(input: String): String? {
            var url = input.trim().trimEnd('/')
            if (url.isEmpty()) return null
            if (!url.contains("://")) url = "https://$url"

            val parsed = url.toHttpUrlOrNull() ?: return null
            return if (parsed.isHttps) url else null
        }

        /** `<server>/remote.php/dav/files/<username>/` (the user's root folder). */
        internal fun userRootUrl(config: NextcloudConfig): HttpUrl? =
            davFilesBuilder(config)?.addPathSegment("")?.build()

        /**
         * `<server>/remote.php/dav/files/<username>/<folder>`.
         * [folder] may contain "/" for nested folders; each level is
         * encoded separately, so the slashes stay real path separators.
         */
        internal fun folderUrl(config: NextcloudConfig, folder: String): HttpUrl? {
            val builder = davFilesBuilder(config) ?: return null
            pathSegments(folder).forEach { builder.addPathSegment(it) }
            return builder.build()
        }

        /** Splits "a/b/c" into ["a", "b", "c"], ignoring empty parts like "a//b/". */
        private fun pathSegments(folder: String): List<String> =
            folder.split('/').filter { it.isNotBlank() }

        /**
         * Builds the common URL prefix. Path segments are added one by one,
         * so special characters in the username are encoded safely.
         */
        private fun davFilesBuilder(config: NextcloudConfig): HttpUrl.Builder? =
            config.serverUrl.toHttpUrlOrNull()
                ?.newBuilder()
                ?.addPathSegments("remote.php/dav/files")
                ?.addPathSegment(config.username)
    }
}