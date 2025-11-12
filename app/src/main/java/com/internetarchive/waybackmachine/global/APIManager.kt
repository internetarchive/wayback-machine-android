package com.internetarchive.waybackmachine.global

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader

class APIManager private constructor(context: Context?) {
    companion object : SingletonHolder<APIManager, Context?>(::APIManager)
    
    private var mContext: Context? = context

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
    fun requestCapture(url: String, loggedInSig: String, loggedInUser: String, completion: (String?, String?) -> Unit) {
        try {
            Thread {
                try {
                    android.util.Log.d("APIManager", "=== SPN2 requestCapture START ===")
                    android.util.Log.d("APIManager", "URL: $url")
                    android.util.Log.d("APIManager", "SPN2_URL: $SPN2_URL")
                    android.util.Log.d("APIManager", "loggedInSig length: ${loggedInSig.length}")
                    android.util.Log.d("APIManager", "loggedInUser: $loggedInUser")
                    
                    // Extension uses: POST to save/?url=... with form-encoded body
                    // API docs also show GET with query params works, so let's try extension format
                    // (query params in URL + form body - both have the URL)
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val urlWithQuery = "$SPN2_URL?url=$encodedUrl"
                    android.util.Log.d("APIManager", "Request URL with query: $urlWithQuery")
                    
                    val connection = URL(urlWithQuery).openConnection() as HttpURLConnection
                    
                    // Set up the connection - match API docs format
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
                    connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
                    
                    // Set cookies - API docs format: logged-in-sig=xxx;logged-in-user=user1%40archive.org;
                    // Note: The API docs show logged-in-user should be URL-encoded email
                    // But we're getting it from login response, so it might already be in the right format
                    // Try both: URL-encoded and as-is
                    val encodedUser = java.net.URLEncoder.encode(loggedInUser, "UTF-8")
                    // API docs example: logged-in-user=user1%40archive.org; (URL-encoded)
                    val cookieHeader = "logged-in-sig=$loggedInSig; logged-in-user=$encodedUser"
                    connection.setRequestProperty("Cookie", cookieHeader)
                    android.util.Log.d("APIManager", "Cookie header set (length: ${cookieHeader.length})")
                    android.util.Log.d("APIManager", "Cookie header: logged-in-sig=[${loggedInSig.take(20)}...] logged-in-user=$encodedUser")
                    android.util.Log.d("APIManager", "Original loggedInUser value: $loggedInUser")
                    
                    // Also set all response headers we receive for debugging
                    connection.setRequestProperty("Referer", "https://web.archive.org/")
                    connection.setRequestProperty("Origin", "https://web.archive.org")
                    
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
                        android.util.Log.d("APIManager", "Response body: $responseStr")
                        
                        // Check if response is empty or just {}
                        if (responseStr.trim().isEmpty() || responseStr.trim() == "{}") {
                            android.util.Log.e("APIManager", "Empty response from server. This might indicate authentication failure.")
                            android.util.Log.e("APIManager", "Response headers: ${connection.headerFields}")
                            completion(null, null)
                        } else {
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
                                android.util.Log.e("APIManager", "Error parsing capture response", e)
                                android.util.Log.e("APIManager", "Response string that failed to parse: $responseStr")
                                e.printStackTrace()
                                completion(null, null)
                            }
                        }
                    } else {
                        // Read error response
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
                        } else {
                            android.util.Log.e("APIManager", "No error stream available")
                        }
                        android.util.Log.e("APIManager", "=== SPN2 requestCapture FAILED: HTTP $responseCode ===")
                        completion(null, null)
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
    fun requestCaptureStatus(jobId: String, loggedInSig: String, loggedInUser: String, completion: (String?, String?) -> Unit) {
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
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "Wayback_Machine_Android/${getAppVersion()}")
                    connection.setRequestProperty("Wayback-Extension-Version", "Wayback_Machine_Android/${getAppVersion()}")
                    
                    // Set cookies - API docs format: logged-in-sig=xxx;logged-in-user=user1%40archive.org;
                    val encodedUser = java.net.URLEncoder.encode(loggedInUser, "UTF-8")
                    connection.setRequestProperty("Cookie", "logged-in-sig=$loggedInSig;logged-in-user=$encodedUser;")
                    
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
                            if (errorMessage == null) {
                                android.util.Log.d("APIManager", "System status OK, checking user status")
                                // Check user status (requires authentication)
                                checkUserStatus(loggedInSig, loggedInUser) { userErrorMessage ->
                                    android.util.Log.d("APIManager", "=== SPN2 checkSystemStatus COMPLETE: errorMessage=$userErrorMessage ===")
                                    completion(userErrorMessage)
                                }
                                connection.disconnect()
                                return@Thread
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

}
