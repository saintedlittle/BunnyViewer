package com.github.saintedlittle.bunnyviewer.data

import com.github.saintedlittle.bunnyviewer.KV
import com.github.saintedlittle.bunnyviewer.KV.observe
import com.github.saintedlittle.bunnyviewer.deserialize
import com.github.saintedlittle.bunnyviewer.serialize
import kotlinx.coroutines.flow.map

@kotlinx.serialization.Serializable
data class Account(val login: String, val password: String)

object LocalAuth {
    private const val KEY_ACCOUNT = "account"
    private const val KEY_LOGGED = "logged"

    fun hasAccount(): Boolean = KV.get<String>(KEY_ACCOUNT) != null
    fun hasAccountFlow() = KV.observe<String>(KEY_ACCOUNT).map { it != null }

    fun isLoggedIn(): Boolean = KV.get<String>(KEY_LOGGED)?.toBoolean() ?: false
    fun isLoggedInFlow() = KV.observe<String>(KEY_LOGGED).map { it?.toBoolean() == true }

    fun setAccount(account: Account) =
        KV.put(KEY_ACCOUNT, serialize(account))          // сериализуем здесь

    fun login(account: Account) {
        val storedJson: String? = KV.get(KEY_ACCOUNT)
        val stored: Account? = storedJson?.let { deserialize<Account>(it) }
        KV.put(KEY_LOGGED, (stored == account).toString()) // пишем "true"/"false"
    }

    fun logout() = KV.put(KEY_LOGGED, "false")
}

