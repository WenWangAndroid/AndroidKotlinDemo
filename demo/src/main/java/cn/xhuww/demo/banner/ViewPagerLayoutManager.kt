package cn.xhuww.demo.banner

import android.graphics.PointF
import android.support.v7.widget.*
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup

class ViewPagerLayoutManager : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {

    private val orientationHelper = OrientationHelper.createHorizontalHelper(this)

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }

        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return PointF(direction.toFloat(), 0f)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
        RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)

        //分离并且回收当前附加的所有View
        detachAndScrapAttachedViews(recycler)

        if (itemCount == 0) return

        var start = orientationHelper.startAfterPadding
        for (i in 0 until itemCount) {
            val child = recycler.getViewForPosition(i)
            layout(child, start, isForwardDirection = true)
            start = orientationHelper.getDecoratedEnd(child)

            if (start > orientationHelper.endAfterPadding) {
                break
            }
        }
    }

    override fun canScrollHorizontally(): Boolean = true

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount == 0 || dx == 0) return 0

        recycleViews(dx, recycler)
        fill(dx, recycler)
        orientationHelper.offsetChildren(-dx)

        return dx
    }

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        var remeasureWidthSpec = widthSpec
        var remeasureHeightSpec = heightSpec

        val widthMode = MeasureSpec.getMode(widthSpec)
        val heightMode = MeasureSpec.getMode(heightSpec)

        if (widthMode == MeasureSpec.AT_MOST) {
            var measureWidth = MeasureSpec.getSize(widthSpec)
            val itemWidthSum = (0 until itemCount)
                .mapNotNull { recycler.getViewForPosition(it) }
                .map { measureChildView(it, widthSpec, heightSpec).first }
                .sum()

            if (itemWidthSum < measureWidth) {
                measureWidth = itemWidthSum
            }
            remeasureWidthSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY)
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            var measureHeight = MeasureSpec.getSize(heightSpec)
            val itemMaxHeight = (0 until itemCount)
                .mapNotNull { recycler.getViewForPosition(it) }
                .map { measureChildView(it, widthSpec, heightSpec).second }
                .max() ?: 0

            if (itemMaxHeight < measureHeight) {
                measureHeight = itemMaxHeight
            }
            remeasureHeightSpec = MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(recycler, state, remeasureWidthSpec, remeasureHeightSpec)
    }

    private fun measureChildView(childView: View, widthSpec: Int, heightSpec: Int): Pair<Int, Int> {
        val layoutParams = childView.layoutParams as RecyclerView.LayoutParams
        val childHeightSpec = ViewGroup.getChildMeasureSpec(
            heightSpec, paddingTop + paddingBottom, layoutParams.height
        )
        childView.measure(widthSpec, childHeightSpec)

        val width = childView.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
        val height = childView.measuredHeight + layoutParams.bottomMargin + layoutParams.topMargin

        return Pair(width, height)
    }

    private fun fill(dx: Int, recycler: RecyclerView.Recycler) {
        while (true) {
            var start: Int
            var currentView: View
            val isForwardDirection = dx > 0

            if (isForwardDirection) {
                val lastVisibleView = getChildAt(childCount - 1) ?: break
                start = orientationHelper.getDecoratedEnd(lastVisibleView)
                if (start - dx > orientationHelper.endAfterPadding) break

                currentView = lastVisibleView
            } else {
                val firstVisibleView = getChildAt(0) ?: break
                start = orientationHelper.getDecoratedStart(firstVisibleView)
                if (start - dx < orientationHelper.startAfterPadding) break

                currentView = firstVisibleView
            }

            val nextView = nextView(currentView, isForwardDirection, recycler) ?: break
            layout(nextView, start, isForwardDirection)
        }
    }

    private fun nextView(currentView: View, isForwardDirection: Boolean, recycler: RecyclerView.Recycler): View? {
        val endPosition = itemCount - 1
        val currentPosition = getPosition(currentView)
        val nextViewPosition: Int = if (isForwardDirection) {
            if (currentPosition == endPosition) 0 else currentPosition + 1
        } else {
            if (currentPosition == 0) endPosition else currentPosition - 1
        }
        return recycler.getViewForPosition(nextViewPosition)
    }

    private fun layout(view: View, start: Int, isForwardDirection: Boolean) {
        //测量View 包含 decorations(装饰物/分割线) 和 margins
        measureChildWithMargins(view, 0, 0)
        //View占用的空间
        val childWidth = orientationHelper.getDecoratedMeasurement(view)
        val childHeight = orientationHelper.getDecoratedMeasurementInOther(view)

        //绘制View
        val left: Int
        val right: Int
        val top = paddingTop
        val bottom = top + childHeight
        if (isForwardDirection) {
            addView(view)
            left = start
            right = start + childWidth
        } else {
            addView(view, 0)
            left = start - childWidth
            right = start
        }
        layoutDecoratedWithMargins(view, left, top, right, bottom)
        orientationHelper.onLayoutComplete()
    }

    private fun recycleViews(dx: Int, recycler: RecyclerView.Recycler) {
        for (i in 0 until itemCount) {
            val childView = getChildAt(i) ?: return
            //左滑
            if (dx > 0) {
                //移除并回收 原点 左侧的子View
                if (orientationHelper.getDecoratedEnd(childView) - dx < orientationHelper.startAfterPadding) {
                    removeAndRecycleViewAt(i, recycler)
                }
            } else { //右滑
                //移除并回收 右侧即RecyclerView宽度之以外的子View
                if (orientationHelper.getDecoratedStart(childView) - dx > orientationHelper.endAfterPadding) {
                    removeAndRecycleViewAt(i, recycler)
                }
            }
        }
    }
}
