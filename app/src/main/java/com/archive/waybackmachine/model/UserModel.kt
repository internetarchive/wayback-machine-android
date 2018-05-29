package com.archive.waybackmachine.model

import java.io.Serializable

class UserModel(
        val username: String,
        val email: String,
        val loggedInSig: String,
        val loggedInUser: String,
        val password: String,
        val s3AccessKey: String,
        val s3SecretKey: String
): Serializable {

}