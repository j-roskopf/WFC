package model.data

import com.google.gson.annotations.SerializedName

data class Neighbor(
        @SerializedName("-left")
        val left: String?,
        @SerializedName("-right")
        val right: String?
)