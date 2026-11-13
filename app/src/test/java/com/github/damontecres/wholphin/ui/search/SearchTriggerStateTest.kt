package com.github.damontecres.wholphin.ui.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTriggerStateTest {
    @Test
    fun emptyQueryDoesNotShowPendingPrompt() {
        assertFalse(shouldShowPendingPrompt("", null))
        assertFalse(shouldShowPendingPrompt("   ", null))
    }

    @Test
    fun typingWithoutSubmitShowsPendingPrompt() {
        assertTrue(shouldShowPendingPrompt("rispo", null))
    }

    @Test
    fun submittedQueryHidesPendingPrompt() {
        assertFalse(shouldShowPendingPrompt("risposata", "risposata"))
        assertFalse(shouldShowPendingPrompt(" risposata ", "risposata"))
    }

    @Test
    fun editingSubmittedQueryShowsPendingPromptAgain() {
        assertTrue(shouldShowPendingPrompt("risposata 2", "risposata"))
    }
}
