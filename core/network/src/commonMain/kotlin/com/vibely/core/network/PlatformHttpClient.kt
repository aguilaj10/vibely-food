package com.vibely.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** Platform-specific engine factory. */
expect fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
