package model.json

import com.google.gson.annotations.SerializedName

data class Simpletiled(
        @SerializedName("height")
        val height: String?,
        @SerializedName("black")
        val black: String?,
        @SerializedName("limit")
        val limit: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("periodic")
        val periodic: String?,
        @SerializedName("screenshots")
        val screenshots: String?,
        @SerializedName("subset")
        val subset: String?,
        @SerializedName("width")
        val width: String?
): CommonModel()