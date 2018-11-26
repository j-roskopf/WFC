package model.data

import com.google.gson.annotations.SerializedName

data class Tile(
        @SerializedName("-name")
        val name: String?,
        @SerializedName("-symmetry")
        val symmetry: String?,
        @SerializedName("-weight")
        val weight: String?
)