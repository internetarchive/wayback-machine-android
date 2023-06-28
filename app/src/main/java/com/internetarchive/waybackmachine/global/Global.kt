package com.internetarchive.waybackmachine.global

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

val Errors: Map<String, String> = mapOf(
        "account_bad_password" to "Incorrect password!",
        "account_not_found" to "Account not found",
        "account_not_verified" to "Account is not verified",
        "account_already_used" to "Account is already in use"
)

fun getFileSize(size: Long): String {
    if (size <= 0)
        return "0"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}