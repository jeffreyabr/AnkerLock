package dev.budlock

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class LockTileService : TileService() {

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var busy = false

    override fun onStartListening() {
        qsTile?.apply {
            if (!busy) { state = Tile.STATE_INACTIVE; subtitle = "Lock touch" }
            updateTile()
        }
    }

    override fun onClick() {
        if (busy) return
        busy = true
        qsTile?.apply { state = Tile.STATE_ACTIVE; subtitle = "Sending…"; updateTile() }

        Thread {
            val result = SoundcoreLock.send(applicationContext)
            main.post {
                busy = false
                val msg = if (result.isSuccess) "Touch controls locked"
                          else "Failed: ${result.exceptionOrNull()?.message}"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    subtitle = if (result.isSuccess) "Locked ✓" else "Failed — tap to retry"
                    updateTile()
                }
            }
        }.start()
    }
}
