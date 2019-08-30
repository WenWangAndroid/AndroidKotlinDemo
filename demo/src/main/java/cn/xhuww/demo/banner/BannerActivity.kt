package cn.xhuww.demo.banner

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.view.Menu
import android.view.MenuItem
import cn.xhuww.demo.R
import kotlinx.android.synthetic.main.activity_banner.*

class BannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banner)

        val imageAdapter = ImageAdapter().apply {
            items = arrayListOf(
                    R.mipmap.image_page_1,
                    R.mipmap.image_page_2,
                    R.mipmap.image_page_3,
                    R.mipmap.image_page_4
            )
        }
        recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycleView.adapter = imageAdapter

        PagerSnapHelper().attachToRecyclerView(recycleView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.banner_type, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_normal_banner -> {
            }
            R.id.menu_loop_banner -> {
            }
        }
        return true
    }
}
