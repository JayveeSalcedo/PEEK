package com.peek.app.features.preview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.peek.app.data.model.SampleConversation
import com.peek.app.ui.theme.PeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeekPreviewCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsConversationAndCloses() {
        var closed = 0
        val conversation = SampleConversation.create()
        compose.setContent {
            PeekTheme { PeekPreview(conversation, onOpenApp = null, onClose = { closed++ }) }
        }

        compose.onNodeWithText(conversation.title).assertIsDisplayed()
        compose.onNodeWithText(conversation.messages.last().text).assertIsDisplayed()
        compose.onNodeWithText(NOTIFICATION_ONLY_NOTE).assertIsDisplayed()

        compose.onNodeWithText("Close").performClick()
        assertEquals(1, closed)
    }
}
