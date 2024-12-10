package com.example.messenger


class User {
    var uid: String? = null
    var login: String? = null
    var password: String? = null
    var surname: String? = null
    var name: String? = null
    var username: String? = null
    var avatar: String? = null

    constructor(
        uid: String?,
        login: String?,
        password: String?,
        surname: String?,
        name: String?,
        username: String?,
        avatar: String?
    ) {
        this.uid = uid
        this.login = login
        this.password = password
        this.surname = surname
        this.name = name
        this.username = username
        this.avatar = avatar
    }
    constructor()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "login" to login,
            "password" to password,
            "surname" to surname,
            "name" to name,
            "username" to username,
            "avatar" to avatar
        )
    }
}
