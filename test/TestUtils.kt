package com.vaticle.typedb.studio.test

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
    runBlocking { compose.rule() }
}

fun fileNameToString(fileName: String): String {
    return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)
        .joinToString("")
}