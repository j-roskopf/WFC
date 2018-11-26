package model.json

import com.google.gson.annotations.SerializedName

data class Overlapping(
        @SerializedName("N")
        val n: String?,
        @SerializedName("ground")
        val ground: String?,
        @SerializedName("height")
        val height: String?,
        @SerializedName("limit")
        val limit: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("periodic")
        val periodic: String?,
        @SerializedName("periodicInput")
        val periodicInput: String?,
        @SerializedName("screenshots")
        val screenshots: String?,
        @SerializedName("symmetry")
        val symmetry: String?,
        @SerializedName("width")
        val width: String?
): CommonModel()