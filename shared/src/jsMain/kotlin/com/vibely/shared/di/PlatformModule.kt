package com.vibely.shared.di

import org.koin.dsl.module

/** JS [platformModule]: no additional platform bindings required for the web target. */
actual fun platformModule() = module {}
