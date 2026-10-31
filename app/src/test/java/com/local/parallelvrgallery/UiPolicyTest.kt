package com.local.parallelvrgallery

import org.junit.Assert.*
import org.junit.Test

class UiPolicyTest {
    @Test fun featureUpdateIsNewerThanRestoredRelease() = assertTrue(compareVersionTags("v3.1.00", "v3.0.08") > 0)
    @Test fun patchComparisonIsNumeric() = assertTrue(compareVersionTags("v3.0.10", "v3.0.9") > 0)
    @Test fun equalVersionsIgnoreLeadingZeroes() = assertEquals(0, compareVersionTags("v3.1.00", "3.1.0"))
    @Test fun missingVersionComponentMeansZero() = assertEquals(0, compareVersionTags("3.1", "v3.1.0"))
    @Test fun oldReleaseIsNotAnUpdate() = assertTrue(compareVersionTags("v3.0.08", "v3.1.00") < 0)
    @Test fun majorVersionWinsOverLargePatch() = assertTrue(compareVersionTags("4.0.0", "3.99.99") > 0)
    @Test fun compactPhoneLimitsImageDensity() = assertEquals(3, boundedColumns(8, 360, 1f, false))
    @Test fun largeTextLeavesReadableCards() = assertEquals(1, boundedColumns(5, 320, 2f, true))
    @Test fun tabletKeepsRequestedColumnsWhenTheyFit() = assertEquals(5, boundedColumns(5, 1000, 1f, true))
    @Test fun landscapeDoesNotExceedEightColumns() = assertEquals(8, boundedColumns(20, 1400, 1f, false))
    @Test fun smallestWindowStillHasOneColumn() = assertEquals(1, boundedColumns(5, 120, 3f, false))
    @Test fun explicitSingleColumnIsRespected() = assertEquals(1, boundedColumns(1, 1400, 1f, false))
    @Test fun reducedFontScaleDoesNotShrinkTouchTargets() = assertEquals(3, boundedColumns(8, 360, 0.8f, false))
    @Test fun emptySampleHasNoCodecFlag() = assertEquals(0, muxerSampleFlags(0))
    @Test fun syncSampleMapsToKeyFrame() = assertEquals(1, muxerSampleFlags(1))
    @Test fun encryptedExtractorBitIsNotCodecConfig() = assertEquals(0, muxerSampleFlags(2))
    @Test fun partialExtractorBitIsNotEndOfStream() = assertEquals(0, muxerSampleFlags(4))
    @Test fun mixedFlagsOnlyPreserveSync() = assertEquals(1, muxerSampleFlags(7))
    @Test fun navigationRoundTripPreservesAlbumAndGeneratedPosition() {
        val state = UiNavigation(tab = "generated", album = "qa", version = "depthV6", versionIndex = 7, versionOffset = 32, generatedColumns = 3)
        assertEquals(state, UiNavigation.from(state.values()))
    }
    @Test fun corruptNavigationFallsBackToSafeLibrary() {
        val state = UiNavigation.from(mapOf("tab" to "viewer", "index" to "-5", "offset" to "bad", "allColumns" to "999"))
        assertEquals("all", state.tab); assertEquals(0, state.index); assertEquals(0, state.offset); assertEquals(8, state.allColumns)
    }
    @Test fun navigationContainsNoGenerationOrViewerActivation() {
        val keys = UiNavigation().values().keys
        assertFalse("vrMode" in keys); assertFalse("selectedIndex" in keys); assertFalse("jobs" in keys)
    }
    @Test fun matchingOriginalCertificateIsAccepted() = assertTrue(sameSigningDigests(setOf("original"), setOf("original")))
    @Test fun differentCertificateIsRejected() = assertFalse(sameSigningDigests(setOf("original"), setOf("new-system-debug")))
    @Test fun unsignedDownloadIsRejected() = assertFalse(sameSigningDigests(setOf("original"), emptySet()))
    @Test fun missingInstalledSignerDoesNotAuthorizeDownload() = assertFalse(sameSigningDigests(emptySet(), emptySet()))
    @Test fun matchingTagWithOldCodeIsRejected() = assertFalse(isNewerApkVersion("v3.1.00", 30008, "v3.0.08", 30008))
    @Test fun highCodeWithOldTagIsRejected() = assertFalse(isNewerApkVersion("v3.0.07", 99999, "v3.0.08", 30008))
    @Test fun ordinaryUpgradeIsAccepted() = assertTrue(isNewerApkVersion("v3.1.00", 30100, "v3.0.08", 30008))
}
