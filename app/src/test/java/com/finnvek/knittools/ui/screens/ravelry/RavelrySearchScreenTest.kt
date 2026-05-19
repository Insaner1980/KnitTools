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
                isCurrentSubmittedSearch = true,
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
                isCurrentSubmittedSearch = true,
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
                isCurrentSubmittedSearch = true,
            ),
        )

        assertFalse(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 12,
                isLoading = false,
                hasError = true,
                isCurrentSubmittedSearch = true,
            ),
        )
    }

    @Test
    fun loadMoreNotRequestedWhenDraftQueryDoesNotMatchSubmittedSearch() {
        assertFalse(
            shouldRequestRavelryLoadMore(
                shouldLoadMore = true,
                resultCount = 12,
                isLoading = false,
                hasError = false,
                isCurrentSubmittedSearch = false,
            ),
        )
    }

    @Test
    fun emptyStateOnlyShownForSubmittedCurrentQuery() {
        assertFalse(
            shouldShowRavelryEmptyState(
                isLoading = false,
                hasError = false,
                resultCount = 0,
                searchQuery = "socks",
                submittedQuery = "",
                hasSubmittedSearch = false,
            ),
        )

        assertFalse(
            shouldShowRavelryEmptyState(
                isLoading = false,
                hasError = false,
                resultCount = 0,
                searchQuery = "hat",
                submittedQuery = "socks",
                hasSubmittedSearch = true,
            ),
        )

        assertTrue(
            shouldShowRavelryEmptyState(
                isLoading = false,
                hasError = false,
                resultCount = 0,
                searchQuery = "socks",
                submittedQuery = "socks",
                hasSubmittedSearch = true,
            ),
        )
    }
}
