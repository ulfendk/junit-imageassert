package dk.ulfen.imageassert

import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * ImageAssertion provides assertion functions for comparing UI element screenshots
 * against expected image files. It handles different screen sizes by scaling images
 * appropriately before comparison.
 */
object ImageAssertion {

    /**
     * Configuration for image comparison
     */
    data class ComparisonConfig(
        val tolerance: Double = 0.01,
        val scalingMode: ScalingMode = ScalingMode.FIT_TO_EXPECTED,
        val allowedPixelDifference: Int = 5
    )

    /**
     * Defines how images should be scaled for comparison
     */
    enum class ScalingMode {
        /** Scale the actual image to match the expected image dimensions */
        FIT_TO_EXPECTED,
        /** Scale the expected image to match the actual image dimensions */
        FIT_TO_ACTUAL,
        /** Scale both images to the smaller dimensions */
        FIT_TO_SMALLER,
        /** Scale both images to the larger dimensions */
        FIT_TO_LARGER
    }

    /**
     * Assert that an actual image matches an expected image file.
     * The comparison handles different screen sizes by scaling appropriately.
     *
     * @param actualImage The actual BufferedImage captured from UI
     * @param expectedImageFile The file containing the expected image
     * @param config Configuration for the comparison
     * @throws AssertionError if the images don't match within tolerance
     */
    fun assertImageEquals(
        actualImage: BufferedImage,
        expectedImageFile: File,
        config: ComparisonConfig = ComparisonConfig()
    ) {
        require(expectedImageFile.exists()) {
            "Expected image file does not exist: ${expectedImageFile.absolutePath}"
        }

        val expectedImage = ImageIO.read(expectedImageFile)
            ?: throw IllegalArgumentException("Could not read expected image from: ${expectedImageFile.absolutePath}")

        assertImageEquals(actualImage, expectedImage, config)
    }

    /**
     * Assert that an actual image matches an expected image.
     * The comparison handles different screen sizes by scaling appropriately.
     *
     * @param actualImage The actual BufferedImage captured from UI
     * @param expectedImage The expected BufferedImage
     * @param config Configuration for the comparison
     * @throws AssertionError if the images don't match within tolerance
     */
    fun assertImageEquals(
        actualImage: BufferedImage,
        expectedImage: BufferedImage,
        config: ComparisonConfig = ComparisonConfig()
    ) {
        val (scaledActual, scaledExpected) = scaleImagesForComparison(
            actualImage,
            expectedImage,
            config.scalingMode
        )

        val difference = calculateImageDifference(scaledActual, scaledExpected)

        if (difference > config.tolerance) {
            throw AssertionError(
                "Images do not match. Difference: ${difference * 100}%, " +
                "Tolerance: ${config.tolerance * 100}%. " +
                "Actual size: ${actualImage.width}x${actualImage.height}, " +
                "Expected size: ${expectedImage.width}x${expectedImage.height}, " +
                "Scaled to: ${scaledActual.width}x${scaledActual.height}"
            )
        }
    }

    /**
     * Assert that an actual image file matches an expected image file.
     * The comparison handles different screen sizes by scaling appropriately.
     *
     * @param actualImageFile The file containing the actual image
     * @param expectedImageFile The file containing the expected image
     * @param config Configuration for the comparison
     * @throws AssertionError if the images don't match within tolerance
     */
    fun assertImageEquals(
        actualImageFile: File,
        expectedImageFile: File,
        config: ComparisonConfig = ComparisonConfig()
    ) {
        require(actualImageFile.exists()) {
            "Actual image file does not exist: ${actualImageFile.absolutePath}"
        }
        require(expectedImageFile.exists()) {
            "Expected image file does not exist: ${expectedImageFile.absolutePath}"
        }

        val actualImage = ImageIO.read(actualImageFile)
            ?: throw IllegalArgumentException("Could not read actual image from: ${actualImageFile.absolutePath}")
        val expectedImage = ImageIO.read(expectedImageFile)
            ?: throw IllegalArgumentException("Could not read expected image from: ${expectedImageFile.absolutePath}")

        assertImageEquals(actualImage, expectedImage, config)
    }

