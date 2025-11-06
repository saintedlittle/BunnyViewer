package com.github.saintedlittle.bunnyviewer


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.github.saintedlittle.bunnyviewer.data.LocalAuth
import com.github.saintedlittle.bunnyviewer.ui.Nav
import com.github.saintedlittle.bunnyviewer.ui.Nav.Feed
import com.github.saintedlittle.bunnyviewer.ui.Navigation
import com.github.saintedlittle.bunnyviewer.ui.auth.LoginScreen
import com.github.saintedlittle.bunnyviewer.ui.auth.RegisterScreen
import com.github.saintedlittle.bunnyviewer.ui.feed.FeedScreen
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    ProvidePlatformDependencies {
        val nav = remember { Navigation() }
        val hasAccount by LocalAuth.hasAccountFlow().collectAsState(initial = LocalAuth.hasAccount())
        val isLoggedIn by LocalAuth.isLoggedInFlow().collectAsState(initial = LocalAuth.isLoggedIn())


        val start = remember(hasAccount, isLoggedIn) {
            when {
                !hasAccount -> Nav.Register
                !isLoggedIn -> Nav.Login
                else -> Feed()
            }
        }
        LaunchedEffect(start) { nav.replaceAll(start) }


        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
            when (nav.current) {
                Nav.Register -> RegisterScreen(
                    onDone = {
                        LocalAuth.setAccount(it)
                        nav.goTo(Nav.Login)
                    },
                    onAlreadyHave = { nav.goTo(Nav.Login) }
                )
                Nav.Login -> LoginScreen(
                    onLogin = {
                        LocalAuth.login(it)
                        nav.goTo(Feed())
                    },
                    onNoAccount = { nav.goTo(Nav.Register) }
                )
                is Feed -> FeedScreen(onOpenPost = { nav.goTo(Feed(postId = it)) })
            }
        }
    }
}