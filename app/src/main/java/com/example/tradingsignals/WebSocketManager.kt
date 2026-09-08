package com.example.tradingsignals

import okhttp3.*
import org.json.JSONObject

class WebSocketManager(private val onPriceUpdate: (Double) -> Unit) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/ws/btcusdt@trade")
            .build()
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val price = json.getDouble("p") // Binance trade payload: "p" = price
                    onPriceUpdate(price)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Gérer l'échec
            }
        }
        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnected by user")
        webSocket = null
    }
}
