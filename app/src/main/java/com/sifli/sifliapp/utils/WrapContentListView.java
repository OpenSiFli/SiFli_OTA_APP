package com.sifli.sifliapp.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * @author hecq
 * @email 33912760@qq.com
 * create at 2025/1/15
 * description
 */

public class WrapContentListView extends ListView {
    public WrapContentListView(Context context) {
        super(context);
    }

    public WrapContentListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapContentListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
