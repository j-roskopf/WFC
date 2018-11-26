package model.json

import com.google.gson.annotations.SerializedName
import model.Model

data class Samples(
        @SerializedName("overlapping")
        val overlapping: List<Overlapping?>?,
        @SerializedName("simpletiled")
        val simpletiled: List<Simpletiled?>?
) {
        fun all(): ArrayList<CommonModel> {
                val toReturn = ArrayList<CommonModel>()

                overlapping?.forEach {
                        toReturn.add(it!!)
                }

                simpletiled?.forEach {
                        toReturn.add(it!!)
                }
                return toReturn
        }
}