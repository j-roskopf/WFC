package model.data

import com.google.gson.annotations.SerializedName

data class Set(
        @SerializedName("-size")
        val size: String?,
        @SerializedName("-unique")
        val unique: String?,
        @SerializedName("neighbors")
        val neighbors: Neighbors?,
        @SerializedName("tiles")
        val tiles: Tiles?,
        @SerializedName("subsets")
        val subsets: Subsets?
)