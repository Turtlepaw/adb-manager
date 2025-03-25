package com.turtlepaw.adb

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Helper class to execute ADB commands
 */
class AdbCommandExecutor(private val adbPath: String = "adb") {

    /**
     * Executes an ADB command and returns the output as a string
     */
    fun executeCommand(vararg args: String): Result<String> {
        return try {
            val command = mutableListOf(adbPath)
            command.addAll(args)

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val exitCode = process.waitFor(30, TimeUnit.SECONDS)
            if (exitCode && process.exitValue() == 0) {
                Result.success(output.toString())
            } else {
                Result.failure(RuntimeException("Command failed with exit code: ${process.exitValue()}\n$output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pairs with a device using the given pairing code
     */
    fun pairDevice(ipAddress: String, port: Int, pairingCode: String): Result<String> {
        return executeCommand("pair", "$ipAddress:$port", pairingCode)
    }

    /**
     * Connects to a device using the IP address and port
     */
    fun connectDevice(ipAddress: String, port: Int): Result<String> {
        return executeCommand("connect", "$ipAddress:$port")
    }

    /**
     * Gets list of connected devices
     */
    fun getDevices(): Result<List<Pair<String, String>>> {
        val result = executeCommand("devices")
        return result.map { output ->
            output.lines()
                .filter { it.contains("\t") }
                .map {
                    val parts = it.split("\t")
                    parts[0].trim() to parts[1].trim() // Pair of device ID and status
                }
        }
    }

    fun getVersion(): Result<String> {
        val result = executeCommand("version")
        return result
    }
}