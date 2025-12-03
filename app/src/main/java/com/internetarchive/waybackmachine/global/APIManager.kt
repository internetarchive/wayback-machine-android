package com.internetarchive.waybackmachine.global

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.CookieManager
import java.net.CookieHandler
import java.net.CookiePolicy

class APIManager private constructor(context: Context?) {
    companion object : SingletonHolder<APIManager, Context?>(::APIManager)
    
    private var mContext: Context? = context
    
    init {
        // Set up CookieManager to handle cookies automatically (like JavaScript credentials: 'include')
        // This allows anonymous requests to send cookies if available from previous visits
        // JavaScript uses: fetch(..., { credentials: 'include' })
        try {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(cookieManager)
            android.util.Log.d("APIManager", "CookieManager initialized (will send cookies automatically if available)")
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error setting up CookieManager", e)
        }
    }

    private val BaseURL = "https://archive.org/"
    private val API_LOGIN = "services/xauthn/?op=login"
    private val API_LOGIN_ALT = "services/xauthn/"
    private val API_AVATAR = "services/xauthn/?op=avatar"
    private val API_UPLOAD = "services/xauthn/?op=upload"
    private val API_CHECK_PLAYBACK = "services/xauthn/?op=check_playback"
    private val SPN2_URL = "https://web.archive.org/save/"
    private val SPN2_STATUS_URL = "https://web.archive.org/save/status/"
    private val SPN2_SYSTEM_STATUS_URL = "https://web.archive.org/save/status/system"
    private val SPN2_USER_STATUS_URL = "https://web.archive.org/save/status/user"

