package com.finnvek.knittools.ui.screens.ravelry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelrySearchScreenTest {
    @Test
    fun loadMoreRequestedOnlyNearEndWithReadyResults() {
        assertTrue(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 12,
                isLoading = false,
                hasError = false,
            ),
        )
    }

    @Test
    fun loadMoreNotRequestedBeforeResultsAreReady() {
        assertFalse(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 0,
                isLoading = false,
                hasError = false,
            ),
        )
    }

    @Test
    fun loadMoreNotRequestedWhileLoadingOrErrored() {
        assertFalse(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 12,
                isLoading = true,
                hasError = false,
            ),
        )

        assertFalse(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 12,
                isLoading = false,
                hasError = true,
            ),
        )
    }
}
