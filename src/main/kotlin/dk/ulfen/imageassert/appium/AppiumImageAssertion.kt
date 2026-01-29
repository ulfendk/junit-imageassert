package dk.ulfen.imageassert.appium

import dk.ulfen.imageassert.ImageAssertion
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * AppiumImageAssertion provides assertion functions for comparing Appium UI elements
 * against expected image files. It handles capturing screenshots of UI elements
 * and then uses the ImageAssertion functionality for comparison.
 */
object AppiumImageAssertion {

    /**
     * Assert that a UI element matches an expected image file.
     * This method captures a screenshot of the element and compares it with the expected image.
     *
     * @param element The Appium WebElement to screenshot and compare
     * @param driver The WebDriver instance (needed for full screenshot)
     * @param expectedImageFile The file containing the expected image
     * @param config Configuration for the image comparison
     * @throws AssertionError if the images don't match within tolerance
     */
    fun assertElementImageEquals(
        element: WebElement,
        driver: WebDriver,
        expectedImageFile: File,
        config: ImageAssertion.ComparisonConfig = ImageAssertion.ComparisonConfig()
    ) {
        val elementScreenshot = captureElementScreenshot(element, driver)
        ImageAssertion.assertImageEquals(elementScreenshot, expectedImageFile, config)
    }

    /**
     * Assert that a UI element matches an expected BufferedImage.
     * This method captures a screenshot of the element and compares it with the expected image.
     *
     * @param element The Appium WebElement to screenshot and compare
     * @param driver The WebDriver instance (needed for full screenshot)
     * @param expectedImage The expected BufferedImage
     * @param config Configuration for the image comparison
     * @throws AssertionError if the images don't match within tolerance
     */
    fun assertElementImageEquals(
        element: WebElement,
        driver: WebDriver,
        expectedImage: BufferedImage,
        config: ImageAssertion.ComparisonConfig = ImageAssertion.ComparisonConfig()
    ) {
        val elementScreenshot = captureElementScreenshot(element, driver)
        ImageAssertion.assertImageEquals(elementScreenshot, expectedImage, config)
    }

    /**
     * Capture a screenshot of a specific UI element.
     * 
     * This method takes a full screenshot of the screen, then crops it to the element's bounds.
     * 
     * @param element The WebElement to screenshot
     * @param driver The WebDriver instance
     * @return BufferedImage of the element
     */
    fun captureElementScreenshot(element: WebElement, driver: WebDriver): BufferedImage {
        // Get the element screenshot directly if supported
        return try {
            // Try direct element screenshot first (supported by some drivers)
            val screenshotBytes = element.getScreenshotAs(OutputType.BYTES)
            val inputStream = ByteArrayInputStream(screenshotBytes)
            ImageIO.read(inputStream)
                ?: throw IllegalStateException("Failed to read element screenshot")
        } catch (e: Exception) {
            // Fallback: capture full screen and crop to element
            captureElementScreenshotFallback(element, driver)
        }
    }

    /**
     * Fallback method to capture element screenshot by cropping the full screenshot.
     * 
     * @param element The WebElement to screenshot
     * @param driver The WebDriver instance
     * @return BufferedImage of the element
     */
    private fun captureElementScreenshotFallback(element: WebElement, driver: WebDriver): BufferedImage {
        // Capture full screenshot
        val screenshotBytes = (driver as? org.openqa.selenium.TakesScreenshot)?.getScreenshotAs(OutputType.BYTES)
            ?: throw IllegalArgumentException("Driver does not support screenshots")
        
        val fullScreenshot = ImageIO.read(ByteArrayInputStream(screenshotBytes))
            ?: throw IllegalStateException("Failed to read full screenshot")
        
        // Get element location and size
        val location: Point = element.location
        val size: org.openqa.selenium.Dimension = element.size
        
        // Crop the image to element bounds
        val x = location.x
        val y = location.y
        val width = size.width
        val height = size.height
        
        // Ensure bounds are within image dimensions
        val actualX = maxOf(0, minOf(x, fullScreenshot.width - 1))
        val actualY = maxOf(0, minOf(y, fullScreenshot.height - 1))
        val actualWidth = minOf(width, fullScreenshot.width - actualX)
        val actualHeight = minOf(height, fullScreenshot.height - actualY)
        
        if (actualWidth <= 0 || actualHeight <= 0) {
            throw IllegalArgumentException(
                "Element is outside visible bounds: location=($x, $y), size=($width x $height), " +
                "screenshot size=(${fullScreenshot.width} x ${fullScreenshot.height})"
            )
        }
        
        return fullScreenshot.getSubimage(actualX, actualY, actualWidth, actualHeight)
    }
}
