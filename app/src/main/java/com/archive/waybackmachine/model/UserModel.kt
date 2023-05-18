package com.archive.waybackmachine.model

import java.io.Serializable

class UserModel(
        val username: String? = null,
        val email: String? = null,
        val loggedInSig: String? = null,
        val loggedInUser: String? = null,
        val password: String? = null,
        val s3AccessKey: String? = null,
        val s3SecretKey: String? = null
): Serializable {

}