    /**
     * Scale images for comparison based on the scaling mode
     */
    private fun scaleImagesForComparison(
        actualImage: BufferedImage,
        expectedImage: BufferedImage,
        mode: ScalingMode
    ): Pair<BufferedImage, BufferedImage> {
        // If images are already the same size, no scaling needed
        if (actualImage.width == expectedImage.width && actualImage.height == expectedImage.height) {
            return Pair(actualImage, expectedImage)
        }

        return when (mode) {
            ScalingMode.FIT_TO_EXPECTED -> {
                val scaledActual = scaleImage(actualImage, expectedImage.width, expectedImage.height)
                Pair(scaledActual, expectedImage)
            }
            ScalingMode.FIT_TO_ACTUAL -> {
                val scaledExpected = scaleImage(expectedImage, actualImage.width, actualImage.height)
                Pair(actualImage, scaledExpected)
            }
            ScalingMode.FIT_TO_SMALLER -> {
                val targetWidth = minOf(actualImage.width, expectedImage.width)
                val targetHeight = minOf(actualImage.height, expectedImage.height)
                val scaledActual = scaleImage(actualImage, targetWidth, targetHeight)
                val scaledExpected = scaleImage(expectedImage, targetWidth, targetHeight)
                Pair(scaledActual, scaledExpected)
            }
            ScalingMode.FIT_TO_LARGER -> {
                val targetWidth = maxOf(actualImage.width, expectedImage.width)
                val targetHeight = maxOf(actualImage.height, expectedImage.height)
                val scaledActual = scaleImage(actualImage, targetWidth, targetHeight)
                val scaledExpected = scaleImage(expectedImage, targetWidth, targetHeight)
                Pair(scaledActual, scaledExpected)
            }
        }
    }

    /**
     * Scale an image to the specified dimensions using high-quality scaling
     */
    private fun scaleImage(image: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        // If already at target size, return as-is
        if (image.width == targetWidth && image.height == targetHeight) {
            return image
        }

        // Use imgscalr for high-quality scaling
        return Scalr.resize(
            image,
            Scalr.Method.QUALITY,
            Scalr.Mode.FIT_EXACT,
            targetWidth,
            targetHeight
        )
    }

    /**
     * Calculate the difference between two images as a percentage.
     * Returns a value between 0.0 (identical) and 1.0 (completely different)
     */
    private fun calculateImageDifference(image1: BufferedImage, image2: BufferedImage): Double {
        require(image1.width == image2.width && image1.height == image2.height) {
            "Images must be the same size for comparison"
        }

        var totalDifference = 0L
        val totalPixels = image1.width * image1.height
        val maxDifferencePerPixel = 255 * 3 // RGB channels

        for (y in 0 until image1.height) {
            for (x in 0 until image1.width) {
                val rgb1 = image1.getRGB(x, y)
                val rgb2 = image2.getRGB(x, y)

                val r1 = (rgb1 shr 16) and 0xFF
                val g1 = (rgb1 shr 8) and 0xFF
                val b1 = rgb1 and 0xFF

                val r2 = (rgb2 shr 16) and 0xFF
                val g2 = (rgb2 shr 8) and 0xFF
                val b2 = rgb2 and 0xFF

                val pixelDifference = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
                totalDifference += pixelDifference
            }
        }

        return totalDifference.toDouble() / (totalPixels * maxDifferencePerPixel)
    }

    /**
     * Check if two images are similar within the given tolerance.
     * Returns true if the difference is within tolerance, false otherwise.
     */
    fun areImagesSimilar(
        actualImage: BufferedImage,
        expectedImage: BufferedImage,
        config: ComparisonConfig = ComparisonConfig()
    ): Boolean {
        return try {
            assertImageEquals(actualImage, expectedImage, config)
            true
        } catch (e: AssertionError) {
            false
        }
    }
}
