package model

import com.google.gson.Gson
import model.data.SampleData
import trimToColor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import javax.imageio.ImageIO


class SimpleTiledModel(width: Int, height: Int, val name: String, subsetName: String, periodic: Boolean, black: Boolean) : Model(width, height) {

    private lateinit var tiles: ArrayList<Array<Color?>>
    private lateinit var tilenames: ArrayList<String>
    private var tilesize: Int = 0
    private var black: Boolean = false
    private val useSubset = subsetName.isNotEmpty()
    val gson = Gson()

    init {
        this.periodic = periodic
        this.black = black

        val fileName = "samples/$name/data.json"
        val file = File(fileName)
        if (file.exists()) {
            val bufferedReader = BufferedReader(FileReader(fileName))
            val data = gson.fromJson(bufferedReader, SampleData::class.java)

            tilesize = data.set?.size?.toInt() ?: 16
            val unique = data.set?.unique?.toBoolean() ?: false

            var subset: ArrayList<String>? = null
            if (!subsetName.isEmpty()) {
                val xSubSet = data.set?.subsets?.subset?.first()?.name
                if (xSubSet == null) {
                    println("ERROR SUBSET $subsetName not found")
                } else {
                    data.set.tiles?.tile?.forEach {
                        if(subset == null) {
                            subset = ArrayList()
                        }
                        subset?.add(it?.name ?: "")
                    }
                }
            }

            fun tile(passedInFunc: (Int, Int) -> Color): Array<Color?> {
                val result = arrayOfNulls<Color>(tilesize * tilesize)
                for (y in 0 until tilesize) for (x in 0 until tilesize) result[x + y * tilesize] = passedInFunc(x, y)
                return result
            }

            fun rotate(array: Array<Color?>): Array<Color?> {
                return tile { x: Int, y: Int ->
                    array[tilesize - 1 - y + x * tilesize] ?: Color.BLACK
                }
            }

            tiles = arrayListOf()
            tilenames = ArrayList()
            val tempStationary = ArrayList<Double>()
            val action = arrayListOf<IntArray>()

            val firstOccurrence = HashMap<String, Int>()

            data.set?.tiles?.tile?.forEach beginning@{ tile ->
                val tileName = tile?.name ?: ""
                if (useSubset && !subset!!.contains(tileName)) {
                    return@beginning
                }

                lateinit var a: (Int) -> Int
                lateinit var b: (Int) -> Int
                val cardinality: Int

                val sym = tile?.symmetry ?: "X"
                when (sym) {
                    "L" -> {
                        cardinality = 4
                        a = { i -> (i + 1) % 4 }
                        b = { i -> if (i % 2 == 0) i + 1 else i - 1 }
                    }
                    "T" -> {
                        cardinality = 4
                        a = { i -> (i + 1) % 4 }
                        b = { i -> if (i % 2 == 0) i else 4 - i }
                    }
                    "I" -> {
                        cardinality = 2
                        a = { i -> 1 - i }
                        b = { i -> i }
                    }
                    "\\" -> {
                        cardinality = 2
                        a = { i -> 1 - i }
                        b = { i -> 1 - i }
                    }
                    else -> {
                        cardinality = 1
                        a = { i -> i }
                        b = { i -> i }
                    }
                }

                tCounter = action.size
                firstOccurrence[tileName] = tCounter

                val map = arrayOfNulls<IntArray>(cardinality)
                for (t in 0 until cardinality) {
                    map[t] = IntArray(8)

                    map[t]?.set(0, t)
                    map[t]?.set(1, a(t))
                    map[t]?.set(2, a(a(t)))
                    map[t]?.set(3, a(a(a(t))))
                    map[t]?.set(4, b(t))
                    map[t]?.set(5, b(a(t)))
                    map[t]?.set(6, b(a(a(t))))
                    map[t]?.set(7, b(a(a(a(t)))))

                    for (s in 0..7) {
                        map[t]!![s] = map[t]!![s] + tCounter
                    }

                    map[t]?.let { action.add(it) }
                }

                if (unique) {
                    for (t in 0 until cardinality) {
                        val bufferedImage: BufferedImage = ImageIO.read(File("samples/$name/$tileName $t.png"))
                        tiles.add(
                                tile { x, y ->
                                    val color = bufferedImage.getRGB(x, y)
                                    Color(color)
                                }
                        )
                        tilenames.add("$tileName $t")
                    }
                } else {
                    val bufferedImage: BufferedImage = ImageIO.read(File("samples/$name/$tileName.png"))
                    tiles.add(
                            tile { x, y ->
                                val color = bufferedImage.getRGB(x, y)
                                Color(color)
                            }
                    )
                    tilenames.add("$tileName ${0}")

                    for (t in 1 until cardinality) {
                        tiles.add(rotate(tiles[tCounter + t - 1]))
                        tilenames.add("$tileName $t")
                    }
                }

                for (t in 0 until cardinality) tempStationary.add(tile?.weight?.toDouble() ?: 1.0)
            }

            tCounter = action.size
            weights = tempStationary.toDoubleArray()

            propagator = arrayOfNulls<Array<IntArray?>?>(4)
            val tempPropagator = arrayOfNulls<Array<Array<Boolean?>?>>(4)

            for (d in 0..3) {
                tempPropagator[d] = arrayOfNulls(tCounter)
                propagator[d] = arrayOfNulls(tCounter)
                for (t in 0 until tCounter) tempPropagator[d]?.set(t, arrayOfNulls(tCounter))
            }

            data.set?.neighbors?.neighbor?.forEach breakOut@{ neighbor ->
                val left = neighbor?.left?.split(" ".toRegex(), 0)?.filter {
                    it.isNotEmpty()
                }
                val right = neighbor?.right?.split(" ".toRegex(), 0)?.filter {
                    it.isNotEmpty()
                }

                if (subset != null && (!subset!!.contains(left?.get(0)) || !subset!!.contains(right?.get(0)))) return@breakOut

                val leftPosition = action[firstOccurrence[left?.get(0)]
                        ?: 0][if (left?.size == 1) 0 else left?.get(1)?.toInt() ?: 0]
                val downPosition = action[leftPosition][1]
                val rightPosition = action[firstOccurrence[right?.get(0)]
                        ?: 0][if (right?.size == 1) 0 else right?.get(1)?.toInt() ?: 0]
                val upPosition = action[rightPosition][1]

                tempPropagator[0]!![rightPosition]!![leftPosition] = true
                tempPropagator[0]!![action[rightPosition][6]]!![action[leftPosition][6]] = true
                tempPropagator[0]!![action[leftPosition][4]]!![action[rightPosition][4]] = true
                tempPropagator[0]!![action[leftPosition][2]]!![action[rightPosition][2]] = true

                tempPropagator[1]!![upPosition]!![downPosition] = true
                tempPropagator[1]!![action[downPosition][6]]!![action[upPosition][6]] = true
                tempPropagator[1]!![action[upPosition][4]]!![action[downPosition][4]] = true
                tempPropagator[1]!![action[downPosition][2]]!![action[upPosition][2]] = true

            }


            for (t2 in 0 until tCounter) {
                for (t1 in 0 until tCounter) {
                    tempPropagator[2]?.get(t2)?.set(t1, tempPropagator[0]?.get(t1)?.get(t2))
                    tempPropagator[3]?.get(t2)?.set(t1, tempPropagator[1]?.get(t1)?.get(t2))
                }
            }

            val sparsePropagator = arrayOfNulls<Array<ArrayList<Int>?>>(4)

            for (d in 0..3) {
                sparsePropagator[d] = arrayOfNulls(tCounter)
                for (t in 0 until tCounter) sparsePropagator[d]?.set(t, arrayListOf())
            }

            for (d in 0..3)
                for (t1 in 0 until tCounter) {
                    val sp = sparsePropagator[d]?.get(t1)
                    val tp = tempPropagator[d]?.get(t1)

                    for (t2 in 0 until tCounter) if (tp?.get(t2) == true) sp?.add(t2)

                    val size = sp?.size ?: 0
                    propagator[d]?.set(t1, IntArray(size))
                    for (st in 0 until size) sp?.get(st)?.let { propagator[d]?.get(t1)?.set(st, it) }
                }
        }
    }

