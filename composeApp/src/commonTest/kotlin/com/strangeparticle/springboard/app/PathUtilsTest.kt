package com.strangeparticle.springboard.app

import com.strangeparticle.springboard.app.platform.expandTildePath

import kotlin.test.Test
import kotlin.test.assertEquals

class PathUtilsTest {

    private val fakeHomeDirectory = "/Users/testuser"

    @Test
    fun `tilde alone expands to home directory`() {
        assertEquals(fakeHomeDirectory, expandTildePath("~", fakeHomeDirectory))
    }

    @Test
    fun `tilde prefix expands to home directory`() {
        assertEquals(
            "/Users/testuser/Desktop/file.json",
            expandTildePath("~/Desktop/file.json", fakeHomeDirectory)
        )
    }

    @Test
    fun `absolute path is returned unchanged`() {
        val absolutePath = "/Users/testuser/Desktop/file.json"
        assertEquals(absolutePath, expandTildePath(absolutePath, fakeHomeDirectory))
    }

    @Test
    fun `relative path without tilde is returned unchanged`() {
        val relativePath = "Desktop/file.json"
        assertEquals(relativePath, expandTildePath(relativePath, fakeHomeDirectory))
    }

    @Test
    fun `path with tilde not at start is returned unchanged`() {
        val path = "/some/path/with~tilde/file.json"
        assertEquals(path, expandTildePath(path, fakeHomeDirectory))
    }

    @Test
    fun `nested path under home expands correctly`() {
        assertEquals(
            "/Users/testuser/a/b/c/file.json",
            expandTildePath("~/a/b/c/file.json", fakeHomeDirectory)
        )
    }
}
