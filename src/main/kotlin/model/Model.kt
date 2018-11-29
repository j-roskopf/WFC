package model

import utils.random
import kotlin.random.Random
import java.awt.image.BufferedImage

abstract class Model(val FMX: Int, val FMY: Int) {

    var dx = intArrayOf(-1, 0, 1, 0)
    var dy = intArrayOf(0, 1, 0, -1)
    private var opposite = intArrayOf(2, 3, 0, 1)
    lateinit var wave: Array<Array<Boolean>?>
    lateinit var propagator: Array<Array<IntArray?>?>
    private lateinit var compatible: Array<Array<IntArray?>?>
    var observed: IntArray? = null
    private lateinit var stack: Array<Pair<Int, Int>?>
    private lateinit var random: Random
    lateinit var weights: DoubleArray
    private lateinit var weightLogWeights: DoubleArray
    private lateinit var sumsOfOnes: IntArray
    private lateinit var sumsOfWeights: DoubleArray
    private lateinit var sumsOfWeightLogWeights: DoubleArray
    private lateinit var entropies: DoubleArray
    private var sumOfWeights: Double = 0.toDouble()
    private var sumOfWeightLogWeights: Double = 0.toDouble()
    private var startingEntropy: Double = 0.toDouble()
    protected var tCounter: Int = 0
    private var stackSize: Int = 0
    var periodic: Boolean = false

    private fun init() {
        wave = arrayOfNulls(FMX * FMY)
        compatible = arrayOfNulls(wave.size)

        for (i in 0 until wave.size) {
            wave[i] = Array(tCounter) { false }
            compatible[i] = arrayOfNulls(tCounter)

            for (t in 0 until tCounter) {
                compatible[i]?.set(t, IntArray(4))
            }
        }

        weightLogWeights = DoubleArray(tCounter)
        sumOfWeights = 0.0
        sumOfWeightLogWeights = 0.0

        for (t in 0 until tCounter) {
            weightLogWeights[t] = weights[t] * Math.log(weights[t])
            sumOfWeights += weights[t]
            sumOfWeightLogWeights += weightLogWeights[t]
        }

        startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights

        sumsOfOnes = IntArray(FMX * FMY)
        sumsOfWeights = DoubleArray(FMX * FMY)
        sumsOfWeightLogWeights = DoubleArray(FMX * FMY)
        entropies = DoubleArray(FMX * FMY)

        stack = arrayOfNulls(wave.size * tCounter)
        stackSize = 0

    }

    private fun observe(): Boolean? {
        var min = 1E+3
        var argMin = -1

        loop@ for (i in 0 until wave.size) {
            if (onBoundary(i % FMX, i / FMX)) continue@loop

            val amount = sumsOfOnes[i]
            if (amount == 0) return false

            val entropy = entropies[i]
            if (amount > 1 && entropy <= min) {
                val noise = 1E-6 * random.nextDouble()
                if (entropy + noise < min) {
                    min = entropy + noise
                    argMin = i
                }
            }
        }

        if (argMin == -1) {
            observed = IntArray(FMX * FMY)
            for (i in 0 until wave.size) for (t in 0 until tCounter) if (wave[i]?.get(t) == true) {
                observed!![i] = t
                break
            }
            return true
        }

        val distribution = DoubleArray(tCounter)
        for (t in 0 until tCounter) {
            distribution[t] = if (wave[argMin]?.get(t) == true) weights[t] else 0.0
        }
        val r = distribution.random(random.nextDouble())

        val w = wave[argMin]
        for (t in 0 until tCounter) if (w?.get(t) != (t == r)) ban(argMin, t)

        return null
    }

    fun ban(i: Int, t: Int) {
        wave[i]?.set(t, false)

        val comp = compatible[i]?.get(t)
        for (d in 0..3) comp?.set(d, 0)
        stack[stackSize] = Pair(i, t)
        stackSize++

        var sum = sumsOfWeights[i]
        entropies[i] += sumsOfWeightLogWeights[i] / sum - Math.log(sum)

        sumsOfOnes[i] -= 1
        sumsOfWeights[i] -= weights[t]
        sumsOfWeightLogWeights[i] -= weightLogWeights[t]

        sum = sumsOfWeights[i]
        entropies[i] -= sumsOfWeightLogWeights[i] / sum - Math.log(sum)
    }

    protected fun propagate() {
        while (stackSize > 0) {
            val e1 = stack[stackSize - 1]
            stackSize--

            val i1 = e1!!.first
            val x1 = i1 % FMX
            val y1 = i1 / FMX

            loop@ for (d in 0..3) {
                val dx = dx[d]
                val dy = dy[d]
                var x2 = x1 + dx
                var y2 = y1 + dy
                if (onBoundary(x2, y2)) continue@loop

                if (x2 < 0)
                    x2 += FMX
                else if (x2 >= FMX) x2 -= FMX
                if (y2 < 0)
                    y2 += FMY
                else if (y2 >= FMY) y2 -= FMY

                val i2 = x2 + y2 * FMX
                val p = propagator[d]?.get(e1.second)
                val compat = compatible[i2]

                for (l in 0 until (p?.size ?: 0)) {
                    val t2 = p?.get(l)
                    val comp = compat?.get(t2 ?: 0)

                    comp?.set(d, comp[d] - 1)
                    if (comp?.get(d) == 0) ban(i2, t2 ?: 0)
                }
            }
        }
    }

    fun run(seed: Int, limit: Int): Boolean {
        if (!::wave.isInitialized) init()

        clear()
        random = Random(seed)

        var l = 0
        do {
            val result = observe()
            if (result != null) {
                return result
            }
            propagate()
            l++
        }while (l < limit || limit == 0)

        return true
    }

    open fun clear() {
        for (i in 0 until wave.size) {
            for (t in 0 until tCounter) {
                wave[i]?.set(t, true)
                for (d in 0..3) propagator[opposite[d]]?.get(t)?.size?.let { compatible[i]?.get(t)?.set(d, it) }
            }

            sumsOfOnes[i] = weights.size
            sumsOfWeights[i] = sumOfWeights
            sumsOfWeightLogWeights[i] = sumOfWeightLogWeights
            entropies[i] = startingEntropy
        }

    }

    abstract fun graphics(): BufferedImage?

    protected abstract fun onBoundary(x: Int, y: Int): Boolean

}