    override fun onBoundary(x: Int, y: Int): Boolean {
        return !periodic && (x < 0 || y < 0 || x >= FMX || y >= FMY)
    }

    override fun graphics(): BufferedImage? {
        val result = BufferedImage(FMX * tilesize, FMY * tilesize, BufferedImage.TYPE_4BYTE_ABGR)

        if (observed != null) {
            for (x in 0 until FMX)
                for (y in 0 until FMY) {
                    val tile = tiles[observed!![x + y * FMX]]
                    for (yt in 0 until tilesize)
                        for (xt in 0 until tilesize) {
                            val c = tile[xt + yt * tilesize]
                            result.setRGB(x * tilesize + xt, y * tilesize + yt, c!!.rgb)
                        }
                }
        } else {
            for (x in 0 until FMX) {
                for (y in 0 until FMY) {
                    val a = wave[x + y * FMX]
                    val amount = a?.asSequence()?.filter {
                        it
                    }?.count()
                    val lambda = 1.0 / (0 until tCounter).filter { t -> a?.get(t) ?: false }.map { t -> weights[t] }.sum()
                    for (yt in 0 until tilesize) {
                        for (xt in 0 until tilesize) {
                            if (black && amount == tCounter) {
                                val blackColor = Color.black
                                result.setRGB(x * tilesize + xt, y * tilesize + yt, blackColor.rgb)
                            } else {
                                var r = 0.0
                                var g = 0.0
                                var b = 0.0
                                for (t in 0 until tCounter) {
                                    if (wave[x + y * FMX]?.get(t) == true) {
                                        val c = tiles[t][xt + yt * tilesize]
                                        r += c?.red?.toDouble() ?: 0.0 * weights[t] * lambda
                                        g += c?.green?.toDouble() ?: 0.0 * weights[t] * lambda
                                        b += c?.blue?.toDouble() ?: 0.0 * weights[t] * lambda
                                    }
                                }

                                val color = Color(r.toInt().trimToColor(), g.toInt().trimToColor(), b.toInt().trimToColor())
                                val xCord = x * tilesize + xt
                                val yCord = y * tilesize + yt
                                result.setRGB(xCord, yCord, color.rgb)
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    fun textOutput(): String {
        val result = StringBuilder()
        for (x in 0 until FMX) {
            for (y in 0 until FMY) {
                val name = tilenames[observed?.get(x + y * FMX)!!]
                result.append(name).append("\n")
            }
        }

        return result.toString()
    }
}