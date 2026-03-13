package com.sifli.sifliapp.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

import com.sifli.siflicore.log.SFLog;

/**
 * SFScrollView是用来干什么的
 * 解决ScrollView滑动冲突问题
 *
 * @author wangyao
 * @email 382708580@qq.com
 * @create 2025/5/8
 */
public class SFScrollView extends ScrollView {
    private static final String TAG = "SFScrollView";

    public SFScrollView(Context context) {
        super(context);
    }

    public SFScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SFScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SFScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private float lastY;

    /**
     * 是否拦截事件
     *
     * @param ev ACTION_DOWN ACTION_UP ACTION_MOVE
     * @return boolean
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ScrollView parentScrollView = findParentScrollView(getParent());
        SFLog.i(TAG, parentScrollView + "," + ev.getAction());
        if (parentScrollView == null) {
            return super.onInterceptTouchEvent(ev);
        }
        SFLog.i(TAG, parentScrollView + " id=" + parentScrollView.getId() + ",getScrollY=" + getScrollY());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            lastY = ev.getRawY();
            SFLog.i(TAG, "getChildAt(0).getHeight()==" + getChildAt(0).getHeight() + " getHeight()=" + getHeight());
            if (getChildAt(0).getHeight() <= getHeight()) { // 子View高度不足滑动
                return false;
            }
            SFLog.i(TAG, "进入点击下");
            parentScrollView.requestDisallowInterceptTouchEvent(true);
            return true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            SFLog.i(TAG, "抬起");
            parentScrollView.requestDisallowInterceptTouchEvent(false);
            return false;
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            SFLog.i(TAG, "移动");
            float currentY = ev.getRawY();
            boolean isSlidingUp = currentY > lastY; // 向上滑动
            boolean isSlidingDown = currentY < lastY; // 向下滑动
            lastY = currentY;
            // 判断是否允许父ScrollView拦截事件
            if (isSlidingUp && isScrolledToBottom()) {
                parentScrollView.requestDisallowInterceptTouchEvent(false);
                return false;
            } else if (isSlidingDown && isScrolledToTop()) {
                parentScrollView.requestDisallowInterceptTouchEvent(false);
                return false;
            } else {
                parentScrollView.requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    private ScrollView findParentScrollView(ViewParent view) {
        while (view != null && !(view instanceof ScrollView)) {
            view = view.getParent();
        }
        return view != null ? (ScrollView) view : null;
    }

    /**
     * 判断滑动是否到达底部
     *
     * @return boolean
     */
    private boolean isScrolledToBottom() {
        // 判断是否滚动到底部
        if (getChildCount() == 0) return false;
        View child = getChildAt(0);
        return getScrollY() != 0 && getScrollY() >= child.getHeight() - getHeight();
    }

    /**
     * 判断滑动是否到达顶部
     *
     * @return boolean
     */
    private boolean isScrolledToTop() {
        // 判断是否滚动到顶部
        return getScrollY() <= 0;
    }
}
