package com.srithar.wizbulb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.util.Log

object WizBulbController {

    private const val TAG = "WizBulbController"
    private const val PORT = 38899

    suspend fun sendCommand(
        ip: String,
        json: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            if (ip.isBlank()) return@withContext false
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                val data = json.toByteArray()
                val address = InetAddress.getByName(ip)
                val packet = DatagramPacket(
                    data,
                    data.size,
                    address,
                    PORT
                )
                socket.send(packet)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command to $ip: ${e.message}")
                false
            } finally {
                socket?.close()
            }
        }
    }

    suspend fun turnOn(ip: String): Boolean {
        val json = """
            {
              "method":"setState",
              "params":{
                "state":true
              }
            }
        """.trimIndent()
        return sendCommand(ip, json)
    }

    suspend fun turnOff(ip: String): Boolean {
        val json = """
            {
              "method":"setState",
              "params":{
                "state":false
              }
            }
        """.trimIndent()
        return sendCommand(ip, json)
    }

    suspend fun setColor(
        ip: String,
        r: Int,
        g: Int,
        b: Int
    ): Boolean {
        val json = """
            {
              "method":"setPilot",
              "params":{
                "r":$r,
                "g":$g,
                "b":$b
              }
            }
        """.trimIndent()
        return sendCommand(ip, json)
    }

    suspend fun getStatus(ip: String): WizState? {
        return withContext(Dispatchers.IO) {
            if (ip.isBlank()) return@withContext null
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = 2000

                val request = """
                {
                  "method":"getPilot",
                  "params":{}
                }
                """.trimIndent()

                val data = request.toByteArray()
                val address = InetAddress.getByName(ip)
                val packet = DatagramPacket(
                    data,
                    data.size,
                    address,
                    PORT
                )

                socket.send(packet)

                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)

                val json = String(responsePacket.data, 0, responsePacket.length)
                val obj = org.json.JSONObject(json)
                
                if (obj.has("result")) {
                    val result = obj.getJSONObject("result")
                    WizState(
                        isOn = result.optBoolean("state", false),
                        brightness = result.optInt("dimming", 0),
                        r = result.optInt("r", 0),
                        g = result.optInt("g", 0),
                        b = result.optInt("b", 0),
                        temp = result.optInt("temp", 0),
                        sceneId = result.optInt("sceneId", 0)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting status from $ip: ${e.message}")
                null
            } finally {
                socket?.close()
            }
        }
    }

    suspend fun setScene(ip: String, sceneId: Int): Boolean {
        val json = """
            {
              "method":"setPilot",
              "params":{
                "sceneId":$sceneId
              }
            }
        """.trimIndent()
        return sendCommand(ip, json)
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        addresses.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting broadcast addresses: ${e.message}")
        }
        // Always include the universal broadcast address as a fallback
        try {
            val universal = InetAddress.getByName("255.255.255.255")
            if (!addresses.contains(universal)) {
                addresses.add(universal)
            }
        } catch (e: Exception) {}
        return addresses
    }

    suspend fun discoverBulbs(): List<String> {
        return withContext(Dispatchers.IO) {
            val discoveredIps = mutableSetOf<String>()
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 500 // Short timeout for responsive looping

                val discoveryMessage = """{"method":"getPilot","params":{}}"""
                val data = discoveryMessage.toByteArray()
                val broadcastAddresses = getBroadcastAddresses()

                val buffer = ByteArray(1024)
                
                // Try multiple discovery cycles to improve reliability across different networks
                repeat(3) {
                    broadcastAddresses.forEach { address ->
                        try {
                            val packet = DatagramPacket(data, data.size, address, PORT)
                            socket.send(packet)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to send broadcast to $address: ${e.message}")
                        }
                    }

                    // Collect responses for a short period after each broadcast
                    val cycleStartTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - cycleStartTime < 700) {
                        try {
                            val responsePacket = DatagramPacket(buffer, buffer.size)
                            socket.receive(responsePacket)
                            val ip = responsePacket.address.hostAddress
                            if (ip != null) {
                                discoveredIps.add(ip)
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            // Expected if no more responses are immediately available
                        } catch (e: Exception) {
                            Log.e(TAG, "Error receiving discovery packet: ${e.message}")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during discovery: ${e.message}")
            } finally {
                socket?.close()
            }
            discoveredIps.toList().sorted()
        }
    }
}

data class WizState(
    val isOn: Boolean,
    val brightness: Int,
    val r: Int,
    val g: Int,
    val b: Int,
    val temp: Int,
    val sceneId: Int = 0
)
