import model.Model
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

class OverlappingModel(val name: String, val N: Int, width: Int, height: Int, periodicInput: Boolean,
                       periodicOutput: Boolean, symmetry: Int, ground: Int) : Model(width, height) {

    private var patterns: Array<ByteArray?>? = null
    private var colors: ArrayList<Color>? = null
    private var ground: Int = 0

    init {
        periodic = periodicOutput
        val imageFile = File("samples/$name.png")
        val bitmap = ImageIO.read(imageFile)

        val smx = bitmap.width
        val smy = bitmap.height
        val sample = Array(smx) { ByteArray(smy) }
        colors = ArrayList()

        for (y in 0 until smy) {
            for (x in 0 until smx) {
                val color = Color(bitmap.getRGB(x, y))
                var i = 0
                run breakOut@{
                    colors?.forEach {
                        if (it == color) {
                            return@breakOut
                        }
                        i++
                    }
                }


                if (i == colors?.size ?: -1) colors?.add(color)
                sample[x][y] = i.toByte()
            }
        }

        val c = colors?.size ?: 0
        val w = Math.pow(c.toDouble(), (N * N).toDouble())

        fun pattern(passedInFunc: (Int, Int) -> Byte): ByteArray {
            val result = ByteArray(N * N)
            for (y in 0 until N) for (x in 0 until N) result[x + y * N] = passedInFunc(x, y)
            return result
        }

        fun patternFromSample(x: Int, y: Int): ByteArray {
            return pattern { dx, dy ->
                sample[(x + dx) % smx][(y + dy) % smy]
            }
        }

        fun rotate(p: ByteArray): ByteArray {
            return pattern { x, y ->
                p[N - 1 - y + x * N]
            }
        }

        fun reflect(p: ByteArray): ByteArray {
            return pattern { x, y ->
                p[N - 1 - x + y * N]
            }
        }

        fun index(p: ByteArray): Long {
            var result: Long = 0
            var power: Long = 1
            for (i in 0 until p.size) {
                result += p[p.size - 1 - i] * power
                power *= c
            }
            return result
        }

        fun patternFromIndex(ind: Long): ByteArray {
            var residue = ind
            var power = w.toLong()
            val result = ByteArray(N * N)

            for (i in 0 until result.size) {
                power /= c
                var count = 0

                while (residue >= power) {
                    residue -= power
                    count++
                }

                result[i] = count.toByte()
            }

            return result
        }

        val weights = HashMap<Long, Int>()
        val ordering = ArrayList<Long>()

        for (y in 0 until (if (periodicInput) smy else smy - N + 1)) {
            for (x in 0 until (if (periodicInput) smx else smx - N + 1)) {
                val ps = arrayOfNulls<ByteArray>(8)
                ps[0] = patternFromSample(x, y)
                ps[1] = reflect(ps[0]!!)
                ps[2] = rotate(ps[0]!!)
                ps[3] = reflect(ps[2]!!)
                ps[4] = rotate(ps[2]!!)
                ps[5] = reflect(ps[4]!!)
                ps[6] = rotate(ps[4]!!)
                ps[7] = reflect(ps[6]!!)

                for (k in 0 until symmetry) {
                    val ind = index(ps[k]!!)
                    if (weights.containsKey(ind))
                        weights[ind] = weights[ind]?.plus(1) ?: 0
                    else {
                        weights[ind] = 1
                        ordering.add(ind)
                    }
                }
            }
        }

        tCounter = weights.size
        this.ground = (ground + tCounter) % tCounter
        patterns = arrayOfNulls(tCounter)
        this.weights = DoubleArray(tCounter)

        var counter = 0
        ordering.forEach { orderItem ->
            patterns!![counter] = patternFromIndex(orderItem)
            this.weights[counter] = weights[orderItem]?.toDouble() ?: 0.0
            counter++
        }

        fun agrees(p1: ByteArray, p2: ByteArray, dx: Int, dy: Int): Boolean {
            val xMin = if (dx < 0) 0 else dx
            val xMax = if (dx < 0) dx + N else N
            val yMin = if (dy < 0) 0 else dy
            val yMax = if (dy < 0) dy + N else N

            for (y in yMin until yMax) for (x in xMin until xMax) {
                if (p1[x + N * y] != p2[x - dx + N * (y - dy)]) return false
            }
            return true
        }

        propagator = arrayOfNulls(4)
        for (d in 0 until 4) {
            propagator[d] = arrayOfNulls(tCounter)
            for (t in 0 until tCounter) {
                val list = ArrayList<Int>()
                for (t2 in 0 until tCounter) {
                    if (agrees(patterns!![t]!!, patterns!![t2]!!, dx[d], dy[d])) {
                        list.add(t2)
                    }
                }
                propagator[d]?.set(t, IntArray(list.size))
                for (i in 0 until list.size) propagator[d]?.get(t)?.set(i, list[i])
            }
        }

    }

    override fun onBoundary(x: Int, y: Int): Boolean {
        return !periodic && (x + N > FMX || y + N > FMY || x < 0 || y < 0)
    }

    override fun graphics(): BufferedImage? {
        val result = BufferedImage(FMX, FMY, BufferedImage.TYPE_4BYTE_ABGR)
        if (observed != null) {
            for (y in 0 until FMY) {
                val dy = if (y < FMY - N + 1){
                    0
                } else{
                    N - 1
                }
                for (x in 0 until FMX) {
                    val dx = if (x < FMX - N + 1){
                        0
                    } else {
                        N - 1
                    }
                    val c = colors!![patterns!![observed!![x - dx + (y - dy) * FMX]]!![dx + dy * N].toInt()]
                    val temp = (-0x1000000 or (c.red shl 16) or (c.green shl 8) or c.blue)
                    result.setRGB(x, y, temp)
                }
            }
        } else {
            for (i in 0 until wave.size) {
                var contributors = 0
                var r = 0
                var g = 0
                var b = 0
                val x = i % FMX
                val y = i / FMX

                for (dy in 0 until N){
                    for (dx in 0 until N) {
                        var sx = x - dx
                        if (sx < 0) sx += FMX

                        var sy = y - dy
                        if (sy < 0) sy += FMY

                        val s = sx + sy * FMX
                        if (onBoundary(sx, sy)){
                            continue
                        }

                        for (t in 0 until tCounter) {
                            if (wave[s]?.get(t) == true) {
                                contributors++
                                val color = colors!![patterns?.get(t)!![dx + dy * N].toInt()]
                                r += (color.red )
                                g += color.green
                                b += (color.blue)
                            }
                        }
                    }
                }

                val temp = (-0x1000000 or (r / contributors shl 16) or (g / contributors shl 8) or b / contributors).toBigInteger()
                val color = Color(temp.toInt())
                result.setRGB(x, y, color.rgb)
            }
        }

        return result
    }

    override fun clear() {
        super.clear()

        if (ground != 0) {
            for (x in 0 until FMX) {
                for (t in 0 until tCounter) if (t != ground) ban(x + (FMY - 1) * FMX, t)
                for (y in 0 until FMY - 1) ban(x + y * FMX, ground)
            }

            propagate()
        }
    }
}

fun Int.trimToColor(): Int {
    return if (this <= 255) {
        this
    } else {
        255
    }
}