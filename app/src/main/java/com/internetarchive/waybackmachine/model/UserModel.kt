package com.internetarchive.waybackmachine.model

import java.io.Serializable

class UserModel(
        username: String,
        email: String,
        loggedInSig: String,
        loggedInUser: String,
        password: String,
        s3AccessKey: String,
        s3SecretKey: String
): Serializable {
    val username: String = username
    val email: String = email
    val loggedInSig: String = loggedInSig
    val loggedInUser: String = loggedInUser
    val password: String = password
    val s3AccessKey: String = s3AccessKey
    val s3SecretKey: String = s3SecretKey
}