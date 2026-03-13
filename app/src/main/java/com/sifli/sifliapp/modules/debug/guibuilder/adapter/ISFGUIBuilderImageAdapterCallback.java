package com.sifli.sifliapp.modules.debug.guibuilder.adapter;

import android.view.View;

import com.sifli.sifliapp.modules.debug.guibuilder.model.MyWatchfaceEditItem;


/**
 * @author hecq
 * @email 33912760@qq.com
 * create at 2024/1/6
 * description
 */
public interface ISFGUIBuilderImageAdapterCallback {
    void onEditBtnTouch(MyWatchfaceEditItem item, View view);
    void onDelBtnTouch(MyWatchfaceEditItem item, View view);
    void onSendBtnTouch(MyWatchfaceEditItem item, View view);
}
