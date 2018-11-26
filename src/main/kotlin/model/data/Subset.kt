package model.data

import com.google.gson.annotations.SerializedName

data class Subset(
        @SerializedName("-name")
        val name: String?,
        @SerializedName("tile")
        val tile: List<Tile?>?
)