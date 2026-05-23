package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.platform.S3GetResult
import com.strangeparticle.springboard.app.platform.S3PutResult
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.S3ContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SaveResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpringboardViewModelS3Test {

    private val s3Url = "https://b.s3.us-east-1.amazonaws.com/sb.json"

    private fun createViewModel(
        fileService: PlatformFileContentServiceInMemoryFake = PlatformFileContentServiceInMemoryFake(),
        s3Service: S3ContentServiceInMemoryFake = S3ContentServiceInMemoryFake(),
    ) = SpringboardViewModel(
        settingsManager = createSettingsManagerForTest(),
        persistenceService = PersistenceServiceInMemoryFake(),
        platformActivationService = PlatformActivationServiceInMemoryFake(),
        fileContentService = fileService,
        s3ContentService = s3Service,
    )

    @Test
    fun `loadConfigFromS3 success records s3 association and etag`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"etag-1\"")
        }
        val vm = createViewModel(s3Service = s3)
        val result = vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        assertTrue(result is SpringboardViewModel.LoadResult.Success)
        val tab = vm.activeTab
        assertNotNull(tab)
        assertEquals(s3Url, tab.source)
        assertEquals("dev", tab.s3AwsProfile)
        assertEquals("\"etag-1\"", tab.s3LastEtag)
    }

    @Test
    fun `canSaveActiveTabInPlace is true for an S3-backed tab`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"e\"")
        }
        val vm = createViewModel(s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        assertTrue(vm.canSaveActiveTabInPlace)
    }

    @Test
    fun `saveTab on S3 tab PUTs with If-Match and updates etag on success`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"prior\"")
            nextEtag = "\"new-etag\""
        }
        val vm = createViewModel(s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        vm.markActiveTabDirty()

        val result = vm.saveActiveTab()

        assertEquals(SaveResult.Success(s3Url), result)
        assertEquals(1, s3.putCalls.size)
        assertEquals("\"prior\"", s3.putCalls.first().ifMatch)
        assertEquals("\"new-etag\"", vm.activeTab?.s3LastEtag)
        assertEquals(false, vm.activeTab?.isDirty)
    }

    @Test
    fun `saveTab on S3 tab returns Conflict on 412`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"first\"")
        }
        val vm = createViewModel(s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        // Simulate external modification.
        s3.objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
            S3ContentServiceInMemoryFake.Stored("changed externally", "\"second\"")
        vm.markActiveTabDirty()

        val result = vm.saveActiveTab()
        assertTrue(result is SaveResult.Conflict)
        assertEquals(s3Url, (result as SaveResult.Conflict).sourceUrl)
    }

    @Test
    fun `saveTabOverwriting on S3 tab PUTs without If-Match`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"first\"")
        }
        val vm = createViewModel(s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        s3.objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
            S3ContentServiceInMemoryFake.Stored("changed", "\"second\"")
        vm.markActiveTabDirty()

        val result = vm.saveTabOverwriting(vm.activeTabId)
        assertTrue(result is SaveResult.Success)
        assertEquals(null, s3.putCalls.last().ifMatch)
    }

    @Test
    fun `saveTab on S3 tab returns Denied on 403`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"e\"")
            putOverrides[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3PutResult.Denied("Read-only profile")
        }
        val vm = createViewModel(s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        vm.markActiveTabDirty()

        val result = vm.saveActiveTab()
        assertTrue(result is SaveResult.Denied)
        assertEquals("Read-only profile", (result as SaveResult.Denied).message)
    }

    @Test
    fun `saveActiveTabAs clears S3 association when source is rewritten`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            objects[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3ContentServiceInMemoryFake.Stored(TestFixtureJson.URL_ONLY, "\"e\"")
        }
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = createViewModel(fileService = fileService, s3Service = s3)
        vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        vm.markActiveTabDirty()

        val result = vm.saveActiveTabAs("/local/copy.json")
        assertEquals(SaveResult.Success("/local/copy.json"), result)
        assertEquals("/local/copy.json", vm.activeTab?.source)
        assertNull(vm.activeTab?.s3AwsProfile)
        assertNull(vm.activeTab?.s3LastEtag)
    }

    @Test
    fun `loadConfigFromS3 surfaces CredentialsUnavailable as Failure`() = runTest {
        val s3 = S3ContentServiceInMemoryFake().apply {
            getOverrides[S3ContentServiceInMemoryFake.Key(s3Url, "dev")] =
                S3GetResult.CredentialsUnavailable("expired")
        }
        val vm = createViewModel(s3Service = s3)
        val result = vm.loadConfigFromS3(s3Url, "dev", inNewTab = false)
        assertTrue(result is SpringboardViewModel.LoadResult.Failure)
        assertEquals("s3_credentials_unavailable", (result as SpringboardViewModel.LoadResult.Failure).code)
    }
}
