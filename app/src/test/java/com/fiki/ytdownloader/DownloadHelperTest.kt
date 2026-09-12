package com.fiki.ytdownloader

import com.fiki.ytdownloader.util.DownloadHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadHelperTest {

    @Test
    fun extractYouTubeId_standardWatchUrl() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", DownloadHelper.extractYouTubeId(url))
    }

    @Test
    fun extractYouTubeId_shortUrl() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", DownloadHelper.extractYouTubeId(url))
    }

    @Test
    fun extractYouTubeId_shortsUrl() {
        val url = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", DownloadHelper.extractYouTubeId(url))
    }

    @Test
    fun extractYouTubeId_embedUrl() {
        val url = "https://www.youtube.com/embed/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", DownloadHelper.extractYouTubeId(url))
    }

    @Test
    fun extractYouTubeId_urlWithExtraParameters() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share&t=10"
        assertEquals("dQw4w9WgXcQ", DownloadHelper.extractYouTubeId(url))
    }

    @Test
    fun extractYouTubeId_emptyOrInvalid() {
        assertNull(DownloadHelper.extractYouTubeId(""))
        assertNull(DownloadHelper.extractYouTubeId("   "))
        assertNull(DownloadHelper.extractYouTubeId("https://google.com"))
    }
}
