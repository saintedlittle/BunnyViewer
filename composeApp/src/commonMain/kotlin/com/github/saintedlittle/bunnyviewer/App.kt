package com.github.saintedlittle.bunnyviewer


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.github.saintedlittle.bunnyviewer.data.LocalAuth
import com.github.saintedlittle.bunnyviewer.ui.Nav
import com.github.saintedlittle.bunnyviewer.ui.Navigation
import com.github.saintedlittle.bunnyviewer.ui.auth.LoginScreen
import com.github.saintedlittle.bunnyviewer.ui.auth.RegisterScreen
import com.github.saintedlittle.bunnyviewer.ui.feed.FeedScreen


@Composable
fun App() {
    ProvidePlatformDependencies {
        val nav = remember { Navigation() }

        val hasAccount by LocalAuth.hasAccountFlow()
            .collectAsState(initial = LocalAuth.hasAccount())
        val isLoggedIn by LocalAuth.isLoggedInFlow()
            .collectAsState(initial = LocalAuth.isLoggedIn())

        // стартовый экран (Register/Login/Feed)
        val start = remember(hasAccount, isLoggedIn) {
            when {
                !hasAccount -> Nav.Register
                !isLoggedIn -> Nav.Login
                else -> Nav.Feed()
            }
        }
        LaunchedEffect(start) { nav.replaceAll(start) }

        // 🔴 ВАЖНО: подписка на backstack
        val backstack by nav.backstack.collectAsState()
        val current = backstack.lastOrNull() ?: Nav.Login

        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
            when (current) {
                Nav.Register -> RegisterScreen(
                    onDone = {
                        LocalAuth.setAccount(it)
                        nav.goTo(Nav.Login)            // можно replaceAll(Nav.Login), если не хочешь «назад»
                    },
                    onAlreadyHave = { nav.goTo(Nav.Login) }
                )
                Nav.Login -> LoginScreen(
                    onLogin = {
                        LocalAuth.login(it)
                        nav.replaceAll(Nav.Feed())      // лучше заменить стек после логина
                    },
                    onNoAccount = { nav.goTo(Nav.Register) }
                )
                is Nav.Feed -> FeedScreen(onOpenPost = { nav.goTo(Nav.Feed(postId = it)) })
            }
        }
    }
}
