# junit-imageassert

A Kotlin library for JUnit that provides image assertion capabilities with built-in support for different screen sizes. This library allows you to validate UI elements against expected image files, even when screen sizes differ.

## Features

- **Screen-Size Aware**: Automatically scales images for comparison to handle different screen resolutions
- **Flexible Comparison**: Multiple scaling modes to fit your testing needs
- **Configurable Tolerance**: Adjust sensitivity for pixel-perfect or fuzzy matching
- **JUnit Integration**: Works seamlessly with JUnit 5 tests
- **Kotlin-First**: Written in Kotlin with a clean, idiomatic API

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("dk.ulfen:junit-imageassert:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    testImplementation 'dk.ulfen:junit-imageassert:1.0.0'
}
```

## Usage

### Basic Usage

```kotlin
import dk.ulfen.imageassert.ImageAssertion
import java.io.File
import javax.imageio.ImageIO

@Test
fun `test UI element matches expected image`() {
    // Capture your UI element as a BufferedImage
    val actualImage = captureUIElement()
    
    // Compare against expected image file
    val expectedFile = File("src/test/resources/expected-button.png")
    
    // Assert images match (handles different sizes automatically)
    ImageAssertion.assertImageEquals(actualImage, expectedFile)
}
```

### Comparing Two Image Files

```kotlin
@Test
fun `test two image files match`() {
    val actualFile = File("screenshots/actual.png")
    val expectedFile = File("screenshots/expected.png")
    
    ImageAssertion.assertImageEquals(actualFile, expectedFile)
}
```

### Comparing Two BufferedImages

```kotlin
@Test
fun `test two images match`() {
    val image1 = ImageIO.read(File("image1.png"))
    val image2 = ImageIO.read(File("image2.png"))
    
    ImageAssertion.assertImageEquals(image1, image2)
}
```

### Configuring Comparison Settings

```kotlin
@Test
fun `test with custom configuration`() {
    val config = ImageAssertion.ComparisonConfig(
        tolerance = 0.05,  // 5% tolerance for differences
        scalingMode = ImageAssertion.ScalingMode.FIT_TO_EXPECTED
    )
    
    ImageAssertion.assertImageEquals(actualImage, expectedFile, config)
}
```

## Scaling Modes

The library supports multiple scaling modes to handle different screen sizes:

### FIT_TO_EXPECTED (Default)
Scales the actual image to match the expected image dimensions.
```kotlin
val config = ImageAssertion.ComparisonConfig(
    scalingMode = ImageAssertion.ScalingMode.FIT_TO_EXPECTED
)
```

### FIT_TO_ACTUAL
Scales the expected image to match the actual image dimensions.
```kotlin
val config = ImageAssertion.ComparisonConfig(
    scalingMode = ImageAssertion.ScalingMode.FIT_TO_ACTUAL
)
```

### FIT_TO_SMALLER
Scales both images to the smaller dimensions.
```kotlin
val config = ImageAssertion.ComparisonConfig(
    scalingMode = ImageAssertion.ScalingMode.FIT_TO_SMALLER
)
```

### FIT_TO_LARGER
Scales both images to the larger dimensions.
```kotlin
val config = ImageAssertion.ComparisonConfig(
    scalingMode = ImageAssertion.ScalingMode.FIT_TO_LARGER
)
```

## Tolerance Configuration

The `tolerance` parameter controls how strict the comparison is:

- **0.0**: Pixel-perfect match required (very strict)
- **0.01**: 1% difference allowed (default)
- **0.05**: 5% difference allowed (lenient)
- **0.10**: 10% difference allowed (very lenient)

```kotlin
// Strict comparison - pixel perfect
val strictConfig = ImageAssertion.ComparisonConfig(tolerance = 0.0)

// Lenient comparison - allows for slight rendering differences
val lenientConfig = ImageAssertion.ComparisonConfig(tolerance = 0.05)
```

## Non-Throwing Comparison

If you want to check similarity without throwing an exception:

```kotlin
val isSimilar = ImageAssertion.areImagesSimilar(
    actualImage, 
    expectedImage,
    config = ImageAssertion.ComparisonConfig(tolerance = 0.05)
)

if (isSimilar) {
    println("Images match!")
} else {
    println("Images differ")
}
```

## Practical Examples

### Testing a Button at Different Resolutions

```kotlin
@Test
fun `button appearance is consistent across resolutions`() {
    // Expected image captured at 1920x1080
    val expectedButton = File("src/test/resources/button-1920x1080.png")
    
    // Test at 1280x720 - scales automatically
    val buttonAt720p = captureButtonAt720p()
    ImageAssertion.assertImageEquals(buttonAt720p, expectedButton)
    
    // Test at 2560x1440 - scales automatically
    val buttonAt1440p = captureButtonAt1440p()
    ImageAssertion.assertImageEquals(buttonAt1440p, expectedButton)
}
```

### Handling Anti-Aliasing Differences

```kotlin
@Test
fun `logo rendering with anti-aliasing tolerance`() {
    val config = ImageAssertion.ComparisonConfig(
        tolerance = 0.02,  // Allow 2% difference for anti-aliasing
        scalingMode = ImageAssertion.ScalingMode.FIT_TO_EXPECTED
    )
    
    ImageAssertion.assertImageEquals(
        actualLogo, 
        File("expected-logo.png"),
        config
    )
}
```

## How It Works

1. **Image Loading**: Loads both actual and expected images
2. **Size Detection**: Checks if images have different dimensions
3. **Scaling**: If sizes differ, scales according to the chosen scaling mode using high-quality interpolation
4. **Pixel Comparison**: Compares each pixel's RGB values
5. **Difference Calculation**: Calculates total difference as a percentage
6. **Assertion**: Throws AssertionError if difference exceeds tolerance

## Building from Source

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Requirements

- Kotlin 1.9.22 or higher
- JUnit 5.10.1 or higher
- Java 11 or higher

## License

This project is open source. See LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
