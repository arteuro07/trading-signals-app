package com.example.tradingsignals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var priceTextView: TextView
    private lateinit var scriptEditText: EditText
    private lateinit var statusTextView: TextView
    private lateinit var toggleButton: Button

    private var webSocketManager: WebSocketManager? = null
    private var strategyEngine = StrategyEngine()
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.priceTextView)
        scriptEditText = findViewById(R.id.scriptEditText)
        statusTextView = findViewById(R.id.statusTextView)
        toggleButton = findViewById(R.id.toggleButton)

        // Exemple de script par défaut
        scriptEditText.setText("""
            // Exemple : acheter si le prix > 60000
            if (price > 60000) {
                signal = { action: "BUY", message: "Prix élevé" };
            } else {
                signal = { action: "HOLD", message: "Attente" };
            }
        """.trimIndent())

        // Demander la permission de notification pour Android 13+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        toggleButton.setOnClickListener {
            if (!isRunning) {
                startStrategy()
            } else {
                stopStrategy()
            }
        }
    }

    private fun startStrategy() {
        val script = scriptEditText.text.toString()
        if (script.isBlank()) {
            Toast.makeText(this, "Le script est vide", Toast.LENGTH_SHORT).show()
            return
        }
        strategyEngine.setScript(script)

        webSocketManager = WebSocketManager { price ->
            runOnUiThread {
                priceTextView.text = "Prix BTC/USDT : $price"
                val signal = strategyEngine.evaluate(price)
                statusTextView.text = "Signal : ${signal.action} - ${signal.message}"
                if (signal.action == "BUY" || signal.action == "SELL") {
                    NotificationHelper.sendNotification(this, signal)
                }
            }
        }
        webSocketManager?.connect()
        isRunning = true
        toggleButton.text = "Arrêter"
        statusTextView.text = "En cours d'exécution..."
    }

    private fun stopStrategy() {
        webSocketManager?.disconnect()
        webSocketManager = null
        isRunning = false
        toggleButton.text = "Démarrer"
        statusTextView.text = "Arrêté"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStrategy()
        scope.cancel()
    }
}
