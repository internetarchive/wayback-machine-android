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
                    
                    android.util.Log.d("APIManager", "Attempting login to: $url")
                    android.util.Log.d("APIManager", "Post data: $postData")
                    
                    // Send the request
                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(postData)
                    writer.flush()
                    writer.close()
                    outputStream.close()
                    
                    // Get the response
                    val responseCode = connection.responseCode
                    android.util.Log.d("APIManager", "Response code: $responseCode")
                    android.util.Log.d("APIManager", "Response message: ${connection.responseMessage}")
                    
                    // Log all response headers for debugging
                    val headerFields = connection.headerFields
                    android.util.Log.d("APIManager", "Response headers: $headerFields")
                    
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
                        
                        android.util.Log.d("APIManager", "Response body: ${response.toString()}")
                        
                        try {
                            // Parse the JSON response
                            val jsonResponse = JSONObject(response.toString())
                            
                            // Check if login was successful
                            val success = jsonResponse.optBoolean("success", false)
                            
                            if (success) {
                                // Login successful
                                android.util.Log.d("APIManager", "Login successful")
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

    fun uploadFile(file: File, title: String, description: String, subject: String, completion: (Boolean, String?) -> Unit) {
        // TODO: Implement with proper HTTP client
        // For now, return a mock response
        completion(false, "Upload not implemented yet")
    }

    fun checkPlaybackAvailability(url: String, timestamp: String, completion: (Boolean, String?) -> Unit) {
        try {
            // Create a background thread for the API call
            Thread {
                try {
                    // Construct the Wayback Machine API URL
                    val waybackUrl = if (timestamp.isEmpty()) {
                        // For recent version, check if URL exists in archive
                        "https://web.archive.org/web/*/http://$url"
                    } else {
                        // For specific timestamp
                        "https://web.archive.org/web/$timestamp/http://$url"
                    }
                    
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

    private fun getFileSize(size: Long): String {
        val kb = size / 1024
        val mb = kb / 1024
        return if (mb > 0) {
            "$mb MB"
        } else {
            "$kb KB"
        }
    }
}
