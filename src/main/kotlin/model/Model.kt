package model

import utils.Random
import kotlin.random.Random
import java.awt.Graphics
import java.awt.image.BufferedImage


abstract class Model(val FMX: Int, val FMY: Int) {

    var DX = intArrayOf(-1, 0, 1, 0)
    var DY = intArrayOf(0, 1, 0, -1)
    var opposite = intArrayOf(2, 3, 0, 1)


    lateinit var wave: Array<Array<Boolean>?>
    lateinit var propagator: Array<Array<IntArray?>?>
    lateinit var compatible: Array<Array<IntArray?>?>
    var observed: IntArray? = null
    lateinit var stack: Array<Pair<Int, Int>?>
    lateinit var random: Random
    lateinit var weights: DoubleArray
    lateinit var weightLogWeights: DoubleArray
    lateinit var sumsOfOnes: IntArray
    lateinit var sumsOfWeights: DoubleArray
    lateinit var sumsOfWeightLogWeights: DoubleArray
    lateinit var entropies: DoubleArray

    var sumOfWeights: Double = 0.toDouble()
    var sumOfWeightLogWeights: Double = 0.toDouble()
    var startingEntropy: Double = 0.toDouble()
    var T: Int = 0
    var stacksize: Int = 0
    var periodic: Boolean = false

    fun Init() {
        wave = arrayOfNulls(FMX * FMY)
        compatible = arrayOfNulls(wave.size)

        for (i in 0 until wave.size) {
            wave[i] = Array(T) { false }
            compatible[i] = arrayOfNulls(T)

            for (t in 0 until T) {
                compatible[i]?.set(t, IntArray(4))
            }
        }

        weightLogWeights = DoubleArray(T)
        sumOfWeights = 0.0
        sumOfWeightLogWeights = 0.0

        for (t in 0 until T) {
            weightLogWeights[t] = weights[t] * Math.log(weights[t])
            sumOfWeights += weights[t]
            sumOfWeightLogWeights += weightLogWeights[t]
        }

        startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights

        sumsOfOnes = IntArray(FMX * FMY)
        sumsOfWeights = DoubleArray(FMX * FMY)
        sumsOfWeightLogWeights = DoubleArray(FMX * FMY)
        entropies = DoubleArray(FMX * FMY)

        stack = arrayOfNulls(wave.size * T)
        stacksize = 0

    }

    fun observe(): Boolean? {
        var min = 1E+3
        var argmin = -1

        loop@ for (i in 0 until wave.size) {
            if (OnBoundary(i % FMX, i / FMX)) continue@loop

            val amount = sumsOfOnes[i]
            if (amount == 0) return false

            val entropy = entropies[i]
            if (amount > 1 && entropy <= min) {
                val noise = 1E-6 * kotlin.random.Random.nextDouble()
                if (entropy + noise < min) {
                    min = entropy + noise
                    argmin = i
                }
            }
        }

        if (argmin == -1) {
            observed = IntArray(FMX * FMY)
            for (i in 0 until wave.size) for (t in 0 until T) if (wave[i]?.get(t) == true) {
                observed!![i] = t
                break
            }
            return true
        }

        val distribution = DoubleArray(T)
        for (t in 0 until T) {
            distribution[t] = if (wave[argmin]?.get(t) == true) weights[t] else 0.0
        }
        val r = distribution.Random(kotlin.random.Random.nextDouble())

        val w = wave[argmin]
        for (t in 0 until T) if (w?.get(t) != (t == r)) ban(argmin, t)

        return null
    }

    fun ban(i: Int, t: Int) {
        wave[i]?.set(t, false)

        val comp = compatible[i]?.get(t)
        for (d in 0..3) comp?.set(d, 0)
        stack[stacksize] = Pair(i, t)
        stacksize++

        var sum = sumsOfWeights[i]
        entropies[i] += sumsOfWeightLogWeights[i] / sum - Math.log(sum)

        sumsOfOnes[i] -= 1
        sumsOfWeights[i] -= weights[t]
        sumsOfWeightLogWeights[i] -= weightLogWeights[t]

        sum = sumsOfWeights[i]
        entropies[i] -= sumsOfWeightLogWeights[i] / sum - Math.log(sum)
    }

    protected fun Propagate() {
        while (stacksize > 0) {
            val e1 = stack[stacksize - 1]
            stacksize--

            val i1 = e1!!.first
            val x1 = i1 % FMX
            val y1 = i1 / FMX

            loop@ for (d in 0..3) {
                val dx = DX[d]
                val dy = DY[d]
                var x2 = x1 + dx
                var y2 = y1 + dy
                if (OnBoundary(x2, y2)) continue@loop

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

    fun Run(seed: Int, limit: Int): Boolean {
        if (!::wave.isInitialized) Init()

        Clear()
        random = Random(seed)

        for (l in 0 until limit) {
            if (limit == 0) break
            val result = observe()
            if (result != null) {
                return result
            }
            Propagate()
        }


        return true
    }

    open fun Clear() {
        for (i in 0 until wave.size) {
            for (t in 0 until T) {
                wave[i]?.set(t, true)
                for (d in 0..3) propagator[opposite[d]]?.get(t)?.size?.let { compatible[i]?.get(t)?.set(d, it) }
            }

            sumsOfOnes[i] = weights.size
            sumsOfWeights[i] = sumOfWeights
            sumsOfWeightLogWeights[i] = sumOfWeightLogWeights
            entropies[i] = startingEntropy
        }

    }

    abstract fun Graphics(): BufferedImage?

    protected abstract fun OnBoundary(x: Int, y: Int): Boolean

}

