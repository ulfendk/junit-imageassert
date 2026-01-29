package dk.ulfen.imageassert.appium

import dk.ulfen.imageassert.ImageAssertion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Test class for AppiumImageAssertion functionality.
 * Uses mock objects to test without requiring an actual Appium server.
 */
class AppiumImageAssertionTest {

    @TempDir
    lateinit var tempDir: File

    /**
     * Create a simple solid color image for testing
     */
    private fun createSolidColorImage(width: Int, height: Int, color: Color): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return image
    }

    /**
     * Create a mock WebElement using a wrapper approach
     */
    private fun createMockWebElement(
        screenshot: BufferedImage,
        elementLocation: Point,
        elementSize: Dimension,
        supportsDirectScreenshot: Boolean = true
    ): WebElement {
        return object : WebElement {
            override fun <X : Any?> getScreenshotAs(target: OutputType<X>): X {
                if (!supportsDirectScreenshot) {
                    throw UnsupportedOperationException("Direct screenshot not supported")
                }
                val baos = ByteArrayOutputStream()
                ImageIO.write(screenshot, "png", baos)
                @Suppress("UNCHECKED_CAST")
                return when (target) {
                    OutputType.BYTES -> baos.toByteArray() as X
                    OutputType.FILE -> {
                        val tmpFile = File.createTempFile("screenshot", ".png")
                        ImageIO.write(screenshot, "png", tmpFile)
                        tmpFile as X
                    }
                    else -> baos.toByteArray() as X
                }
            }

            override fun getLocation(): Point = elementLocation
            override fun getSize(): Dimension = elementSize

            // Stub implementations for required methods
            override fun click() {}
            override fun submit() {}
            override fun sendKeys(vararg keysToSend: CharSequence?) {}
            override fun clear() {}
            override fun getTagName(): String = "div"
            override fun getAttribute(name: String): String? = null
            override fun isSelected(): Boolean = false
            override fun isEnabled(): Boolean = true
            override fun getText(): String = ""
            override fun findElements(by: org.openqa.selenium.By): List<WebElement> = emptyList()
            override fun findElement(by: org.openqa.selenium.By): WebElement? = null
            override fun isDisplayed(): Boolean = true
            override fun getRect(): org.openqa.selenium.Rectangle = org.openqa.selenium.Rectangle(elementLocation, elementSize)
            override fun getCssValue(propertyName: String): String = ""
            override fun getDomProperty(name: String): String = ""
            override fun getDomAttribute(name: String): String? = null
            override fun getAriaRole(): String = ""
            override fun getAccessibleName(): String = ""
            override fun getShadowRoot(): org.openqa.selenium.SearchContext? = null
        }
    }

    /**
     * Create a mock WebDriver
     */
    private fun createMockWebDriver(fullScreenshot: BufferedImage): WebDriver {
        return object : WebDriver, TakesScreenshot {
            override fun <X : Any?> getScreenshotAs(target: OutputType<X>): X {
                val baos = ByteArrayOutputStream()
                ImageIO.write(fullScreenshot, "png", baos)
                @Suppress("UNCHECKED_CAST")
                return when (target) {
                    OutputType.BYTES -> baos.toByteArray() as X
                    OutputType.FILE -> {
                        val tmpFile = File.createTempFile("screenshot", ".png")
                        ImageIO.write(fullScreenshot, "png", tmpFile)
                        tmpFile as X
                    }
                    else -> baos.toByteArray() as X
                }
            }

            // Stub implementations for required methods
            override fun get(url: String) {}
            override fun getCurrentUrl(): String = ""
            override fun getTitle(): String = ""
            override fun findElements(by: org.openqa.selenium.By): List<WebElement> = emptyList()
            override fun findElement(by: org.openqa.selenium.By): WebElement? = null
            override fun getPageSource(): String = ""
            override fun close() {}
            override fun quit() {}
            override fun getWindowHandles(): Set<String> = emptySet()
            override fun getWindowHandle(): String = ""
            override fun switchTo(): WebDriver.TargetLocator = throw UnsupportedOperationException()
            override fun navigate(): WebDriver.Navigation = throw UnsupportedOperationException()
            override fun manage(): WebDriver.Options = throw UnsupportedOperationException()
        }
    }

    @Test
    fun `test assertElementImageEquals with matching images`() {
        // Create a simple element screenshot
        val elementImage = createSolidColorImage(50, 50, Color.RED)
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(50, 50))
        
        // Create a matching expected image
        val expectedImage = createSolidColorImage(50, 50, Color.RED)
        
        // Create a mock driver (not used in this case since element returns screenshot directly)
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // Should not throw
        assertDoesNotThrow {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedImage)
        }
    }

    @Test
    fun `test assertElementImageEquals with file and matching images`() {
        // Create a simple element screenshot
        val elementImage = createSolidColorImage(50, 50, Color.BLUE)
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(50, 50))
        
        // Create a matching expected image file
        val expectedFile = File(tempDir, "expected.png")
        ImageIO.write(createSolidColorImage(50, 50, Color.BLUE), "png", expectedFile)
        
        // Create a mock driver
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // Should not throw
        assertDoesNotThrow {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedFile)
        }
    }

    @Test
    fun `test assertElementImageEquals with different images fails`() {
        // Create a red element screenshot
        val elementImage = createSolidColorImage(50, 50, Color.RED)
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(50, 50))
        
        // Create a blue expected image
        val expectedImage = createSolidColorImage(50, 50, Color.BLUE)
        
        // Create a mock driver
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // Should throw AssertionError
        assertThrows(AssertionError::class.java) {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedImage)
        }
    }

    @Test
    fun `test assertElementImageEquals handles different sizes`() {
        // Create a small element screenshot
        val elementImage = createSolidColorImage(25, 25, Color.GREEN)
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(25, 25))
        
        // Create a larger expected image with same color
        val expectedImage = createSolidColorImage(50, 50, Color.GREEN)
        
        // Create a mock driver
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // Should not throw - same color, different sizes
        assertDoesNotThrow {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedImage)
        }
    }

    @Test
    fun `test captureElementScreenshot returns correct image`() {
        // Create an element screenshot
        val elementImage = createSolidColorImage(50, 50, Color.YELLOW)
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(50, 50))
        
        // Create a mock driver
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // Capture the screenshot
        val capturedImage = AppiumImageAssertion.captureElementScreenshot(element, driver)

        // Verify dimensions
        assertEquals(50, capturedImage.width)
        assertEquals(50, capturedImage.height)

        // Verify it's the same image (pixel comparison)
        assertTrue(imagesAreIdentical(elementImage, capturedImage))
    }

    @Test
    fun `test element screenshot with custom configuration`() {
        // Create an element with slight color variation
        val elementImage = createSolidColorImage(50, 50, Color(255, 0, 0))
        val element = createMockWebElement(elementImage, Point(0, 0), Dimension(50, 50))
        
        // Create an expected image with slight variation
        val expectedImage = createSolidColorImage(50, 50, Color(250, 0, 0))
        
        // Create a mock driver
        val driver = createMockWebDriver(createSolidColorImage(100, 100, Color.WHITE))

        // With strict tolerance, should fail
        val strictConfig = ImageAssertion.ComparisonConfig(tolerance = 0.0)
        assertThrows(AssertionError::class.java) {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedImage, strictConfig)
        }

        // With lenient tolerance, should pass
        val lenientConfig = ImageAssertion.ComparisonConfig(tolerance = 0.02)
        assertDoesNotThrow {
            AppiumImageAssertion.assertElementImageEquals(element, driver, expectedImage, lenientConfig)
        }
    }

    @Test
    fun `test fallback screenshot cropping`() {
        // Create a full screenshot with a colored region for the element
        val fullScreenshot = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
        val graphics = fullScreenshot.createGraphics()
        
        // Fill with white background
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, 200, 200)
        
        // Fill element region with red (at position 50,50 with size 50x50)
        graphics.color = Color.RED
        graphics.fillRect(50, 50, 50, 50)
        graphics.dispose()

        // Create a mock element that will fail direct screenshot (to force fallback)
        val element = createMockWebElement(
            createSolidColorImage(50, 50, Color.RED),
            Point(50, 50),
            Dimension(50, 50),
            supportsDirectScreenshot = false
        )

        val driver = createMockWebDriver(fullScreenshot)

        // Capture using fallback method
        val capturedImage = AppiumImageAssertion.captureElementScreenshot(element, driver)

        // Verify dimensions
        assertEquals(50, capturedImage.width)
        assertEquals(50, capturedImage.height)

        // Verify the cropped region is red
        val expectedCropped = createSolidColorImage(50, 50, Color.RED)
        assertTrue(imagesAreIdentical(expectedCropped, capturedImage))
    }

    /**
     * Helper method to check if two images are pixel-perfect identical
     */
    private fun imagesAreIdentical(image1: BufferedImage, image2: BufferedImage): Boolean {
        if (image1.width != image2.width || image1.height != image2.height) {
            return false
        }

        for (y in 0 until image1.height) {
            for (x in 0 until image1.width) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false
                }
            }
        }

        return true
    }
}
