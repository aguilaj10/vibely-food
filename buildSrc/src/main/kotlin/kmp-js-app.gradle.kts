// KMP JS-browser application convention: applies kotlin.multiplatform for the JS/IR target.
// Used by app-web and any future browser-facing JS entry-point modules.
// Applying id() without version here is intentional — the plugin is already on the
// buildSrc classpath; alias() would cause a "already on classpath" version conflict.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}
