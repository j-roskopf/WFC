import junit.framework.Assert.fail
import model.SimpleTiledModel
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File

const val SEED = 1236548748

class MainTests {

    @Test
    fun overlappingModelTestConsistency() {
        val image = generateOverlappingImage()
        if(image == null) {
            fail("Image was null")
        }

        val actualHexes = HashMap<String, Int>()
        for(x in 0 until (image?.width ?: 0)) {
            for(y in 0 until (image?.height ?: 0)) {
                val color = Color(image?.getRGB(x, y) ?: 0)
                val hex = String.format("%02X%02X%02X", color.red, color.green, color.blue)
                actualHexes[hex] = (actualHexes.getOrDefault(hex, 0) + 1)
            }
        }

        val expectedHexes = HashMap<String, Int>()
        File(javaClass.classLoader.getResource("overlapping_hex.txt").file).forEachLine { hex ->
            expectedHexes[hex] = (expectedHexes.getOrDefault(hex, 0) + 1)
        }

        if(expectedHexes.keys.size != actualHexes.keys.size) {
            fail("Different map size")
        }

        expectedHexes.forEach {
            assert(it.value == actualHexes[it.key])
        }
    }

    private fun generateOverlappingImage(): BufferedImage? {
        val model = OverlappingModel(
                name = "Skyline",
                N = 3,
                symmetry = 2,
                ground = -1,
                periodicInput = true,
                periodicOutput = true,
                width = 48,
                height = 48
        )

        for (i in 0 until 2) {
            for (k in 0 until 10) {
                val finished = model.Run(SEED, 0)
                if (finished) {
                    return model.Graphics()
                }
            }
        }

        return null
    }

    @Test
    fun simpleModelTestConsistency() {
        val image = generateSimpleImage()
        if(image == null) {
            fail("Image was null")
        }

        val actualHexes = HashMap<String, Int>()
        for(x in 0 until (image?.width ?: 0)) {
            for(y in 0 until (image?.height ?: 0)) {
                val color = Color(image?.getRGB(x, y) ?: 0)
                val hex = String.format("%02X%02X%02X", color.red, color.green, color.blue)
                actualHexes[hex] = (actualHexes.getOrDefault(hex, 0) + 1)
            }
        }

        val expectedHexes = HashMap<String, Int>()
        File(javaClass.classLoader.getResource("simple.txt").file).forEachLine { hex ->
            expectedHexes[hex] = (expectedHexes.getOrDefault(hex, 0) + 1)
        }

        if(expectedHexes.keys.size != actualHexes.keys.size) {
            fail("Different map size")
        }

        expectedHexes.forEach {
            assert(it.value == actualHexes[it.key])
        }
    }

    private fun generateSimpleImage(): BufferedImage? {
        val model = SimpleTiledModel(
                name = "Knots",
                width = 24,
                height = 24,
                subsetName = "Dense Fabric",
                periodic = true,
                black = false
        )

        for (i in 0 until 2) {
            for (k in 0 until 10) {
                val finished = model.Run(SEED, 0)
                if (finished) {
                    return model.Graphics()
                }
            }
        }

        return null
    }
}

