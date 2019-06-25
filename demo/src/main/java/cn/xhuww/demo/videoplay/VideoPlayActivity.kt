package cn.xhuww.demo.videoplay

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import cn.xhuww.demo.R
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)

        videoPlayView.setDataSource(R.raw.sample_video_1280x720)
        videoPlayView.setVolume(0f, 0f)
        videoPlayView.isLooping = false
        videoPlayView.onCompletionListener = {}
        videoPlayView.prepare { it.start() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.video_scale_type, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val scaleType = when (item.itemId) {
            R.id.fitStart -> VideoPlayView.ScaleType.FIT_START
            R.id.fitCenter -> VideoPlayView.ScaleType.FIT_CENTER
            R.id.fitEnd -> VideoPlayView.ScaleType.FIT_END
            R.id.fitXY -> VideoPlayView.ScaleType.FIT_XY
            R.id.center -> VideoPlayView.ScaleType.CENTER
            R.id.centerCrop -> VideoPlayView.ScaleType.CENTER_CROP
            R.id.centerInside -> VideoPlayView.ScaleType.CENTER_INSIDE
            else -> VideoPlayView.ScaleType.NONE
        }
        videoPlayView.scaleType = scaleType
        videoPlayView.invalidate()
        return super.onOptionsItemSelected(item)
    }
}
