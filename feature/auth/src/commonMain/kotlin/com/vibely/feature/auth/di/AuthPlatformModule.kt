package com.vibely.feature.auth.di

import org.koin.core.module.Module

/** Platform-specific TokenStorage binding. Implemented per platform via expect/actual. */
expect fun authPlatformModule(): Module
