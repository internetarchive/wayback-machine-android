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

    private val BaseURL = "https://archive.org/"
    private val API_LOGIN = "services/xauthn/?op=login"
    private val API_LOGIN_ALT = "services/xauthn/"
    private val API_AVATAR = "services/xauthn/?op=avatar"
    private val API_UPLOAD = "services/xauthn/?op=upload"
    private val API_CHECK_PLAYBACK = "services/xauthn/?op=check_playback"

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
                                    val errorMessage = errorJson.optString("error", "Login failed")
                                    completion(false, errorMessage, null)
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

}
