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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

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

    // decodeSize's caps: 16 megapixels, 8192 on a side

    @Test
    fun anExtremeAspectIsCappedEitherWay() {
        // Covering a square scales up, so the frame rule alone would decode the whole source.
        assertEquals(IntSize(8192, 1), decodeSize(IntSize(1_000_000, 100), IntSize(300, 300), ImageFit.Cover))
        assertEquals(IntSize(1, 8192), decodeSize(IntSize(100, 1_000_000), IntSize(300, 300), ImageFit.Cover))
        // Scaled to the frame, but still past the side cap.
        assertEquals(IntSize(8192, 204), decodeSize(IntSize(40_000, 1000), IntSize(300, 300), ImageFit.Cover))
    }

    @Test
    fun aHugeSquareIsCappedWhateverTheFit() {
        for (fit in ImageFit.entries) {
            assertEquals(IntSize(4000, 4000), decodeSize(IntSize(20_000, 20_000), IntSize(30_000, 30_000), fit))
        }
    }

    @Test
    fun aZeroTargetIsCappedToo() {
        assertEquals(IntSize(4000, 4000), decodeSize(IntSize(20_000, 20_000), IntSize.Zero, ImageFit.Cover))
        assertEquals(IntSize(8192, 1), decodeSize(IntSize(1_000_000, 100), IntSize.Zero, ImageFit.Contain))
    }

    @Test
    fun sizesWithinTheCapsAreUnchanged() {
        assertEquals(IntSize(4000, 3000), decodeSize(IntSize(4000, 3000), IntSize.Zero, ImageFit.Cover))
        assertEquals(IntSize(8192, 1953), decodeSize(IntSize(8192, 1953), IntSize(9000, 9000), ImageFit.Contain))
        // Containing the extreme picture in a square is already small; the frame rule decides.
        assertEquals(IntSize(300, 1), decodeSize(IntSize(1_000_000, 100), IntSize(300, 300), ImageFit.Contain))
    }

    @Test
    fun theCapsHoldForEverySourceFrameAndFit() {
        val lengths = listOf(1, 7, 640, 1000, 4097, 8191, 8192, 8193, 12_345, 99_991, 1_000_000)
        val targets = listOf(IntSize.Zero, IntSize(1, 1), IntSize(300, 300), IntSize(1080, 2400), IntSize(50_000, 50_000))
        for (width in lengths) for (height in lengths) for (target in targets) for (fit in ImageFit.entries) {
            val size = decodeSize(IntSize(width, height), target, fit)
            assertTrue(size.width in 1..min(width, 8192) && size.height in 1..min(height, 8192))
            assertTrue(size.width.toLong() * size.height <= 16_000_000L)
        }
    }

    @Test
    fun aCappedDecodeCoversEveryFrameThatNeedsMore() {
        val source = IntSize(1_000_000, 100)
        val decoded = decodeSize(source, IntSize(300, 300), ImageFit.Cover)
        assertTrue(decodeCovers(decoded, source, IntSize(1080, 1080), ImageFit.Cover))
        assertTrue(decodeCovers(decoded, source, IntSize.Zero, ImageFit.Cover))
        assertTrue(decodeCovers(decoded, source, IntSize(1080, 1080), ImageFit.Contain))
    }

    @Test
    fun aSmallDecodeDoesNotCoverABiggerFrame() {
        val source = IntSize(1500, 1000)
        val decoded = decodeSize(source, IntSize(300, 300), ImageFit.Cover)
        assertTrue(decodeCovers(decoded, source, IntSize(300, 300), ImageFit.Cover))
        assertFalse(decodeCovers(decoded, source, IntSize(600, 600), ImageFit.Cover))
    }

    // readBounded

    @Test
    fun aBodyUpToTheLimitIsReadWhole() {
        val body = ByteArray(100) { it.toByte() }
        assertArrayEquals(body, readBounded(ByteArrayInputStream(body), maxBytes = 100))
        assertArrayEquals(body, readBounded(ByteArrayInputStream(body), maxBytes = 100, expectedLength = 100))
        assertArrayEquals(ByteArray(0), readBounded(ByteArrayInputStream(ByteArray(0)), maxBytes = 100, expectedLength = 0))
    }

    @Test
    fun aBodyOverTheLimitThrows() {
        val body = ByteArray(101)
        assertTrue(runCatching { readBounded(ByteArrayInputStream(body), maxBytes = 100) }.exceptionOrNull() is IOException)
        // The announced length is only a hint: a server that claims less is still held to the limit.
        assertTrue(runCatching { readBounded(ByteArrayInputStream(body), maxBytes = 100, expectedLength = 10) }.exceptionOrNull() is IOException)
    }

    @Test
    fun aWrongAnnouncedLengthStillReadsTheBody() {
        val body = ByteArray(5000) { (it % 251).toByte() }
        assertArrayEquals(body, readBounded(ByteArrayInputStream(body), maxBytes = 10_000, expectedLength = 10))
        assertArrayEquals(body, readBounded(ByteArrayInputStream(body), maxBytes = 10_000, expectedLength = 9000))
    }

    @Test
    fun anEndlessBodyStopsJustPastTheLimit() {
        for (limit in listOf(1000, 200_000)) {
            val endless = EndlessStream()
            assertTrue(runCatching { readBounded(endless, maxBytes = limit) }.exceptionOrNull() is IOException)
            assertEquals(limit + 1L, endless.served)
        }
    }

    // runAbortable

    @Test
    fun aDeadlineCutsOffAStepAnInterruptDoesNotStop() = runBlocking {
        val cutOff = CountDownLatch(1)
        val began = System.nanoTime()
        val result = runCatching {
            runAbortable(deadlineMillis = 100) { signal ->
                signal.onAbort { cutOff.countDown() }
                if (blockLikeASocketRead(cutOff, timeoutMillis = 10_000)) throw IOException("Socket closed") else "finished on its own"
            }
        }
        // A failure, not a cancellation, so the views waiting on it show their fallback.
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(0L, cutOff.count)
        assertTrue(System.nanoTime() - began < 5_000_000_000L)
    }

    @Test
    fun aStepDoneInTimeIsNeverCutOff() = runBlocking {
        val cutOff = AtomicBoolean(false)
        assertEquals("done", runAbortable(deadlineMillis = 5_000) { signal -> signal.onAbort { cutOff.set(true) }; "done" })
        assertFalse(cutOff.get())
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

/** A body that never ends, counting what it has served. */
private class EndlessStream : InputStream() {
    var served = 0L

    override fun read(): Int {
        served++
        return 0
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        served += len
        return len
    }
}

/**
 * Blocks until [latch] opens or [timeoutMillis] pass, shrugging off interrupts the way a read
 * blocked on a socket does. Whether it opened.
 */
private fun blockLikeASocketRead(latch: CountDownLatch, timeoutMillis: Long): Boolean {
    val end = System.nanoTime() + timeoutMillis * 1_000_000
    while (true) {
        val left = end - System.nanoTime()
        if (left <= 0) return false
        try {
            if (latch.await(left, TimeUnit.NANOSECONDS)) return true
        } catch (_: InterruptedException) {
            // Ignored, as a socket read ignores it.
        }
    }
}
