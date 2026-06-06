package com.strangeparticle.springboard.app.shared

// On wasmJs, IgnoreOnWasm aliases to kotlin.test.Ignore so annotated tests are skipped.
// See the expect declaration in commonTest for why these tests cannot run on wasmJs.
actual typealias IgnoreOnWasm = kotlin.test.Ignore
