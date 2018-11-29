import com.google.gson.Gson
import model.Model
import model.SimpleTiledModel
import model.json.SampleJson
import java.io.File
import java.io.FileReader
import java.io.BufferedReader
import kotlin.random.Random
import model.json.Overlapping
import model.json.Simpletiled
import javax.imageio.ImageIO

class Main {
    companion object {
        private val gson = Gson()
        private val random = Random

        @JvmStatic
        fun main(args: Array<String>) {
            val startTime = System.currentTimeMillis()
            println("start time = $startTime")

            val fileName = "samples.json"
            val file = File(fileName)
            if(file.exists()) {
                //Read the employee.json file
                val bufferedReader = BufferedReader(FileReader(fileName))

                val data = gson.fromJson(bufferedReader, SampleJson::class.java)

                var model: Model? = null
                var screenshots = 2
                var limit = 0
                var counter = 1
                var name = ""
                data.samples?.all()?.forEach { commonModel ->
                    if(commonModel is Overlapping) {
                        model = OverlappingModel(
                                name = commonModel.name ?: "",
                                N = commonModel.n?.toInt() ?: 2,
                                width = commonModel.width?.toInt() ?: 48,
                                height = commonModel.height?.toInt() ?: 48,
                                periodicInput = commonModel.periodicInput?.toBoolean() ?: true,
                                periodicOutput = commonModel.periodic?.toBoolean() ?: false,
                                symmetry = commonModel.symmetry?.toInt() ?: 8,
                                ground = commonModel.ground?.toInt() ?: 0
                        )
                        screenshots = commonModel.screenshots?.toInt() ?: 2
                        limit = commonModel.limit?.toInt() ?: 0
                        name = commonModel.name?: "WRONG NAME 1"
                    } else if(commonModel is Simpletiled) {
                        model = SimpleTiledModel(
                                width = commonModel.width?.toInt() ?: 10,
                                height = commonModel.height?.toInt() ?: 10,
                                name = commonModel.name ?: "Knots",
                                subsetName = commonModel.subset ?: "",
                                periodic = commonModel.periodic?.toBoolean() ?: false,
                                black = commonModel.black?.toBoolean() ?: false
                        )
                        screenshots = commonModel.screenshots?.toInt() ?: 2
                        limit = commonModel.limit?.toInt() ?: 0
                        name = commonModel.name?: "WRONG NAME 2"
                    }

                    for(i in 0 until screenshots) {
                        for(k in 0 until 10) {
                            println("> ")
                            val seed = random.nextInt()
                            val finished = model?.Run(seed, limit)
                            if(finished == true) {
                                println("DONE")
                                val image = model?.Graphics()
                                val outputFile = File("out/$counter $name $i.png")
                                ImageIO.write(image, "png", outputFile)
                            } else {
                                println("CONTRADICTION")
                            }
                        }
                    }

                    counter++
                }

                println("time = ${System.currentTimeMillis() - startTime} milliseconds")
            }
        }
    }
}