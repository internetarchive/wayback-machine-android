package com.archive.waybackmachine.global

import android.content.Context
import android.provider.Settings
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import org.json.JSONObject
import java.sql.Array
import android.os.AsyncTask.execute
import com.github.kittinunf.fuel.core.Blob
import com.github.kittinunf.fuel.core.Method
import okhttp3.*
import java.io.IOException
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.io.File
import java.io.InputStream


class APIManager private constructor(context: Context?) {
    companion object : SingletonHolder<APIManager, Context?>(::APIManager)

    private val BaseURL = "https://archive.org/"
    private val API_CREATE = "services/xauthn/?op=create"
    private val API_LOGIN = "services/xauthn/?op=authenticate"
    private val API_INFO = "services/xauthn/?op=info"
    private val API_AVAILABILITY = "wayback/available"
    private val API_S3_KEY = "account/s3.php?output_json=1"
    private val UploadURL = "https://s3.us.archive.org"
    private val SparkLineURL = "https://web.archive.org/__wb/sparkline"
    private val WEB_LOGIN = "account/login.php"
    private val API_METADATA = "metadata/"
    private val API_VERSION = 1
    private val ACCESS = "trS8dVjP8dzaE296"
    private val SECRET = "ICXDO78cnzUlPAt1"
    private var HEADER: MutableMap<String, String>

    private val mContext: Context?

    init {
        mContext = context
        HEADER = mutableMapOf(
            "User-Agent" to "Wayback_Machine_Android/"
                + AppManager.getInstance(mContext).getVersionName()
        )

        FuelManager.instance.baseHeaders = HEADER
    }

    private fun SendDataToService(params: List<Pair<String, Any>>,
                                  op: String,
                                  completion: (Boolean, JSONObject?, Int?) -> Unit) {
        var parameters: MutableList<Pair<String, Any>> = mutableListOf()
        parameters.addAll(params)
        parameters.add("access" to ACCESS)
        parameters.add("secret" to SECRET)
        parameters.add("version" to API_VERSION)

        Fuel.post(BaseURL + op, parameters).responseJson{_, response, result ->
            when (result) {
                is Result.Failure -> {
                    completion(false, null, response.statusCode)
                }
                is Result.Success -> {
                    completion(true, result.get().obj(), response.statusCode)
                }
            }
        }

    }

    private fun SendDataToSparkLine(params: List<Pair<String, Any>>,
                                    completion: (String?) -> Unit) {

    }

    fun uploadFile(params: Map<String, String>,
                                 completion: (Boolean, String?, String?, String?) -> Unit) {

        val authorization = "LOW " + params["s3accesskey"] + ":" + params["s3secretkey"]
        val url = UploadURL + "/" + params["identifier"] + "/" + params["filename"]

        /*************************/
//        val file = File(params["path"])
//        val client = OkHttpClient()
//        val requestBody =MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("X-File-Name", params["filename"]!!,
//                        RequestBody.create(MediaType.parse("multipart/form-data"), file))
//                .build()
//
//        val request = Request.Builder()
//                .url(url)
//                .addHeader("X-File-Name", params["filename"]!!)
//                .addHeader("x-amz-acl", "bucket-owner-full-control")
//                .addHeader("x-amz-auto-make-bucket", "1")
//                .addHeader("x-archive-meta-collection", "opensource_media")
//                .addHeader("x-archive-meta-mediatype", params["mediatype"]!!)
//                .addHeader("x-archive-meta-title", params["title"]!!)
//                .addHeader("x-archive-meta-description", params["description"]!!)
//                .addHeader("x-archive-meta-subject", params["tags"]!!)
//                .addHeader("authorization", authorization)
//                .put(requestBody)
//                .build()
//
//        client.newCall(request).enqueue(object: Callback{
//            override fun onResponse(call: Call?, response: Response?) {
//                if (response != null) {
//                    print(response.body()?.string())
//                } else {
//                    completion(false, null, null, call.toString())
//                }
//
//            }
//
//            override fun onFailure(call: Call?, e: IOException?) {
//                completion(false, null, null, e?.hashCode().toString())
//            }
//        })

        /*************************/
        HEADER["X-File-Name"] = params["filename"]!!
        HEADER["x-amz-acl"] = "bucket-owner-full-control"
        HEADER["x-amz-auto-make-bucket"] = "1"
        HEADER["x-archive-meta-collection"] = "opensource_media"
        HEADER["x-archive-meta-mediatype"] = params["mediatype"]!!
        HEADER["x-archive-meta-title"] = params["title"]!!
        HEADER["x-archive-meta-description"] = params["description"]!!
        HEADER["x-archive-meta-subject"] = params["tags"]!!
        HEADER["authorization"] = authorization

        FuelManager.instance.baseHeaders = HEADER

        val file = File(params["path"])
        val blob = Blob(params["filename"]!!, file.length(), {file.inputStream()})

        Fuel.upload(url, Method.PUT).source{ request, url ->
            // here end - test58
            file
        }.interrupt {request ->
            print(request.headers)
        }.progress { readBytes, totalBytes ->
            println("ReadBytes:")
            println(readBytes)
            println("totalBytes:")
            println(totalBytes)
        }.responseString {request, response, result ->
            when (result) {
                is Result.Failure -> {
                    completion(false, null, null, response.hashCode().toString())
                }
                is Result.Success -> {
                    completion(true, getFileSize(file.length()), url, null)
                }
            }
        }

    }

