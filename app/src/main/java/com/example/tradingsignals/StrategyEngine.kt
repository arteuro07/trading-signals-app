package com.example.tradingsignals

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

data class Signal(val action: String, val message: String)

class StrategyEngine {

    private var script: String = ""

    fun setScript(script: String) {
        this.script = script
    }

    fun evaluate(price: Double): Signal {
        val rhino = Context.enter()
        rhino.optimizationLevel = -1
        return try {
            val scope = rhino.initStandardObjects()
            // Injecter le prix
            scope.put("price", scope, price)
            // Exécuter le script
            rhino.evaluateString(scope, script, "strategy", 1, null)
            // Le script doit définir un objet "signal" avec action et message
            val signalObj = scope.get("signal", scope) as? Scriptable
            if (signalObj != null) {
                val action = signalObj.get("action", signalObj)?.toString() ?: "HOLD"
                val message = signalObj.get("message", signalObj)?.toString() ?: ""
                Signal(action.uppercase(), message)
            } else {
                Signal("HOLD", "Pas de signal défini")
            }
        } catch (e: Exception) {
            Signal("ERROR", e.message ?: "Erreur dans le script")
        } finally {
            Context.exit()
        }
    }
}
