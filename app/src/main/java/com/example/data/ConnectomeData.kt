package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConnectomeMeta(
    val dataset: String,
    val edgesSource: String,
    val annotationsSource: String,
    val countMeaning: String,
    val signMeaning: String,
    val license: String,
    val simplifications: List<String>
)

data class ConnectomeEngineConfig(
    val leak: Double = 0.6,
    val gain: Double = 0.95,
    val rule: String = "",
    val eatThreshold: Double = 0.5,
    val escapeThreshold: Double = 0.5
)

data class ConnectomeData(
    val meta: ConnectomeMeta,
    val engineConfig: ConnectomeEngineConfig,
    val neurons: List<Neuron>,
    val synapses: List<Synapse>,
    val idToNeuron: HashMap<String, Neuron>
)

object ConnectomeParser {

    fun loadFromAssets(context: Context, fileName: String = "connectome.json"): ConnectomeData {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return parse(jsonString)
    }

    fun parse(jsonString: String): ConnectomeData {
        val root = JSONObject(jsonString)

        // Parse meta
        val metaObj = root.optJSONObject("meta") ?: JSONObject()
        val simplificationsList = mutableListOf<String>()
        val simplificationsArr = metaObj.optJSONArray("simplifications")
        if (simplificationsArr != null) {
            for (i in 0 until simplificationsArr.length()) {
                simplificationsList.add(simplificationsArr.getString(i))
            }
        }
        val meta = ConnectomeMeta(
            dataset = metaObj.optString("dataset", ""),
            edgesSource = metaObj.optString("edges_source", ""),
            annotationsSource = metaObj.optString("annotations_source", ""),
            countMeaning = metaObj.optString("count_meaning", ""),
            signMeaning = metaObj.optString("sign_meaning", ""),
            license = metaObj.optString("license", ""),
            simplifications = simplificationsList
        )

        // Parse engine
        val engObj = root.optJSONObject("engine") ?: JSONObject()
        val engineConfig = ConnectomeEngineConfig(
            leak = engObj.optDouble("leak", 0.6),
            gain = engObj.optDouble("gain", 0.95),
            rule = engObj.optString("rule", ""),
            eatThreshold = engObj.optDouble("eatThreshold", 0.5),
            escapeThreshold = engObj.optDouble("escapeThreshold", 0.5)
        )

        // Parse neurons
        val neuronsArr = root.getJSONArray("neurons")
        val neurons = ArrayList<Neuron>(neuronsArr.length())
        val idToNeuron = HashMap<String, Neuron>(neuronsArr.length())

        for (i in 0 until neuronsArr.length()) {
            val nObj = neuronsArr.getJSONObject(i)
            val id = nObj.getString("id")
            val cellType = nObj.getString("cellType")
            val side = nObj.getString("side")
            val role = nObj.getString("role")

            val circuitsList = mutableListOf<String>()
            val circArr = nObj.optJSONArray("circuits")
            if (circArr != null) {
                for (c in 0 until circArr.length()) {
                    circuitsList.add(circArr.getString(c))
                }
            }

            val neuron = Neuron(
                id = id,
                cellType = cellType,
                side = side,
                role = role,
                circuits = circuitsList,
                index = i
            )
            neurons.add(neuron)
            idToNeuron[id] = neuron
        }

        // Parse synapses
        val synapsesArr = root.getJSONArray("synapses")
        val synapses = ArrayList<Synapse>(synapsesArr.length())

        for (i in 0 until synapsesArr.length()) {
            val synArr = synapsesArr.getJSONArray(i)
            val preIndex = synArr.getInt(0)
            val postIndex = synArr.getInt(1)
            val count = synArr.getInt(2)
            val sign = synArr.getInt(3)
            synapses.add(Synapse(preIndex, postIndex, count, sign))
        }

        return ConnectomeData(
            meta = meta,
            engineConfig = engineConfig,
            neurons = neurons,
            synapses = synapses,
            idToNeuron = idToNeuron
        )
    }
}
