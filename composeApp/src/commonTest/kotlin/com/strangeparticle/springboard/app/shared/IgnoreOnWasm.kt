package com.strangeparticle.springboard.app.shared

// Marks a test that must not run on the wasmJs target.
//
// Some tests drive a live ktor request through the Compose UI under runComposeUiTest.
// On wasmJs the ktor engine delivers responses via JS Promise microtasks, which the
// Compose UI-test virtual clock never pumps, so the request never completes and the
// test's waitUntil(...) blocks until it times out. The same tests run deterministically
// on desktop (jvm), where the request completes on real threads.
//
// @OptionalExpectation means no actual is required on platforms where the annotation
// should have no effect: on desktop it is simply absent (the test runs normally), while
// wasmJsTest provides an actual that aliases this to kotlin.test.Ignore.
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
expect annotation class IgnoreOnWasm()
