package com.vibely.shared.di

import org.koin.core.module.Module

/** Returns the Koin [Module] for platform-specific dependency bindings. */
expect fun platformModule(): Module
