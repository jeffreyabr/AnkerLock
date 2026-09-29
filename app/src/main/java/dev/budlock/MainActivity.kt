package dev.budlock

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        status = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }
        val lockBtn = Button(this).apply { text = "Send lock now" }
        val uuidBtn = Button(this).apply { text = "Show device UUIDs" }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(TextView(this@MainActivity).apply {
                text = "Bud Lock\n\nAdd the \"Bud Lock\" tile from your Quick Settings edit panel. " +
                       "This screen is only needed once to grant permission, and for testing."
                textSize = 16f
            })
            addView(lockBtn); addView(uuidBtn); addView(status)
        })

        lockBtn.setOnClickListener {
            status.text = "Sending…"
            Thread {
                val r = SoundcoreLock.send(applicationContext)
                runOnUiThread {
                    status.text = if (r.isSuccess) "Sent ✓" else "Failed: ${r.exceptionOrNull()?.message}"
                }
            }.start()
        }
        uuidBtn.setOnClickListener { status.text = SoundcoreLock.knownUuids(this) }

        if (!SoundcoreLock.hasPermission(this)) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        status.text = if (SoundcoreLock.hasPermission(this)) "Permission granted ✓"
                      else "Permission denied — the tile won't work without it"
    }
}
