package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageDiskCacheTest {

    // imageCacheFileName

    @Test
    fun aNameIsTheUrlsSha256InLowercaseHex() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", imageCacheFileName("abc"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", imageCacheFileName(""))
    }

    @Test
    fun urlsGetTheirOwnNames() {
        val name = imageCacheFileName("https://picsum.photos/seed/a/800/600")
        assertEquals(name, imageCacheFileName("https://picsum.photos/seed/a/800/600"))
        assertNotEquals(name, imageCacheFileName("https://picsum.photos/seed/b/800/600"))
        assertTrue(isImageCacheFileName(name))
    }

    @Test
    fun onlyStoredBodiesCountAsEntries() {
        val name = imageCacheFileName("https://example.com/a.jpg")
        assertFalse(isImageCacheFileName("${name}123456.tmp"))
        assertFalse(isImageCacheFileName(name.uppercase()))
        assertFalse(isImageCacheFileName(name.dropLast(1)))
        assertFalse(isImageCacheFileName("journal"))
    }

    // imageCacheEvictions

    @Test
    fun nothingIsDeletedWhileTheFilesFit() {
        val files = listOf(ImageCacheFile("a", 40, 1), ImageCacheFile("b", 60, 2))
        assertEquals(emptyList<ImageCacheFile>(), imageCacheEvictions(files, maxBytes = 100))
        assertEquals(emptyList<ImageCacheFile>(), imageCacheEvictions(emptyList(), maxBytes = 0))
    }

    @Test
    fun theLeastRecentlyUsedGoFirstUntilTheRestFit() {
        val newest = ImageCacheFile("a", 10, lastUsed = 3)
        val oldest = ImageCacheFile("b", 10, lastUsed = 1)
        val middle = ImageCacheFile("c", 10, lastUsed = 2)
        assertEquals(listOf(oldest, middle), imageCacheEvictions(listOf(newest, oldest, middle), maxBytes = 15))
        assertEquals(listOf(oldest), imageCacheEvictions(listOf(newest, oldest, middle), maxBytes = 20))
    }

    @Test
    fun aFileReadLatelyOutlivesBiggerOlderOnes() {
        val read = ImageCacheFile("a", 30, lastUsed = 9)
        val big = ImageCacheFile("b", 50, lastUsed = 4)
        val small = ImageCacheFile("c", 5, lastUsed = 5)
        assertEquals(listOf(big), imageCacheEvictions(listOf(read, big, small), maxBytes = 40))
    }

    @Test
    fun tiesAreBrokenByName() {
        val files = listOf(ImageCacheFile("b", 10, 1), ImageCacheFile("a", 10, 1), ImageCacheFile("c", 10, 1))
        assertEquals(listOf("a"), imageCacheEvictions(files, maxBytes = 20).map { it.name })
    }
}
