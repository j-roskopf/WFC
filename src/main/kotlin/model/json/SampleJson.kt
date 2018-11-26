package model.json

import com.google.gson.annotations.SerializedName

data class SampleJson(
        @SerializedName("samples")
        val samples: Samples?
)