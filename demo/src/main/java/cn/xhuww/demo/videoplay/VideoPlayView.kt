package cn.xhuww.demo.videoplay

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.support.annotation.RawRes
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import cn.xhuww.demo.R

import java.io.IOException
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min


class VideoPlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var mediaPlayer: MediaPlayer? = null
    private var playComplete = false

    private var existingSurfaceTexture: SurfaceTexture? = null
    private var onVideoSizeChangedListener: MediaPlayer.OnVideoSizeChangedListener =
        MediaPlayer.OnVideoSizeChangedListener { _, width, height ->
            scaleVideoSize(width, height)
        }
    private var onSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            //实现视频播放完成后切换至后台在切回来仍然显示最后一帧画面
            if (existingSurfaceTexture != null && playComplete && !isLooping) {
                surfaceTexture = existingSurfaceTexture
            }
            if (surface != null) {
                mediaPlayer?.setSurface(Surface(surface))
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            existingSurfaceTexture = surface
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }
    }

    var onCompletionListener: (mediaPlayer: MediaPlayer) -> Unit = {}
    var onErrorListener: (mediaPlayer: MediaPlayer, what: Int, extra: Int) -> Boolean =
        { _, _, _ -> false }

    var scaleType = ScaleType.NONE
        set(value) {
            field = value
            scaleVideoSize(videoWidth, videoHeight)
        }

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0

    val duration: Int
        get() = mediaPlayer?.duration ?: 0

    val videoHeight: Int
        get() = mediaPlayer?.videoHeight ?: 0

    val videoWidth: Int
        get() = mediaPlayer?.videoWidth ?: 0

    var isLooping: Boolean
        get() = mediaPlayer?.isLooping ?: false
        set(looping) {
            mediaPlayer?.isLooping = looping
        }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.VideoPlayView, 0, 0
        )
        val value = typedArray.getInt(R.styleable.VideoPlayView_scalableType, 0)
        scaleType = getScaleTypeByValue(value)
        typedArray.recycle()

        surfaceTextureListener = onSurfaceTextureListener
    }

    private fun openMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener(onErrorListener)
                setOnCompletionListener {
                    playComplete = true
                    onCompletionListener(it)
                }
                setOnVideoSizeChangedListener(onVideoSizeChangedListener)
            }
        } else {
            mediaPlayer!!.reset()
        }
    }

    private fun scaleVideoSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth == 0 || videoHeight == 0) {
            return
        }

        val videoSize = Size(videoWidth, videoHeight)
        val viewSize = Size(width, height)
        val matrix =
            getScaleMatrix(videoSize = videoSize, viewSize = viewSize, scaleType = scaleType)
        setTransform(matrix)
    }

    fun setDataSource(@RawRes id: Int) {
        val assetFileDescriptor = resources.openRawResourceFd(id)
        setDataSource(assetFileDescriptor)
    }

    fun setDataSource(path: String) {
        setDataSource(Uri.parse(path))
    }

    fun setDataSource(uri: Uri) {
        try {
            openMediaPlayer()
            mediaPlayer?.setDataSource(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setDataSource(afd: AssetFileDescriptor) {
        try {
            openMediaPlayer()
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun prepare(onPreparedListener: (mediaPlayer: MediaPlayer) -> Unit = {}) {
        try {
            playComplete = false
            mediaPlayer?.setOnPreparedListener(onPreparedListener)
            mediaPlayer?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start() {
        playComplete = false
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer?.setVolume(leftVolume, rightVolume)
    }

    fun stop() {
        mediaPlayer?.stop()
    }

    fun release() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    private fun getScaleTypeByValue(value: Int): ScaleType = when (value) {
        1 -> ScaleType.FIT_START
        2 -> ScaleType.FIT_CENTER
        3 -> ScaleType.FIT_END
        4 -> ScaleType.FIT_XY
        5 -> ScaleType.CENTER
        6 -> ScaleType.CENTER_CROP
        7 -> ScaleType.CENTER_INSIDE
        else -> ScaleType.NONE
    }

    enum class ScaleType {
        NONE,

        FIT_START,
        FIT_CENTER,
        FIT_END,
        FIT_XY,

        CENTER,
        CENTER_CROP,
        CENTER_INSIDE;
    }

    internal inner class Size(val width: Int, val height: Int) {
        fun contains(size: Size): Boolean = width >= size.width && height >= size.height
    }

    private fun getScaleMatrix(videoSize: Size, viewSize: Size, scaleType: ScaleType): Matrix {
        return when (scaleType) {
            ScaleType.FIT_XY -> fitXY()
            ScaleType.FIT_CENTER -> fitCenter(videoSize = videoSize, viewSize = viewSize)
            ScaleType.FIT_START -> fitStart(videoSize, viewSize)
            ScaleType.FIT_END -> fitEnd(videoSize, viewSize)

            ScaleType.CENTER -> center(videoSize, viewSize)
            ScaleType.CENTER_CROP -> centerCrop(videoSize, viewSize)
            ScaleType.CENTER_INSIDE -> centerInside(videoSize, viewSize)
            else -> noScale(videoSize, viewSize)
        }
    }

    /**
     * 按原比例缩放 到View的宽或高度，居中显示
     *
     * 轴心点 (px, py): View中间
     */
    private fun fitCenter(videoSize: Size, viewSize: Size): Matrix {
        val scalePair = getFitScale(videoSize, viewSize)

        val px = viewSize.width / 2f
        val py = viewSize.height / 2f
        return getMatrix(scalePair.first, scalePair.second, px, py)
    }

    /**
     * 按比例缩放 显示在View的上部分位置
     *
     * 轴心点：屏幕左上角（0，0）
     */
    private fun fitStart(videoSize: Size, viewSize: Size): Matrix {
        val scalePair = getFitScale(videoSize, viewSize)
        return getMatrix(scalePair.first, scalePair.second, 0f, 0f)
    }

    /**
     * 按比例缩放 显示在View的下部分位置
     *
     * 轴心点：屏幕右下角（width，height）
     */
    private fun fitEnd(videoSize: Size, viewSize: Size): Matrix {
        val scalePair = getFitScale(videoSize, viewSize)
        return getMatrix(
            scalePair.first, scalePair.second, viewSize.width.toFloat(), viewSize.height.toFloat()
        )
    }

    /**
     * 不按比例缩放 视频充满View的大小显示 sx = 1; sy =1
     * 轴心点：屏幕左上角（0，0）
     */
    private fun fitXY(): Matrix = getMatrix(1f, 1f, 0f, 0f)

    /**
     * 不缩放，按原视频大小居中显示，当视频大小大于View大小时截取中间部分显示
     *
     * 轴心点 (px, py): View中间
     */
    private fun center(videoSize: Size, viewSize: Size): Matrix {
        val sx = videoSize.width / viewSize.width.toFloat()
        val sy = videoSize.height / viewSize.height.toFloat()
        val px = viewSize.width / 2f
        val py = viewSize.height / 2f

        return getMatrix(sx, sy, px, py)
    }

    /**
     * 按比例缩放 将图片的内容完整居中显示: 视频大小 <= View大小
     *
     * 轴心点 (px, py): View中间
     */
    private fun centerInside(videoSize: Size, viewSize: Size): Matrix {
        return if (viewSize.contains(videoSize)) {
            center(videoSize, viewSize)
        } else {
            fitCenter(videoSize, viewSize)
        }
    }

    /**
     * 按比例缩放 从视频中间缩放，直到充满屏幕 视频会被裁剪
     *
     * 轴心点 (px, py): View中间
     */
    private fun centerCrop(videoSize: Size, viewSize: Size): Matrix {
        var sx = videoSize.width / viewSize.width.toFloat()
        var sy = videoSize.height / viewSize.height.toFloat()
        val px = viewSize.width / 2f
        val py = viewSize.height / 2f

        max(sx, sy).also {
            sx = it / sx
            sy = it / sy
        }
        return getMatrix(sx, sy, px, py)
    }

    /**
     * 未设置Scale 则按照比较合理的 fitCenter来显示
     */
    private fun noScale(videoSize: Size, viewSize: Size): Matrix = fitCenter(videoSize, viewSize)

    private fun getMatrix(sx: Float, sy: Float, px: Float, py: Float): Matrix =
        Matrix().apply { setScale(sx, sy, px, py) }

    /**
     * 缩放比 sx,sy 填充模式
     */
    private fun getFitScale(videoSize: Size, viewSize: Size): Pair<Float, Float> {
        var sx = viewSize.width.toFloat() / videoSize.width
        var sy = viewSize.height.toFloat() / videoSize.height

        min(sx, sy).also {
            sx = it / sx
            sy = it / sy
        }
        return Pair(sx, sy)
    }
}

