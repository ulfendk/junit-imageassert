package dk.ulfen.imageassert

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageAssertionTest {

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
     * Create an image with a simple pattern for testing
     */
    private fun createPatternImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        
        // Create a checkerboard pattern
        val squareSize = 10
        for (y in 0 until height step squareSize) {
            for (x in 0 until width step squareSize) {
                val isWhite = ((x / squareSize) + (y / squareSize)) % 2 == 0
                graphics.color = if (isWhite) Color.WHITE else Color.BLACK
                graphics.fillRect(x, y, squareSize, squareSize)
            }
        }
        
        graphics.dispose()
        return image
    }

    /**
     * Create an image with a gradient that scales better
     */
    private fun createGradientImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            val greenValue = (255.0 * y / height).toInt()
            for (x in 0 until width) {
                val redValue = (255.0 * x / width).toInt()
                val rgb = (redValue shl 16) or (greenValue shl 8) or 0
                image.setRGB(x, y, rgb)
            }
        }
        return image
    }

    @Test
    fun `test identical images pass assertion`() {
        val image1 = createSolidColorImage(100, 100, Color.RED)
        val image2 = createSolidColorImage(100, 100, Color.RED)

        // Should not throw
        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(image1, image2)
        }
    }

    @Test
    fun `test different images fail assertion`() {
        val image1 = createSolidColorImage(100, 100, Color.RED)
        val image2 = createSolidColorImage(100, 100, Color.BLUE)

        // Should throw AssertionError
        assertThrows(AssertionError::class.java) {
            ImageAssertion.assertImageEquals(image1, image2)
        }
    }

    @Test
    fun `test images with different sizes are scaled and compared`() {
        val smallImage = createSolidColorImage(50, 50, Color.GREEN)
        val largeImage = createSolidColorImage(100, 100, Color.GREEN)

        // Should not throw - same color, different sizes
        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(smallImage, largeImage)
        }
    }

    @Test
    fun `test images with different sizes and colors fail`() {
        val smallImage = createSolidColorImage(50, 50, Color.GREEN)
        val largeImage = createSolidColorImage(100, 100, Color.RED)

        // Should throw - different colors
        assertThrows(AssertionError::class.java) {
            ImageAssertion.assertImageEquals(smallImage, largeImage)
        }
    }

    @Test
    fun `test gradient images with different sizes scale well`() {
        // Gradient images scale much better than patterns
        val smallGradient = createGradientImage(50, 50)
        val largeGradient = createGradientImage(100, 100)

        // Should pass with modest tolerance - gradients scale cleanly
        val config = ImageAssertion.ComparisonConfig(tolerance = 0.05)
        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(smallGradient, largeGradient, config)
        }
    }

    @Test
    fun `test FIT_TO_EXPECTED scaling mode`() {
        val actualImage = createSolidColorImage(200, 200, Color.BLUE)
        val expectedImage = createSolidColorImage(100, 100, Color.BLUE)

        val config = ImageAssertion.ComparisonConfig(
            scalingMode = ImageAssertion.ScalingMode.FIT_TO_EXPECTED
        )

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(actualImage, expectedImage, config)
        }
    }

    @Test
    fun `test FIT_TO_ACTUAL scaling mode`() {
        val actualImage = createSolidColorImage(100, 100, Color.YELLOW)
        val expectedImage = createSolidColorImage(200, 200, Color.YELLOW)

        val config = ImageAssertion.ComparisonConfig(
            scalingMode = ImageAssertion.ScalingMode.FIT_TO_ACTUAL
        )

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(actualImage, expectedImage, config)
        }
    }

    @Test
    fun `test FIT_TO_SMALLER scaling mode`() {
        val image1 = createSolidColorImage(100, 100, Color.MAGENTA)
        val image2 = createSolidColorImage(200, 150, Color.MAGENTA)

        val config = ImageAssertion.ComparisonConfig(
            scalingMode = ImageAssertion.ScalingMode.FIT_TO_SMALLER
        )

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(image1, image2, config)
        }
    }

    @Test
    fun `test FIT_TO_LARGER scaling mode`() {
        val image1 = createSolidColorImage(100, 100, Color.CYAN)
        val image2 = createSolidColorImage(150, 200, Color.CYAN)

        val config = ImageAssertion.ComparisonConfig(
            scalingMode = ImageAssertion.ScalingMode.FIT_TO_LARGER
        )

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(image1, image2, config)
        }
    }

    @Test
    fun `test tolerance configuration`() {
        // Create two very slightly different images
        val image1 = createSolidColorImage(100, 100, Color(255, 0, 0))
        val image2 = createSolidColorImage(100, 100, Color(250, 0, 0))

        // With zero tolerance, should fail
        val strictConfig = ImageAssertion.ComparisonConfig(tolerance = 0.0)
        assertThrows(AssertionError::class.java) {
            ImageAssertion.assertImageEquals(image1, image2, strictConfig)
        }

        // With reasonable tolerance, should pass
        val lenientConfig = ImageAssertion.ComparisonConfig(tolerance = 0.02)
        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(image1, image2, lenientConfig)
        }
    }

    @Test
    fun `test assertImageEquals with file paths`() {
        val image = createSolidColorImage(100, 100, Color.ORANGE)
        
        val actualFile = File(tempDir, "actual.png")
        val expectedFile = File(tempDir, "expected.png")
        
        ImageIO.write(image, "png", actualFile)
        ImageIO.write(image, "png", expectedFile)

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(actualFile, expectedFile)
        }
    }

    @Test
    fun `test assertImageEquals with BufferedImage and File`() {
        val image = createSolidColorImage(100, 100, Color.PINK)
        
        val expectedFile = File(tempDir, "expected.png")
        ImageIO.write(image, "png", expectedFile)

        val actualImage = createSolidColorImage(100, 100, Color.PINK)

        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(actualImage, expectedFile)
        }
    }

    @Test
    fun `test file does not exist throws exception`() {
        val nonExistentFile = File(tempDir, "does-not-exist.png")
        val actualImage = createSolidColorImage(100, 100, Color.WHITE)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ImageAssertion.assertImageEquals(actualImage, nonExistentFile)
        }

        assertTrue(exception.message!!.contains("does not exist"))
    }

    @Test
    fun `test areImagesSimilar returns true for similar images`() {
        val image1 = createSolidColorImage(100, 100, Color.RED)
        val image2 = createSolidColorImage(100, 100, Color.RED)

        assertTrue(ImageAssertion.areImagesSimilar(image1, image2))
    }

    @Test
    fun `test areImagesSimilar returns false for different images`() {
        val image1 = createSolidColorImage(100, 100, Color.RED)
        val image2 = createSolidColorImage(100, 100, Color.BLUE)

        assertFalse(ImageAssertion.areImagesSimilar(image1, image2))
    }

    @Test
    fun `test different aspect ratios with scaling`() {
        // Create images with same content but different aspect ratios
        // Wide image with horizontal gradient
        val wideImage = BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 100) {
            for (x in 0 until 200) {
                val redValue = (255.0 * x / 200).toInt()
                val rgb = (redValue shl 16)
                wideImage.setRGB(x, y, rgb)
            }
        }
        
        // Create a similar wide image to compare
        val wideImage2 = BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 200) {
            for (x in 0 until 400) {
                val redValue = (255.0 * x / 400).toInt()
                val rgb = (redValue shl 16)
                wideImage2.setRGB(x, y, rgb)
            }
        }

        // Should handle same aspect ratio images with reasonable tolerance
        val config = ImageAssertion.ComparisonConfig(tolerance = 0.02)
        assertDoesNotThrow {
            ImageAssertion.assertImageEquals(wideImage, wideImage2, config)
        }
    }

    @Test
    fun `test assertion error message contains useful information`() {
        val smallImage = createSolidColorImage(50, 50, Color.RED)
        val largeImage = createSolidColorImage(100, 100, Color.BLUE)

        val exception = assertThrows(AssertionError::class.java) {
            ImageAssertion.assertImageEquals(smallImage, largeImage)
        }

        val message = exception.message!!
        assertTrue(message.contains("Images do not match"))
        assertTrue(message.contains("50x50")) // Actual size
        assertTrue(message.contains("100x100")) // Expected size
        assertTrue(message.contains("Difference"))
        assertTrue(message.contains("Tolerance"))
    }
}
