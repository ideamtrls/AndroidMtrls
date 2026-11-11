package com.ideamtrls.androidmtrls

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class ImagePipelineTest {

    // decodeSize

    @Test
    fun coverScalesUntilTheShortSideFillsTheFrame() {
        assertEquals(IntSize(450, 300), decodeSize(IntSize(1500, 1000), IntSize(300, 300), ImageFit.Cover))
        assertEquals(IntSize(300, 600), decodeSize(IntSize(1000, 2000), IntSize(300, 300), ImageFit.Cover))
    }

    @Test
    fun containScalesUntilTheLongSideFitsTheFrame() {
        assertEquals(IntSize(300, 200), decodeSize(IntSize(1500, 1000), IntSize(300, 300), ImageFit.Contain))
    }

    @Test
    fun coverLandsOnTheFrameDespiteRounding() {
        // 667 × (300 / 667) is a hair off 300 in floating point.
        assertEquals(300, decodeSize(IntSize(1000, 667), IntSize(300, 300), ImageFit.Cover).height)
    }

    @Test
    fun neverDecodesLargerThanTheSource() {
        assertEquals(IntSize(200, 100), decodeSize(IntSize(200, 100), IntSize(800, 800), ImageFit.Cover))
        assertEquals(IntSize(200, 100), decodeSize(IntSize(200, 100), IntSize(0, 0), ImageFit.Cover))
    }

    // ImageMemoryCache

    @Test
    fun keysIncludeTheFrame() {
        val cache = ImageMemoryCache<String>(1000) { it.length }
        cache.put(ImageKey("a", 100, 100, ImageFit.Cover), "small")
        assertEquals("small", cache[ImageKey("a", 100, 100, ImageFit.Cover)])
        assertNull(cache[ImageKey("a", 200, 200, ImageFit.Cover)])
        assertNull(cache[ImageKey("a", 100, 100, ImageFit.Contain)])
    }

    @Test
    fun latestIsTheMostRecentDecodeOfAUrl() {
        val cache = ImageMemoryCache<String>(1000) { it.length }
        cache.put(ImageKey("a", 100, 100, ImageFit.Cover), "small")
        cache.put(ImageKey("a", 400, 400, ImageFit.Cover), "large")
        assertEquals("large", cache.latest("a"))
        assertNull(cache.latest("b"))
    }

    @Test
    fun evictingTheLatestDecodeForgetsIt() {
        val cache = ImageMemoryCache<String>(10) { it.length }
        cache.put(ImageKey("a", 1, 1, ImageFit.Cover), "aaaaaa")
        cache.put(ImageKey("b", 1, 1, ImageFit.Cover), "bbbbbb") // evicts a
        assertNull(cache.latest("a"))
        assertEquals("bbbbbb", cache.latest("b"))
    }

    // InFlightLoads

    @Test
    fun callersOfOneKeyShareOneLoad() = runBlocking {
        val loads = InFlightLoads<String, String>(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val runs = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val block: suspend () -> String = { runs.incrementAndGet(); gate.await(); "done" }
        val first = async(start = CoroutineStart.UNDISPATCHED) { loads.load("a", block) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { loads.load("a", block) }
        gate.complete(Unit)
        assertEquals("done", first.await())
        assertEquals("done", second.await())
        assertEquals(1, runs.get())
    }

    @Test
    fun theLoadIsCancelledOnlyWhenEveryCallerLeaves() = runBlocking {
        val loads = InFlightLoads<String, String>(CoroutineScope(SupervisorJob() + Dispatchers.Default))

        // One of two callers leaves: the load goes on for the other.
        val gate = CompletableDeferred<Unit>()
        val shared: suspend () -> String = { gate.await(); "done" }
        val leaving = async(start = CoroutineStart.UNDISPATCHED) { loads.load("a", shared) }
        val staying = async(start = CoroutineStart.UNDISPATCHED) { loads.load("a", shared) }
        leaving.cancel()
        leaving.join()
        gate.complete(Unit)
        assertEquals("done", staying.await())

        // The only caller leaves: the load is cancelled.
        val cancelled = CompletableDeferred<Unit>()
        val lone = async(start = CoroutineStart.UNDISPATCHED) {
            loads.load("b") {
                try {
                    CompletableDeferred<String>().await()
                } catch (e: CancellationException) {
                    cancelled.complete(Unit)
                    throw e
                }
            }
        }
        lone.cancel()
        lone.join()
        withTimeout(5_000) { cancelled.await() }
    }

    @Test
    fun aFailureIsRetriedByTheNextCall() = runBlocking {
        val loads = InFlightLoads<String, String>(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val runs = AtomicInteger()
        val failed = runCatching { loads.load("a") { runs.incrementAndGet(); throw IOException("offline") } }
        assertTrue(failed.exceptionOrNull() is IOException)
        assertEquals("done", loads.load("a") { runs.incrementAndGet(); "done" })
        assertEquals(2, runs.get())
    }
}