    // Register new Account
    fun registerAccount(email: String,
                        password: String,
                        username: String,
                        completion: (Boolean, String?) -> Unit) {
        SendDataToService(listOf(
            "screenname" to username,
            "email" to email,
            "password" to password,
            "verified" to false
        ), API_CREATE, {success, data, errCode ->
            if (success) {
                val register_success = data?.getBoolean("success")
                if (register_success != null) {
                    if (register_success) {
                        completion(true, null)
                    } else {
                        completion(false, Errors["account_already_used"])
                    }
                } else {
                    completion(false, errCode?.toString())
                }
            } else {
                completion(false, errCode?.toString())
            }
        })
    }

    // Login
    fun login(email: String,
              password: String,
              completion: (Boolean, String?) -> Unit) {

        SendDataToService(listOf(
            "email" to email,
            "password" to password
        ), API_LOGIN, {success, data, errCode ->
            if (success) {
                val login_success = data?.getBoolean("success")

                if (login_success != null) {
                    if (login_success) {
                        completion(true, null)
                    } else {
                        val values = data?.getJSONObject("values")
                        val reason = values?.getString("reason")
                        completion(false, Errors[reason])
                    }
                } else {
                    completion(false, errCode.toString())
                }
            } else {
                completion(false, errCode.toString())
            }
        })

    }

    // Get Account Info
    fun getUsername(email: String,
                       completion: (Boolean, String?, Int?) -> Unit) {
        SendDataToService(listOf(
            "email" to email
        ), API_INFO, {success, data, errCode ->
            if (success) {
                val values = data?.getJSONObject("values")
                val username = values?.getString("screenname")
                completion(true, username, null)
            } else {
                completion(false, null, errCode)
            }
        })
    }

    // Get IAS3Key
    fun getIAS3Keys(loggedInSig: String, loggedInUser:String, completion: (Boolean, String?, String?, Int?) -> Unit) {
        val client = OkHttpClient().newBuilder()
                .cookieJar(object: CookieJar{
                    override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>?) {
                    }
                    override fun loadForRequest(url: HttpUrl?): MutableList<Cookie> {
                        val cookies = ArrayList<Cookie>(3)
                        cookies.add(Cookie.Builder()
                                .domain("archive.org")
                                .path("/")
                                .name("test-cookie")
                                .value("1")
                                .build())
                        cookies.add(Cookie.Builder()
                                .domain("archive.org")
                                .path("/")
                                .name("logged-in-sig")
                                .value(loggedInSig)
                                .build())
                        cookies.add(Cookie.Builder()
                                .domain("archive.org")
                                .path("/")
                                .name("logged-in-user")
                                .value(loggedInUser)
                                .build())
                        return cookies
                    }
                }).build()

        val request = Request.Builder()
                .url(BaseURL + API_S3_KEY)
                .get()
                .addHeader("cache-control", "no-cache")
                .build()

        client.newCall(request).enqueue(object: Callback{
            override fun onResponse(call: Call?, response: Response?) {
                if (response != null) {
                    val jsonData = response.body()?.string()
                    val json = JSONObject(jsonData)
                    val keys = json.getJSONObject("key")
                    val accessKey = keys.getString("s3accesskey")
                    val secretKey = keys.getString("s3secretkey")
                    completion(true, accessKey, secretKey, null)
                } else {
                    completion(false, null, null, call?.hashCode())
                }

            }

            override fun onFailure(call: Call?, e: IOException?) {
                completion(false, null, null, e?.hashCode())
            }
        })
    }

    // Get Cookie Data
    fun getCookieData(email: String,
                      password: String,
                      completion: (Boolean, String?, String?, Int?) -> Unit) {

        val client = OkHttpClient().newBuilder()
                .cookieJar(object: CookieJar{
                    override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>?) {
                    }
                    override fun loadForRequest(url: HttpUrl?): MutableList<Cookie> {
                        val cookies = ArrayList<Cookie>(1)
                        cookies.add(Cookie.Builder()
                                .domain("archive.org")
                                .path("/")
                                .name("test-cookie")
                                .value("1")
                                .build())
                        return cookies
                    }
                }).build()

        val mediaType = MediaType.parse("application/x-www-form-urlencoded")
        val body = RequestBody.create(mediaType, "username=$email&password=$password&action=login")
        val request = Request.Builder()
                .url(BaseURL + WEB_LOGIN)
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build()

        client.newCall(request).enqueue(object: Callback{
            override fun onResponse(call: Call?, response: Response?) {
                if (response != null && response?.priorResponse() != null) {
                    val cookies = response.priorResponse()?.headers("Set-Cookie")
                    val loggedInSig = cookies!![1].split(";")[0].split("=")[1]
                    val loggedInUser = email
                    completion(true, loggedInSig, loggedInUser, null)
                } else {
                    completion(false, null, null, call?.hashCode())
                }

            }

            override fun onFailure(call: Call?, e: IOException?) {
                completion(false, null, null, e?.hashCode())
            }
        })
    }

    // Check if a URL is Blocked
    fun isURLBlocked(url: String,
                     completion: (String?) -> Unit) {

    }

    fun checkPlaybackAvailability(url: String, timestamp: String, completion: (Boolean, String?) -> Unit) {
        var params = mutableListOf(
                "url" to url
        )

        if (!timestamp.isEmpty()) {
            params.add("timestamp" to timestamp)
        }

        Fuel.get(BaseURL + API_AVAILABILITY, params).responseJson {_, _, data ->
            val json = data.component1()?.obj()

            if (json != null) {
                try {
                    val archivedSnapshots = json.getJSONObject("archived_snapshots")
                    val closest = archivedSnapshots.getJSONObject("closest")
                    val available = closest.getBoolean("available")
                    val status = closest.getString("status")
                    val waybackURL = closest.getString("url")

                    if (archivedSnapshots != null &&
                            closest != null &&
                            available != null &&
                            status == "200") {
                        completion(true, waybackURL)
                    } else {
                        completion(false, null)
                    }
                } catch (err: Exception) {
                    completion(false, null)
                }
            }
        }
    }

}
