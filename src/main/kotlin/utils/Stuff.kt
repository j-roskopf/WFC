package utils


fun DoubleArray.random(r: Double): Int {
    var sum = this.sum()

    if (sum == 0.0) {
        for (j in 0 until this.size) this[j] = 1.0
        sum = this.sum()
    }

    for (j in 0 until this.size) this[j] /= sum

    var i = 0
    var x = 0.0

    while (i < this.size) {
        x += this[i]
        if (r <= x) return i
        i++
    }

    return 0

}