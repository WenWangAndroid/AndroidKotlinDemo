package cn.xhuww.demo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import cn.xhuww.demo.videoplay.VideoPlayActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startActivity(Intent(this, VideoPlayActivity::class.java))
    }
}
