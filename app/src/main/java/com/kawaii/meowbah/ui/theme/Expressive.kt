package com.kawaii.meowbah.ui.theme

import kotlin.RequiresOptIn // Changed import

/**
 * Indicates that a Material 3 API related to expressive styling is experimental and is likely
 * to change or to be removed in the future.
 */
@RequiresOptIn(message = "This material3 expressive API is experimental and is likely to change or to be removed in the future.")
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalMaterial3ExpressiveApi
