package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.persistence.TabDto
import com.strangeparticle.springboard.app.platform.S3ContentService
import com.strangeparticle.springboard.app.platform.S3GetResult
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class TabRestorer(
    private val persistenceService: PersistenceService,
    private val loader: SpringboardContentLoader,
    private val s3ContentService: S3ContentService? = null,
    private val reportError: (String) -> Unit = { ToastBroadcaster.error(it) },
) {

    /**
     * Restores tabs from the resolved [tabSources] list (already resolved by the
     * settings precedence chain). Zoom levels and S3 association are read from
     * the separate tab persistence layer when the source matches.
     *
     * S3-backed tabs (persisted with a non-null `s3AwsProfile`) are fetched via
     * the authenticated [S3ContentService] using the stored profile. All other
     * tabs go through the regular [SpringboardContentLoader].
     */
    suspend fun restoreInto(viewModel: SpringboardViewModel, tabSources: List<String>) {
        if (tabSources.isEmpty()) return

        val persistedTabs = persistenceService.loadTabs()
        val persistedBySource: Map<String, TabDto> = persistedTabs?.tabs
            ?.filter { it.source != null }
            ?.associateBy { it.source!! }
            ?: emptyMap()

        viewModel.runSuppressingAutosave {
            for (source in tabSources) {
                if (!viewModel.canCreateNewTab && !isReusableFirstEmptyTab(viewModel)) {
                    reportError("Skipped restoring tab for '$source': maximum of $MAX_OPEN_TABS tabs reached")
                    continue
                }
                val persisted = persistedBySource[source]
                val s3Profile = persisted?.s3AwsProfile
                val loadOutcome = if (s3Profile != null) {
                    loadFromS3(source, s3Profile)
                } else {
                    loadFromLoader(source)
                }
                when (loadOutcome) {
                    is RestoreLoad.Failed -> {
                        reportError("Failed to restore tab for '$source': ${loadOutcome.message}")
                        continue
                    }
                    is RestoreLoad.Loaded -> {
                        val zoomPercent = persisted?.zoomPercent ?: 100
                        viewModel.restoreTabFromPersistence(
                            source = source,
                            jsonContents = loadOutcome.contents,
                            zoomPercent = zoomPercent,
                            s3AwsProfile = s3Profile,
                            s3LastEtag = loadOutcome.etag ?: persisted?.s3LastEtag,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadFromLoader(source: String): RestoreLoad {
        return try {
            RestoreLoad.Loaded(loader.loadContent(source), etag = null)
        } catch (e: Exception) {
            RestoreLoad.Failed(e.message ?: "unknown error")
        }
    }

    private suspend fun loadFromS3(source: String, profile: String): RestoreLoad {
        val service = s3ContentService
            ?: return RestoreLoad.Failed("S3 content service is not available")
        return when (val result = service.getObject(source, profile)) {
            is S3GetResult.Success -> RestoreLoad.Loaded(result.content, etag = result.etag)
            is S3GetResult.Denied -> RestoreLoad.Failed(result.message)
            is S3GetResult.CredentialsUnavailable -> RestoreLoad.Failed(result.message)
            is S3GetResult.Failed -> RestoreLoad.Failed(result.message)
        }
    }

    private fun isReusableFirstEmptyTab(viewModel: SpringboardViewModel): Boolean {
        return viewModel.tabs.size == 1 && viewModel.tabs.first().isEmpty
    }

    private sealed class RestoreLoad {
        data class Loaded(val contents: String, val etag: String?) : RestoreLoad()
        data class Failed(val message: String) : RestoreLoad()
    }
}
