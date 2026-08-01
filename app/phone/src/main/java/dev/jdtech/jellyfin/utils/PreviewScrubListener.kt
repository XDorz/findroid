package dev.jdtech.jellyfin.utils

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.common.Player
import androidx.media3.ui.TimeBar
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import timber.log.Timber

class PreviewScrubListener(
    private val scrubbingPreview: ImageView,
    private val timeBarView: View,
    private val player: Player,
) : TimeBar.OnScrubListener {
    var currentTrickplay: Trickplay? = null
        set(value) {
            if (field !== value) currentBitMap = null
            field = value
        }

    private var currentBitMap: Bitmap? = null

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing started at $position")

        if (currentTrickplay == null) {
            return
        }

        scrubbingPreview.visibility = View.VISIBLE
        onScrubMove(timeBar, position)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        Timber.d("Scrubbing to $position")

        try {
            val trickplay = currentTrickplay ?: return
            if (trickplay.interval <= 0 || trickplay.images.isEmpty()) {
                scrubbingPreview.visibility = View.GONE
                return
            }

            val imageIndex =
                position.div(trickplay.interval).toInt().coerceIn(trickplay.images.indices)
            val image = trickplay.images[imageIndex]

            val parent = scrubbingPreview.parent as ViewGroup
            val previewWidth =
                scrubbingPreview.width.takeIf { it > 0 } ?: scrubbingPreview.layoutParams.width

            val offset = position.toFloat() / player.duration
            val minX = scrubbingPreview.left
            val maxX = parent.width - parent.paddingRight

            val startX =
                timeBarView.left + (timeBarView.right - timeBarView.left) * offset -
                    previewWidth / 2
            val endX = startX + previewWidth

            val layoutX =
                when {
                    startX >= minX && endX <= maxX -> startX
                    startX < minX -> minX
                    else -> maxX - previewWidth
                }.toFloat()

            scrubbingPreview.x = layoutX

            if (currentBitMap != image || scrubbingPreview.drawable == null) {
                scrubbingPreview.setImageBitmap(image)
                currentBitMap = image
            }
            scrubbingPreview.visibility = View.VISIBLE
        } catch (e: Exception) {
            scrubbingPreview.visibility = View.GONE
            Timber.e(e)
        }
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        Timber.d("Scrubbing stopped at $position")

        scrubbingPreview.visibility = View.GONE
    }
}
