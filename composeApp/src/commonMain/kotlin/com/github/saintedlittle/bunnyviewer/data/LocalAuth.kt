package com.github.saintedlittle.bunnyviewer.data

import com.github.saintedlittle.bunnyviewer.KV
import com.github.saintedlittle.bunnyviewer.KV.observe
import kotlinx.coroutines.flow.map

@kotlinx.serialization.Serializable
data class Account(val login: String, val password: String)

object LocalAuth {
    private const val KEY_ACCOUNT = "account"
    private const val KEY_LOGGED = "logged"

    fun hasAccount(): Boolean = KV.get<Account>(KEY_ACCOUNT) != null

    fun hasAccountFlow() = observe<Account>(KEY_ACCOUNT).map { it != null }

    fun isLoggedIn(): Boolean = KV.get<Boolean>(KEY_LOGGED) ?: false
    fun isLoggedInFlow() = observe<Boolean>(KEY_LOGGED).map { it == true }

    fun setAccount(account: Account) = KV.put(KEY_ACCOUNT, account)
    fun login(account: Account) {
        val stored: Account? = KV.get(KEY_ACCOUNT)
        KV.put(KEY_LOGGED, stored == account)
    }
    fun logout() = KV.put(KEY_LOGGED, false)
}