    fun login(email: String, password: String, completion: (Boolean, String?, JSONObject?) -> Unit) {
        try {
            // Create a background thread for the API call
            Thread {
                try {
                    val url = URL("$BaseURL$API_LOGIN")
                    val connection = url.openConnection() as HttpURLConnection
                    
                    // Set up the connection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/1.6.0")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.doOutput = true
                    connection.doInput = true
                    
                    // Prepare the login data - use the original format
                    val postData = "email=$email&password=$password"
                    
                    
                    // Send the request
                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(postData)
                    writer.flush()
                    writer.close()
                    outputStream.close()
                    
                    // Get the response
                    val responseCode = connection.responseCode
                    
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Read the response
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        inputStream.close()
                        
                        
                        try {
                            // Parse the JSON response
                            val jsonResponse = JSONObject(response.toString())
                            
                            // Check if login was successful
                            val success = jsonResponse.optBoolean("success", false)
                            
                            if (success) {
                                // Login successful
                                completion(true, null, jsonResponse)
                            } else {
                                // Login failed - get error message from response
                                val errorMessage = jsonResponse.optString("error", "Login failed")
                                android.util.Log.e("APIManager", "Login failed: $errorMessage")
                                completion(false, errorMessage, null)
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("APIManager", "Error parsing login response", e)
                            completion(false, "Invalid response from server", null)
                        }
                        
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        // 401 Unauthorized - read error response
                        val errorStream = connection.errorStream
                        val reader = BufferedReader(InputStreamReader(errorStream))
                        val errorResponse = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            errorResponse.append(line)
                        }
                        
                        reader.close()
                        errorStream.close()
                        
                        android.util.Log.e("APIManager", "401 Unauthorized - Error response: ${errorResponse.toString()}")
                        
                        // Try to parse error response
                        try {
                            val errorJson = JSONObject(errorResponse.toString())
                            val errorMessage = errorJson.optString("error", "Invalid credentials")
                            completion(false, errorMessage, null)
                        } catch (e: Exception) {
                            completion(false, "Invalid credentials (401 Unauthorized)", null)
                        }
                        
                    } else {
                        // Other HTTP error
                        val errorMessage = "HTTP Error: $responseCode"
                        android.util.Log.e("APIManager", errorMessage)
                        
                        // Try to read error response
                        try {
                            val errorStream = connection.errorStream
                            if (errorStream != null) {
                                val reader = BufferedReader(InputStreamReader(errorStream))
                                val errorResponse = StringBuilder()
                                var line: String?
                                
                                while (reader.readLine().also { line = it } != null) {
                                    errorResponse.append(line)
                                }
                                
                                reader.close()
                                errorStream.close()
                                
                                android.util.Log.e("APIManager", "Error response: ${errorResponse.toString()}")
                                
                                // Try to parse error response
                                try {
                                    val errorJson = JSONObject(errorResponse.toString())
                                    val errorMsg = errorJson.optString("error", "Login failed")
                                    completion(false, errorMsg, null)
                                } catch (e: Exception) {
                                    completion(false, "Login failed - HTTP $responseCode", null)
                                }
                            } else {
                                completion(false, "Login failed - HTTP $responseCode", null)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("APIManager", "Could not read error response", e)
                            completion(false, "Login failed - HTTP $responseCode", null)
                        }
                    }
                    
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Error during login", e)
                    completion(false, "Network error: ${e.message}", null)
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error in login", e)
            completion(false, "Login error: ${e.message}", null)
        }
    }

    fun uploadFile(file: File, title: String, description: String, subject: String, s3AccessKey: String, s3SecretKey: String, username: String, completion: (Boolean, String?) -> Unit) {
        try {
            // Generate unique identifier
            val identifier = "${username}_${System.currentTimeMillis() / 1000L}"
            val filename = "$identifier${getFileExtension(file.name)}"
            
            // Prepare upload parameters
            val params = mapOf(
                "identifier" to identifier,
                "filename" to filename,
                "path" to file.absolutePath,
                "s3accesskey" to s3AccessKey,
                "s3secretkey" to s3SecretKey,
                "title" to title,
                "description" to description,
                "tags" to subject,
                "username" to username,
                "mediatype" to if (filename.lowercase().contains(".mp4")) "movies" else "image"
            )
            
            // Call the real upload implementation
            uploadFileToS3(params) { success, _, _, error ->
                if (success) {
                    completion(true, identifier)
                } else {
                    completion(false, "Upload failed: $error")
                }
            }
            
        } catch (e: Exception) {
            completion(false, "Upload error: ${e.message}")
        }
    }
    
    private fun getFileExtension(filename: String): String {
        return if (filename.contains(".")) {
            filename.substring(filename.lastIndexOf(".")).lowercase()
        } else {
            ".jpg" // Default extension
        }
    }
    
    private fun getFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    private fun uploadFileToS3(params: Map<String, String>, completion: (Boolean, String?, String?, String?) -> Unit) {
        try {
            val authorization = "LOW " + params["s3accesskey"] + ":" + params["s3secretkey"]
            val url = "https://s3.us.archive.org" + "/" + params["identifier"] + "/" + params["filename"]
            val file = File(params["path"]!!)
            
            Thread {
                try {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Content-Type", "application/octet-stream")
                    connection.setRequestProperty("X-File-Name", params["filename"]!!)
                    connection.setRequestProperty("x-amz-acl", "bucket-owner-full-control")
                    connection.setRequestProperty("x-amz-auto-make-bucket", "1")
                    connection.setRequestProperty("x-archive-meta-collection", "opensource_media")
                    connection.setRequestProperty("x-archive-meta-mediatype", params["mediatype"]!!)
                    connection.setRequestProperty("x-archive-meta-title", params["title"]!!)
                    connection.setRequestProperty("x-archive-meta-description", params["description"]!!)
                    connection.setRequestProperty("x-archive-meta-subject", params["tags"]!!)
                    connection.setRequestProperty("x-archive-meta-creator", params["username"]!!)
                    connection.setRequestProperty("x-archive-meta-licenseurl", "http://creativecommons.org/licenses/by/3.0/")
                    connection.setRequestProperty("x-archive-meta-uploader", params["username"]!!)
                    connection.setRequestProperty("x-archive-meta-publicdate", java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date()))
                    connection.setRequestProperty("authorization", authorization)
                    connection.doOutput = true
                    
                    // Upload the file
                    connection.outputStream.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    val responseCode = connection.responseCode
                    
                    if (responseCode in 200..299) {
                        completion(true, getFileSize(file.length()), url, null)
                    } else {
                        val errorMessage = "HTTP $responseCode: ${connection.responseMessage}"
                        completion(false, null, null, errorMessage)
                    }
                    
                } catch (e: Exception) {
                    completion(false, null, null, e.message)
                }
            }.start()
            
        } catch (e: Exception) {
            completion(false, null, null, e.message)
        }
    }

    fun checkPlaybackAvailability(url: String, timestamp: String, completion: (Boolean, String?) -> Unit) {
        try {
            // Create a background thread for the API call
            Thread {
                try {
                    // Construct the Wayback Machine API URL
                    
                    // For now, we'll use a simple approach - construct the Wayback Machine URL
                    // and let the user open it to see if it exists
                    val finalUrl = if (timestamp.isEmpty()) {
                        "https://web.archive.org/web/*/http://$url"
                    } else {
                        "https://web.archive.org/web/$timestamp/http://$url"
                    }
                    
                    // Return success with the Wayback Machine URL
                    completion(true, finalUrl)
                    
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Error checking playback availability", e)
                    completion(false, null)
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error in checkPlaybackAvailability", e)
            completion(false, null)
        }
    }

    // Request capture using SPN2 API
    fun requestCapture(url: String, loggedInSig: String, loggedInUser: String, s3AccessKey: String, s3SecretKey: String, completion: (String?, String?) -> Unit) {
        try {
            Thread {
                try {
                    android.util.Log.d("APIManager", "=== SPN2 requestCapture START ===")
                    android.util.Log.d("APIManager", "URL: $url")
                    android.util.Log.d("APIManager", "SPN2_URL: $SPN2_URL")
                    android.util.Log.d("APIManager", "loggedInSig length: ${loggedInSig.length}")
                    android.util.Log.d("APIManager", "loggedInUser: $loggedInUser")
                    android.util.Log.d("APIManager", "s3AccessKey provided: ${s3AccessKey.isNotEmpty()}")
                    
                    // Extension uses: POST to save/?url=... with form-encoded body
                    // API docs also show GET with query params works, so let's try extension format
                    // (query params in URL + form body - both have the URL)
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val urlWithQuery = "$SPN2_URL?url=$encodedUrl"
                    android.util.Log.d("APIManager", "Request URL with query: $urlWithQuery")
                    
                    // JavaScript uses credentials: 'include' which sends cookies automatically
                    // CookieManager is set up in init() to handle this automatically
                    // For anonymous mode, cookies from previous visits to archive.org will be included
                    val connection = URL(urlWithQuery).openConnection() as HttpURLConnection
                    
                    // Determine if user is logged in (for Accept header and response parsing)
                    // Note: Even when anonymous, CookieManager may send session cookies
                    val isLoggedIn = (s3AccessKey.isNotEmpty() && s3SecretKey.isNotEmpty()) ||
                                    (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty())
                    
                    // Set up the connection - match API docs format
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    
                    // Set Accept header based on login status (matches JavaScript behavior)
                    // When logged in: Accept: application/json
                    // When not logged in: Accept: text/html,application/xhtml+xml,application/xml
                    if (isLoggedIn) {
                        connection.setRequestProperty("Accept", "application/json")
                        android.util.Log.d("APIManager", "Accept header: application/json (logged in)")
                    } else {
                        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml")
                        android.util.Log.d("APIManager", "Accept header: text/html (anonymous mode)")
                    }
                    
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
                    connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
                    
                    // API docs: S3 API Keys (highly preferable) OR Cookies
                    // Priority: S3 keys > Cookies > Anonymous
                    if (s3AccessKey.isNotEmpty() && s3SecretKey.isNotEmpty()) {
                        // Use S3 keys (preferred method per API docs)
                        val authHeader = "LOW $s3AccessKey:$s3SecretKey"
                        connection.setRequestProperty("Authorization", authHeader)
                        android.util.Log.d("APIManager", "Authorization header set using S3 keys (preferred method)")
                    } else if (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty()) {
                        // Fall back to cookies if S3 keys not available
                        val encodedUser = java.net.URLEncoder.encode(loggedInUser, "UTF-8")
                        val cookieHeader = "logged-in-sig=$loggedInSig; logged-in-user=$encodedUser"
                        connection.setRequestProperty("Cookie", cookieHeader)
                        android.util.Log.d("APIManager", "Cookie header set (length: ${cookieHeader.length})")
                        android.util.Log.d("APIManager", "Cookie header: logged-in-sig=[${loggedInSig.take(20)}...] logged-in-user=$encodedUser")
                    } else {
                        android.util.Log.d("APIManager", "No explicit authentication provided - using anonymous mode (HTML response expected)")
                        android.util.Log.d("APIManager", "CookieManager may send session cookies from previous visits (like JavaScript credentials: 'include')")
                    }
                    
                    // Also set all response headers we receive for debugging
                    connection.setRequestProperty("Referer", "https://web.archive.org/")
                    connection.setRequestProperty("Origin", "https://web.archive.org")
                    
                    // Note: CookieManager will automatically add cookies from CookieHandler if available
                    // This matches JavaScript behavior: credentials: 'include'
                    
                    connection.doOutput = true
                    connection.doInput = true
                    
                    // Log all request headers for debugging (BEFORE writing to output stream)
                    android.util.Log.d("APIManager", "Request method: ${connection.requestMethod}")
                    android.util.Log.d("APIManager", "Request URL: ${connection.url}")
                    android.util.Log.d("APIManager", "Request headers:")
                    connection.requestProperties.forEach { (key, values) ->
                        android.util.Log.d("APIManager", "  $key: ${values.joinToString()}")
                    }
                    
                    // Prepare form-encoded body (API docs format)
                    val formBody = "url=$encodedUrl"
                    android.util.Log.d("APIManager", "Request form body: $formBody")
                    
                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream, "UTF-8")
                    writer.write(formBody)
                    writer.flush()
                    writer.close()
                    outputStream.close()
                    
                    val responseCode = connection.responseCode
                    android.util.Log.d("APIManager", "Response code: $responseCode")
                    android.util.Log.d("APIManager", "Response headers:")
                    connection.headerFields.forEach { (key, values) ->
                        android.util.Log.d("APIManager", "  $key: ${values?.joinToString()}")
                    }
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        inputStream.close()
                        
                        val responseStr = response.toString()
                        android.util.Log.d("APIManager", "Response body (first 500 chars): ${responseStr.take(500)}")
                        android.util.Log.d("APIManager", "Response body length: ${responseStr.length}")
                        android.util.Log.d("APIManager", "Response trimmed: ${responseStr.trim()}")
                        
                        // Check if response is empty or just {}
                        // For anonymous users, even empty responses should be checked for HTML job_id
                        if (responseStr.trim().isEmpty() || responseStr.trim() == "{}") {
                            android.util.Log.w("APIManager", "Empty response from server")
                            android.util.Log.w("APIManager", "Is logged in: $isLoggedIn")
                            
                            // For anonymous, try to extract job_id from any available source
                            if (!isLoggedIn) {
                                // Check response headers for any job_id hint
                                android.util.Log.d("APIManager", "Response headers: ${connection.headerFields}")
                                // Default error message for anonymous
                                completion(null, "Please Try Again")
                            } else {
                                android.util.Log.e("APIManager", "Empty response from server. This might indicate authentication failure.")
                                completion(null, null)
                            }
                        } else {
                            // Determine if user is logged in for response parsing
                            val isLoggedIn = (s3AccessKey.isNotEmpty() && s3SecretKey.isNotEmpty()) ||
                                            (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty())
                            
                            if (isLoggedIn) {
                                // Parse JSON response (logged in)
                                try {
                                    val jsonResponse = JSONObject(responseStr)
                                    
                                    // Extract job_id first - it may exist even with a message
                                    val jobId = jsonResponse.optString("job_id", "")
                                    val message = jsonResponse.optString("message", "")
                                    
                                    android.util.Log.d("APIManager", "Extracted job_id: $jobId")
                                    if (message.isNotEmpty()) {
                                        android.util.Log.d("APIManager", "API message: $message")
                                    }
                                    
                                    if (jobId.isEmpty()) {
                                        // No job_id - this is a real error
                                        if (message.isNotEmpty()) {
                                            android.util.Log.e("APIManager", "API error message (no job_id): $message")
                                            android.util.Log.e("APIManager", "Full response: $responseStr")
                                            completion(null, message)
                                        } else {
                                            android.util.Log.e("APIManager", "No job_id in response. Full response: $responseStr")
                                            val keys = jsonResponse.keys()
                                            val keysList = keys.asSequence().joinToString(", ")
                                            android.util.Log.e("APIManager", "Response keys: $keysList")
                                            
                                            // Check all available fields
                                            keys.forEach { key ->
                                                android.util.Log.e("APIManager", "Response field: $key = ${jsonResponse.opt(key)}")
                                            }
                                            completion(null, null)
                                        }
                                    } else {
                                        // Job_id exists - success! Message is informational (e.g., "same snapshot")
                                        android.util.Log.d("APIManager", "=== SPN2 requestCapture SUCCESS: job_id=$jobId ===")
                                        if (message.isNotEmpty()) {
                                            android.util.Log.d("APIManager", "Informational message: $message")
                                        }
                                        // Return job_id along with message (if any)
                                        completion(jobId, if (message.isNotEmpty()) message else null)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("APIManager", "Error parsing JSON response", e)
                                    android.util.Log.e("APIManager", "Response string that failed to parse: $responseStr")
                                    e.printStackTrace()
                                    completion(null, null)
                                }
                            } else {
                                // Parse HTML response (anonymous mode) - extract job_id from HTML
                                android.util.Log.d("APIManager", "Parsing HTML response (anonymous mode)")
                                android.util.Log.d("APIManager", "HTML response length: ${responseStr.length}")
                                android.util.Log.d("APIManager", "HTML contains 'spn2': ${responseStr.contains("spn2", ignoreCase = true)}")
                                
                                val jobId = extractJobIdFromHTML(responseStr)
                                android.util.Log.d("APIManager", "Job ID extraction result: ${if (jobId.isNotEmpty()) "Found: $jobId" else "Not found"}")
                                
                                // Check if this is the form page (not a save response)
                                // Form page is typically > 100KB and contains form elements
                                val isFormPage = responseStr.length > 100000 && 
                                                (responseStr.contains("Save Page Now", ignoreCase = true) || 
                                                 responseStr.contains("<form", ignoreCase = true) ||
                                                 responseStr.contains("id=\"spn-form\"", ignoreCase = true))
                                
                                if (isFormPage && jobId.isEmpty()) {
                                    // This is the form page, not a save response
                                    // The API returned the form instead of processing the save
                                    // This typically means authentication is required
                                    android.util.Log.e("APIManager", "API returned form page instead of processing save")
                                    android.util.Log.e("APIManager", "HTML length: ${responseStr.length}, contains form: ${responseStr.contains("<form", ignoreCase = true)}")
                                    
                                    // The API requires authentication for saves
                                    completion(null, "You need to be logged in to use Save Page Now.")
                                } else if (jobId.isNotEmpty()) {
                                    // Success! Job_id found in HTML response
                                    // The API may embed job_id in the form page HTML when processing saves
                                    android.util.Log.d("APIManager", "=== SPN2 requestCapture SUCCESS (anonymous): job_id=$jobId ===")
                                    
                                    // Extract message only if it's a meaningful one (not form default text)
                                    // When we have a valid job_id, form default messages should be ignored
                                    val message = extractMessageFromHTML(responseStr)
                                    
                                    // List of generic form messages to ignore when we have a valid job_id
                                    val genericFormMessages = listOf(
                                        "Please enter a valid web address",
                                        "Please enter a valid web address.",
                                        "Please enter a valid website",
                                        "Please enter a valid website.",
                                        "Enter a valid URL",
                                        "Enter a valid URL."
                                    )
                                    
                                    val isGenericMessage = genericFormMessages.any { 
                                        message.equals(it, ignoreCase = true) || 
                                        message.contains(it, ignoreCase = true)
                                    }
                                    
                                    val meaningfulMessage = if (message.isNotEmpty() && !isGenericMessage && 
                                        !message.contains("form", ignoreCase = true) &&
                                        !message.startsWith("Please enter", ignoreCase = true)) {
                                        // Check for "same snapshot" message specifically (this is meaningful)
                                        if (responseStr.contains("same snapshot", ignoreCase = true)) {
                                            val snapshotPattern = Regex(""".*?(same snapshot[^<]*).*?""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                                            val snapshotMatch = snapshotPattern.find(responseStr)
                                            snapshotMatch?.groupValues?.getOrNull(1)?.trim() ?: message
                                        } else {
                                            message
                                        }
                                    } else {
                                        // Check for "same snapshot" message specifically (only meaningful message in form page)
                                        if (responseStr.contains("same snapshot", ignoreCase = true)) {
                                            val snapshotPattern = Regex(""".*?(same snapshot[^<]*).*?""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                                            val snapshotMatch = snapshotPattern.find(responseStr)
                                            snapshotMatch?.groupValues?.getOrNull(1)?.trim() ?: null
                                        } else {
                                            // Ignore generic form messages when we have a valid job_id
                                            null
                                        }
                                    }
                                    
                                    if (meaningfulMessage != null) {
                                        android.util.Log.d("APIManager", "Informational message: $meaningfulMessage")
                                    } else {
                                        android.util.Log.d("APIManager", "Ignoring generic form message: $message")
                                    }
                                    completion(jobId, meaningfulMessage)
                                } else {
                                    // No job_id found in HTML
                                    android.util.Log.e("APIManager", "No job_id found in HTML response")
                                    // JavaScript: const errMsg = (loggedInFlag && data?.message) || 'Please Try Again'
                                    // When not logged in and no message, use default
                                    val message = extractMessageFromHTML(responseStr)
                                    completion(null, if (message.isNotEmpty()) message else "Please Try Again")
                                }
                            }
                        }
                    } else {
                        // Handle other response codes (including 401)
                        android.util.Log.e("APIManager", "HTTP Error: $responseCode")
                        android.util.Log.e("APIManager", "Response message: ${connection.responseMessage}")
                        android.util.Log.e("APIManager", "Is logged in: $isLoggedIn")
                        
                        val errorStream = if (responseCode >= 400) connection.errorStream else connection.inputStream
                        var errorMessage = if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                            if (!isLoggedIn) {
                                // For anonymous users getting 401, API requires authentication
                                "You need to be logged in to use Save Page Now."
                            } else {
                                "You need to be logged in to use Save Page Now."
                            }
                        } else if (!isLoggedIn) {
                            "Please Try Again"
                        } else {
                            "Failed to save page. Please try again."
                        }
                        
                        // Read response body - might be HTML for anonymous, JSON for logged in
                        if (errorStream != null) {
                            try {
                                val reader = BufferedReader(InputStreamReader(errorStream))
                                val errorResponse = StringBuilder()
                                var line: String?
                                
                                while (reader.readLine().also { line = it } != null) {
                                    errorResponse.append(line)
                                }
                                
                                reader.close()
                                errorStream.close()
                                val errorBody = errorResponse.toString()
                                android.util.Log.e("APIManager", "Error response body (first 1000 chars): ${errorBody.take(1000)}")
                                
                                if (!isLoggedIn) {
                                    // For anonymous, try to extract job_id first (even from error response)
                                    // JavaScript extracts job_id even if there's an error message
                                    val jobIdFromError = extractJobIdFromHTML(errorBody)
                                    if (jobIdFromError.isNotEmpty()) {
                                        android.util.Log.d("APIManager", "Found job_id in error/response body: $jobIdFromError")
                                        val htmlMessage = extractMessageFromHTML(errorBody)
                                        completion(jobIdFromError, if (htmlMessage.isNotEmpty()) htmlMessage else null)
                                        connection.disconnect()
                                        return@Thread
                                    }
                                    
                                    // If no job_id, try to extract message from HTML
                                    val htmlMessage = extractMessageFromHTML(errorBody)
                                    if (htmlMessage.isNotEmpty()) {
                                        errorMessage = htmlMessage
                                    }
                                } else {
                                    // For logged in, try JSON
                                    try {
                                        val jsonError = JSONObject(errorBody)
                                        if (jsonError.has("message")) {
                                            errorMessage = jsonError.getString("message")
                                        }
                                    } catch (e: Exception) {
                                        // If not JSON, use default message
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("APIManager", "Error reading error stream", e)
                            }
                        }
                        
                        android.util.Log.e("APIManager", "=== SPN2 requestCapture FAILED: HTTP $responseCode === Message: $errorMessage")
                        completion(null, errorMessage)
                    }
                    
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Exception during requestCapture", e)
                    android.util.Log.e("APIManager", "Exception message: ${e.message}")
                    e.printStackTrace()
                    android.util.Log.e("APIManager", "=== SPN2 requestCapture EXCEPTION ===")
                    completion(null, null)
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Exception in requestCapture", e)
            android.util.Log.e("APIManager", "Exception message: ${e.message}")
            e.printStackTrace()
            completion(null, null)
        }
    }
    
    // Check capture status with polling
    fun requestCaptureStatus(jobId: String, loggedInSig: String, loggedInUser: String, s3AccessKey: String, s3SecretKey: String, completion: (String?, String?) -> Unit) {
        try {
            Thread {
                try {
                    android.util.Log.d("APIManager", "=== SPN2 requestCaptureStatus START ===")
                    android.util.Log.d("APIManager", "jobId: $jobId")
                    android.util.Log.d("APIManager", "SPN2_STATUS_URL: $SPN2_STATUS_URL")
                    
                    // According to API docs: POST with form-encoded body
                    // curl -X POST -H "Accept: application/json" -d'job_id=ac58789b-f3ca-48d0-9ea6-1d1225e98695' 
                    //      --cookie "logged-in-sig=xxx;logged-in-user=user1%40archive.org;" https://web.archive.org/save/status
                    val connection = URL(SPN2_STATUS_URL).openConnection() as HttpURLConnection
                    
                    // Set up the connection - match API docs format
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
                    connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
                    
                    // Determine if user is logged in
                    val isLoggedIn = (s3AccessKey.isNotEmpty() && s3SecretKey.isNotEmpty()) ||
                                    (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty())
                    
                    // JavaScript: Accept header required when logged-out, even though response is in JSON.
                    // headers.set('Accept', 'text/html,application/xhtml+xml,application/xml')
                    if (!isLoggedIn) {
                        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml")
                        android.util.Log.d("APIManager", "Status request: Accept header set to HTML (anonymous mode)")
                    } else {
                        connection.setRequestProperty("Accept", "application/json")
                        android.util.Log.d("APIManager", "Status request: Accept header set to JSON (logged in)")
                    }
                    
                    // API docs: S3 API Keys (highly preferable) OR Cookies
                    // Priority: S3 keys > Cookies
                    if (s3AccessKey.isNotEmpty() && s3SecretKey.isNotEmpty()) {
                        // Use S3 keys (preferred method per API docs)
                        val authHeader = "LOW $s3AccessKey:$s3SecretKey"
                        connection.setRequestProperty("Authorization", authHeader)
                        android.util.Log.d("APIManager", "Status request: Authorization header set using S3 keys")
                    } else if (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty()) {
                        // Fall back to cookies if S3 keys not available
                        val encodedUser = java.net.URLEncoder.encode(loggedInUser, "UTF-8")
                        connection.setRequestProperty("Cookie", "logged-in-sig=$loggedInSig;logged-in-user=$encodedUser;")
                        android.util.Log.d("APIManager", "Status request: Cookie header set")
                    } else {
                        android.util.Log.d("APIManager", "Status request: No authentication provided (anonymous mode)")
                    }
                    
                    connection.doOutput = true
                    connection.doInput = true
                    
                    // Prepare form-encoded body (API docs format)
                    val formBody = "job_id=$jobId"
                    android.util.Log.d("APIManager", "Status request form body: $formBody")
                    
                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream, "UTF-8")
                    writer.write(formBody)
                    writer.flush()
                    writer.close()
                    outputStream.close()
                    
                    val responseCode = connection.responseCode
                    android.util.Log.d("APIManager", "Status response code: $responseCode")
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        inputStream.close()
                        
                        val responseStr = response.toString()
                        android.util.Log.d("APIManager", "Status response body: $responseStr")
                        
                        try {
                            val jsonResponse = JSONObject(responseStr)
                            val status = jsonResponse.optString("status", "")
                            android.util.Log.d("APIManager", "Status: $status")
                            
                            if (status == "pending") {
                                // Status is pending, will be polled again by the caller
                                android.util.Log.d("APIManager", "Status is pending, will poll again")
                                completion("pending", null)
                            } else {
                                // Status is complete - check for success or error
                                if (jsonResponse.has("timestamp") && jsonResponse.has("original_url")) {
                                    val timestamp = jsonResponse.getString("timestamp")
                                    val originalUrl = jsonResponse.getString("original_url")
                                    val waybackUrl = "http://web.archive.org/web/$timestamp/$originalUrl"
                                    android.util.Log.d("APIManager", "=== SPN2 requestCaptureStatus SUCCESS: $waybackUrl ===")
                                    completion(waybackUrl, null)
                                } else if (jsonResponse.has("message")) {
                                    val errorMessage = jsonResponse.getString("message")
                                    android.util.Log.e("APIManager", "Status error message: $errorMessage")
                                    android.util.Log.e("APIManager", "Full response: $responseStr")
                                    completion(null, errorMessage)
                                } else {
                                    android.util.Log.e("APIManager", "Unknown status response. Status: $status, Full response: $responseStr")
                                    completion(null, status)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("APIManager", "Error parsing status response", e)
                            android.util.Log.e("APIManager", "Response string: $responseStr")
                            e.printStackTrace()
                            completion(null, "Error parsing response: ${e.message}")
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        // 404 - job not found (might be expired or invalid)
                        android.util.Log.e("APIManager", "Job not found (404) - job_id might be invalid or expired: $jobId")
                        val errorStream = connection.errorStream
                        if (errorStream != null) {
                            try {
                                val reader = BufferedReader(InputStreamReader(errorStream))
                                val response = StringBuilder()
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    response.append(line)
                                }
                                reader.close()
                                errorStream.close()
                                android.util.Log.e("APIManager", "404 Error response body: ${response.toString()}")
                            } catch (e: Exception) {
                                android.util.Log.e("APIManager", "Could not read 404 error response", e)
                            }
                        }
                        completion(null, "Capture job not found. The job may have expired or been cancelled.")
                    } else {
                        val errorMessage = "HTTP Error: $responseCode"
                        android.util.Log.e("APIManager", errorMessage)
                        android.util.Log.e("APIManager", "Response message: ${connection.responseMessage}")
                        
                        // Try to read error response
                        val errorStream = connection.errorStream
                        if (errorStream != null) {
                            val reader = BufferedReader(InputStreamReader(errorStream))
                            val errorResponse = StringBuilder()
                            var line: String?
                            
                            while (reader.readLine().also { line = it } != null) {
                                errorResponse.append(line)
                            }
                            
                            reader.close()
                            errorStream.close()
                            android.util.Log.e("APIManager", "Error response body: $errorResponse")
                        }
                        android.util.Log.e("APIManager", "=== SPN2 requestCaptureStatus FAILED: HTTP $responseCode ===")
                        completion(null, errorMessage)
                    }
                    
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Error during requestCaptureStatus", e)
                    completion(null, "Network error: ${e.message}")
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error in requestCaptureStatus", e)
            completion(null, "Error: ${e.message}")
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            mContext?.let { context ->
                AppManager.getInstance(context).getVersionName()
            } ?: "2.0.0"
        } catch (e: Exception) {
            "2.0.0"
        }
    }
    
    // Check system status for SPN2 - matches JavaScript implementation
    fun checkSystemStatus(loggedInSig: String, loggedInUser: String, completion: (String?) -> Unit) {
        try {
            Thread {
                try {
                    android.util.Log.d("APIManager", "=== SPN2 checkSystemStatus START ===")
                    
                    val connection = URL(SPN2_SYSTEM_STATUS_URL).openConnection() as HttpURLConnection
                    
                    // Set up the connection - system status doesn't require auth according to API docs
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
                    connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
                    
                    // System status doesn't require auth - try without cookies first
                    connection.doInput = true
                    
                    val responseCode = connection.responseCode
                    android.util.Log.d("APIManager", "System status response code: $responseCode")
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        reader.close()
                        inputStream.close()
                        
                        val responseStr = response.toString()
                        android.util.Log.d("APIManager", "System status response body: $responseStr")
                        
                        try {
                            val jsonResponse = JSONObject(responseStr)
                            var errorMessage: String? = null
                            
                            // Match JavaScript logic exactly:
                            // if (data?.recent_captures < 100 || data?.status !== 'ok')
                            val status = jsonResponse.optString("status", "")
                            val recentCaptures = jsonResponse.optInt("recent_captures", -1)
                            
                            android.util.Log.d("APIManager", "System status: $status, recent_captures: $recentCaptures")
                            
                            // Match JavaScript logic exactly:
                            // if (data?.recent_captures < 100 || data?.status !== 'ok')
                            if ((recentCaptures > 0 && recentCaptures < 100) || (status.isNotEmpty() && status != "ok")) {
                                android.util.Log.d("APIManager", "System has issues: recent_captures=$recentCaptures, status=$status")
                                errorMessage = "Save Page Now has issues right now, please try again later."
                            } else if (recentCaptures > 3000) {
                                // else if (data?.recent_captures > 3000)
                                android.util.Log.d("APIManager", "System overloaded: recent_captures=$recentCaptures")
                                errorMessage = "Save Page Now is overloaded right now, please try again later."
                            } else {
                                // Check queues (note: JavaScript uses "queues" not "queue")
                                if (jsonResponse.has("queues")) {
                                    try {
                                        val queuesObj = jsonResponse.getJSONObject("queues")
                                        val keys = queuesObj.keys()
                                        var queueSum = 0
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            val queueValue = queuesObj.optInt(key, 0)
                                            queueSum += queueValue
                                            android.util.Log.d("APIManager", "Queue $key: $queueValue")
                                        }
                                        android.util.Log.d("APIManager", "Total queue sum: $queueSum")
                                        
                                        if (queueSum > 0) {
                                            errorMessage = "Save Page Now is overloaded right now, please try again later."
                                        }
                                    } catch (e: Exception) {
                                        // Queues might not be an object, or might be missing
                                        android.util.Log.d("APIManager", "Could not parse queues object: ${e.message}")
                                    }
                                }
                            }
                            
                            // If no system error, check user status (matches JavaScript flow)
                            // JavaScript: if (msg !== '') { show error } else { checkAuthentication -> checkUserStatus }
                            // Only check user status if authentication is provided (user status requires auth)
                            if (errorMessage == null) {
                                if (loggedInSig.isNotEmpty() && loggedInUser.isNotEmpty()) {
                                    android.util.Log.d("APIManager", "System status OK, checking user status (authenticated)")
                                    // Check user status (requires authentication)
                                    checkUserStatus(loggedInSig, loggedInUser) { userErrorMessage ->
                                        android.util.Log.d("APIManager", "=== SPN2 checkSystemStatus COMPLETE: errorMessage=$userErrorMessage ===")
                                        completion(userErrorMessage)
                                    }
                                    connection.disconnect()
                                    return@Thread
                                } else {
                                    android.util.Log.d("APIManager", "System status OK, skipping user status check (anonymous mode)")
                                    // No authentication, skip user status check
                                    completion(null)
                                }
                            }
                            
                            android.util.Log.d("APIManager", "=== SPN2 checkSystemStatus SUCCESS: errorMessage=$errorMessage ===")
                            completion(errorMessage)
                        } catch (e: Exception) {
                            android.util.Log.e("APIManager", "Error parsing system status response", e)
                            android.util.Log.e("APIManager", "Response string: $responseStr")
                            e.printStackTrace()
                            completion(null)
                        }
                    } else {
                        android.util.Log.e("APIManager", "HTTP Error: $responseCode")
                        android.util.Log.e("APIManager", "Response message: ${connection.responseMessage}")
                        
                        val errorStream = connection.errorStream
                        if (errorStream != null) {
                            val reader = BufferedReader(InputStreamReader(errorStream))
                            val errorResponse = StringBuilder()
                            var line: String?
                            
                            while (reader.readLine().also { line = it } != null) {
                                errorResponse.append(line)
                            }
                            
                            reader.close()
                            errorStream.close()
                            android.util.Log.e("APIManager", "Error response body: $errorResponse")
                        }
                        android.util.Log.e("APIManager", "=== SPN2 checkSystemStatus FAILED: HTTP $responseCode ===")
                        completion(null)
                    }
                    
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Exception during checkSystemStatus", e)
                    android.util.Log.e("APIManager", "Exception message: ${e.message}")
                    e.printStackTrace()
                    android.util.Log.e("APIManager", "=== SPN2 checkSystemStatus EXCEPTION ===")
                    completion(null)
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Exception in checkSystemStatus", e)
            android.util.Log.e("APIManager", "Exception message: ${e.message}")
            e.printStackTrace()
            completion(null)
        }
    }
    
    
    // Check user status for SPN2
    private fun checkUserStatus(loggedInSig: String, loggedInUser: String, completion: (String?) -> Unit) {
        try {
            android.util.Log.d("APIManager", "=== SPN2 checkUserStatus START ===")
            
            val connection = URL(SPN2_USER_STATUS_URL).openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
            connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
            
            // Set cookies for authentication
            val encodedUser = java.net.URLEncoder.encode(loggedInUser, "UTF-8")
            connection.setRequestProperty("Cookie", "logged-in-sig=$loggedInSig;logged-in-user=$encodedUser;")
            
            connection.doInput = true
            
            val responseCode = connection.responseCode
            android.util.Log.d("APIManager", "User status response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                
                reader.close()
                inputStream.close()
                
                val responseStr = response.toString()
                android.util.Log.d("APIManager", "User status response body: $responseStr")
                
                try {
                    val jsonResponse = JSONObject(responseStr)
                    var errorMessage: String? = null
                    
                    // Check: if (data?.available - data?.processing <= 1)
                    val available = jsonResponse.optInt("available", -1)
                    val processing = jsonResponse.optInt("processing", 0)
                    
                    android.util.Log.d("APIManager", "User status: available=$available, processing=$processing")
                    
                    if (available > 0 && (available - processing) <= 1) {
                        android.util.Log.d("APIManager", "User has too many active captures")
                        errorMessage = "You have done too many captures, please wait a few minutes and retry."
                    } else {
                        // Check: if (data?.daily_captures >= data?.daily_captures_limit)
                        val dailyCaptures = jsonResponse.optInt("daily_captures", -1)
                        val dailyCapturesLimit = jsonResponse.optInt("daily_captures_limit", -1)
                        
                        android.util.Log.d("APIManager", "User daily: captures=$dailyCaptures, limit=$dailyCapturesLimit")
                        
                        if (dailyCaptures > 0 && dailyCapturesLimit > 0 && dailyCaptures >= dailyCapturesLimit) {
                            android.util.Log.d("APIManager", "User exceeded daily capture limit")
                            errorMessage = "You have done too many captures today, please try again tomorrow."
                        }
                    }
                    
                    android.util.Log.d("APIManager", "=== SPN2 checkUserStatus SUCCESS: errorMessage=$errorMessage ===")
                    completion(errorMessage)
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Error parsing user status response", e)
                    android.util.Log.e("APIManager", "Response string: $responseStr")
                    e.printStackTrace()
                    completion(null)
                }
            } else {
                android.util.Log.e("APIManager", "HTTP Error: $responseCode")
                android.util.Log.e("APIManager", "Response message: ${connection.responseMessage}")
                
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val reader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    
                    reader.close()
                    errorStream.close()
                    android.util.Log.e("APIManager", "Error response body: $errorResponse")
                }
                android.util.Log.e("APIManager", "=== SPN2 checkUserStatus FAILED: HTTP $responseCode ===")
                completion(null)
            }
            
            connection.disconnect()
            
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Exception during checkUserStatus", e)
            android.util.Log.e("APIManager", "Exception message: ${e.message}")
            e.printStackTrace()
            completion(null)
        }
    }
    
    // Extract job_id from HTML response (for anonymous mode)
    // JavaScript: const jobRegex = /spn2-[a-z0-9-]*/g; const jobIds = html.match(jobRegex); return jobIds?.[0] || null;
    private fun extractJobIdFromHTML(html: String): String {
        try {
            // Match the spn id pattern exactly as JavaScript: /spn2-[a-z0-9-]*/g
            // JavaScript returns first match or null
            // Try multiple patterns to catch all variations
            val patterns = listOf(
                Regex("""spn2-[a-z0-9-]+""", RegexOption.IGNORE_CASE),  // Original pattern
                Regex("""spn2-[a-z0-9]{40,}""", RegexOption.IGNORE_CASE),  // Long hash pattern
                Regex("""["']job_id["']\s*:\s*["'](spn2-[^"']+)["']""", RegexOption.IGNORE_CASE),  // JSON-like in HTML
                Regex("""data-job-id=["'](spn2-[^"']+)["']""", RegexOption.IGNORE_CASE),  // Data attribute
                Regex("""id=["'](spn2-[^"']+)["']""", RegexOption.IGNORE_CASE)  // ID attribute
            )
            
            for (pattern in patterns) {
                val matches = pattern.findAll(html).toList()
                if (matches.isNotEmpty()) {
                    // Prefer longer matches (more complete job_id)
                    val bestMatch = matches.maxByOrNull { it.value.length }
                    if (bestMatch != null) {
                        val jobId = if (bestMatch.groupValues.size > 1) {
                            // Use captured group if available
                            bestMatch.groupValues[1]
                        } else {
                            bestMatch.value
                        }
                        android.util.Log.d("APIManager", "Extracted job_id from HTML: $jobId (pattern: ${pattern.pattern})")
                        return jobId
                    }
                }
            }
            
            // Log a sample of the HTML to help debug if no job_id found
            val htmlSample = html.take(2000)
            android.util.Log.d("APIManager", "Could not extract job_id from HTML")
            android.util.Log.d("APIManager", "HTML sample (first 2000 chars): $htmlSample")
            android.util.Log.d("APIManager", "HTML contains 'spn2': ${html.contains("spn2", ignoreCase = true)}")
            
            return ""
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error extracting job_id from HTML", e)
            return ""
        }
    }
    
    // Extract error message from HTML response (for anonymous mode)
    private fun extractMessageFromHTML(html: String): String {
        return try {
            // First, check if this is just the form page (not a response)
            // The form page typically has "Save Page Now" title or form elements
            if (html.contains("Save Page Now", ignoreCase = true) && 
                (html.contains("<form", ignoreCase = true) || html.contains("id=\"spn-form\"", ignoreCase = true))) {
                // This is the form page, not a response - check for validation errors
                // Look for common validation messages
                val validationPatterns = listOf(
                    Regex("""Please enter a valid web address""", RegexOption.IGNORE_CASE),
                    Regex("""invalid.*url""", RegexOption.IGNORE_CASE),
                    Regex("""error.*message[^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                )
                
                for (pattern in validationPatterns) {
                    val match = pattern.find(html)
                    if (match != null) {
                        val message = match.groupValues.getOrNull(1)?.replace(Regex("""<[^>]+>"""), "")?.trim() 
                                    ?: match.value.trim()
                        if (message.isNotEmpty() && message.length < 200) {
                            android.util.Log.d("APIManager", "Found validation message in form page: $message")
                            return message
                        }
                    }
                }
                
                // If it's the form page and no specific error, it means the request wasn't processed
                // This could mean authentication is required
                android.util.Log.d("APIManager", "HTML response is the form page - request may not have been processed")
                return "Please enter a valid web address" // Default form message
            }
            
            // Look for common error message patterns in HTML
            // Pattern 1: <div class="error">...</div> or <p class="error">...</p>
            val errorPattern1 = Regex("""<(?:div|p)[^>]*class\s*=\s*["'][^"']*error[^"']*["'][^>]*>(.*?)</(?:div|p)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val errorMatch = errorPattern1.find(html)
            if (errorMatch != null) {
                val message = errorMatch.groupValues[1].replace(Regex("""<[^>]+>"""), "").trim()
                if (message.isNotEmpty()) {
                    return message
                }
            }
            
            // Pattern 2: Look for "same snapshot" or similar messages
            if (html.contains("same snapshot", ignoreCase = true)) {
                val snapshotPattern = Regex(""".*?(same snapshot[^<]*).*?""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val snapshotMatch = snapshotPattern.find(html)
                if (snapshotMatch != null) {
                    return snapshotMatch.groupValues[1].trim()
                }
            }
            
            // Pattern 3: Look for any message in data attributes or script tags
            val dataMessagePattern = Regex("""data-message\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val dataMatch = dataMessagePattern.find(html)
            if (dataMatch != null) {
                return dataMatch.groupValues[1].trim()
            }
            
            ""
        } catch (e: Exception) {
            android.util.Log.e("APIManager", "Error extracting message from HTML", e)
            ""
        }
    }

}
