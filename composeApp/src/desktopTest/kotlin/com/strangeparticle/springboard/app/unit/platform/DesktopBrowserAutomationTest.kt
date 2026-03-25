package com.strangeparticle.springboard.app.unit.platform

import com.strangeparticle.springboard.app.platform.extractDefaultBrowserBundleId
import com.strangeparticle.springboard.app.platform.parseLaunchServicesEntries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopBrowserAutomationTest {

    @Test
    fun `prefers com_apple_default_app_web_browser content type entry`() {
        val entries = parseLaunchServicesEntries("""
            (
                    {
                    LSHandlerContentType = "com.apple.default-app.web-browser";
                    LSHandlerModificationDate = 793895068;
                    LSHandlerPreferredVersions =         {
                        LSHandlerRoleAll = "7632.117";
                    };
                    LSHandlerRoleAll = "com.google.chrome";
                },
                    {
                    LSHandlerModificationDate = 793895068;
                    LSHandlerRoleAll = "com.apple.Safari";
                    LSHandlerURLScheme = https;
                }
            )
        """.trimIndent())

        assertEquals("com.google.chrome", extractDefaultBrowserBundleId(entries))
    }

    @Test
    fun `falls back to https handler when browser content type entry is absent`() {
        val entries = parseLaunchServicesEntries("""
            (
                    {
                    LSHandlerModificationDate = 793895068;
                    LSHandlerRoleAll = "com.apple.Safari";
                    LSHandlerURLScheme = https;
                },
                    {
                    LSHandlerModificationDate = 793895068;
                    LSHandlerRoleAll = "com.google.chrome";
                    LSHandlerURLScheme = http;
                }
            )
        """.trimIndent())

        assertEquals("com.apple.Safari", extractDefaultBrowserBundleId(entries))
    }

    @Test
    fun `falls back to http handler when https handler is absent`() {
        val entries = parseLaunchServicesEntries("""
            (
                    {
                    LSHandlerModificationDate = 793895068;
                    LSHandlerRoleAll = "com.google.chrome";
                    LSHandlerURLScheme = http;
                }
            )
        """.trimIndent())

        assertEquals("com.google.chrome", extractDefaultBrowserBundleId(entries))
    }

    @Test
    fun `returns null when no browser handler entry is present`() {
        val entries = parseLaunchServicesEntries("""
            (
                    {
                    LSHandlerContentType = "public.json";
                    LSHandlerRoleAll = "com.microsoft.vscode";
                },
                    {
                    LSHandlerURLScheme = xcode;
                    LSHandlerRoleAll = "com.apple.dt.xcode";
                }
            )
        """.trimIndent())

        assertNull(extractDefaultBrowserBundleId(entries))
    }

    @Test
    fun `returns null for empty input`() {
        val entries = parseLaunchServicesEntries("")
        assertNull(extractDefaultBrowserBundleId(entries))
    }

    @Test
    fun `ignores nested LSHandlerPreferredVersions values`() {
        val entries = parseLaunchServicesEntries("""
            (
                    {
                    LSHandlerContentType = "com.apple.default-app.web-browser";
                    LSHandlerPreferredVersions =         {
                        LSHandlerRoleAll = "7632.117";
                    };
                    LSHandlerRoleAll = "com.apple.Safari";
                }
            )
        """.trimIndent())

        assertEquals("com.apple.Safari", extractDefaultBrowserBundleId(entries))
    }
}
