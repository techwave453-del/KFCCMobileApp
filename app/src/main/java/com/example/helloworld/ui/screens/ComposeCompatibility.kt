package com.example.helloworld.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Lightweight fallback for screens that only need in-composition state retention. */
@Composable
fun <T> rememberSaveable(calculation: () -> T): T = remember(calculation)
