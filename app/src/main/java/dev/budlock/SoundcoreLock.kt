package dev.budlock

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import java.util.UUID

object SoundcoreLock {

    // ---- Configuration -------------------------------------------------
    const val MAC = "90:A0:BE:D3:19:85"

    /** RFCOMM channel seen in the HCI snoop log. Used when SERVICE_UUID is null. */
    private const val CHANNEL = 6

    /**
     * Optional: the vendor UUID-128 from the SDP exchange (or from the
     * "Show device UUIDs" button in the app). When set, the app uses the
     * public createRfcommSocketToServiceRecord() API instead of reflection,
     * which is more robust against Android and firmware changes.
     * Example: "0cf12d31-fac3-4553-bd80-d6832e7b3952"
     */
    private val SERVICE_UUID: String? = null

    const val LOCK_HEX = "08ee00000001010a0002"
    // ---------------------------------------------------------------------

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @SuppressLint("MissingPermission")
    private fun device(ctx: Context): BluetoothDevice {
        check(hasPermission(ctx)) { "Nearby devices permission not granted — open Bud Lock" }
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("No Bluetooth adapter")
        check(adapter.isEnabled) { "Bluetooth is off" }
        val dev = adapter.getRemoteDevice(MAC)
        check(dev.bondState == BluetoothDevice.BOND_BONDED) { "Earbuds not paired" }
        adapter.cancelDiscovery()
        return dev
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(dev: BluetoothDevice): BluetoothSocket =
        if (SERVICE_UUID != null) {
            dev.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
        } else {
            dev.javaClass
                .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                .invoke(dev, CHANNEL) as BluetoothSocket
        }

    /** Blocking — call from a background thread. */
    @SuppressLint("MissingPermission")
    fun send(ctx: Context, hex: String = LOCK_HEX): Result<Unit> = runCatching {
        val dev = device(ctx)
        val payload = hexToBytes(hex)
        var lastError: Exception? = null
        repeat(2) { attempt ->   // Android RFCOMM connects are occasionally flaky; retry once
            try {
                openSocket(dev).use { sock ->
                    sock.connect()
                    sock.outputStream.write(payload)
                    sock.outputStream.flush()
                    Thread.sleep(400)   // let the UIH frame go out before closing
                }
                return@runCatching
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0) Thread.sleep(600)
            }
        }
        throw lastError ?: IllegalStateException("Unknown error")
    }

    /** Cached SDP UUIDs the phone knows for the earbuds. */
    @SuppressLint("MissingPermission")
    fun knownUuids(ctx: Context): String = runCatching {
        device(ctx).uuids?.joinToString("\n") { it.uuid.toString() } ?: "(none cached)"
    }.getOrElse { "Error: ${it.message}" }
}
