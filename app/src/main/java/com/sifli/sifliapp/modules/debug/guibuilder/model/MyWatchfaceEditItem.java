package com.sifli.sifliapp.modules.debug.guibuilder.model;

import com.sifli.sifliguibuildersdk.editor.SGImageEditItem;

/**
 * @author hecq
 * @email 33912760@qq.com
 * create at 2026/1/16
 * description
 */
public class MyWatchfaceEditItem {
    private boolean hasSend;
    private SGImageEditItem editItem;

    public MyWatchfaceEditItem(SGImageEditItem imageEditItem) {
        this.editItem = imageEditItem;
    }

    public boolean isHasSend() {
        return hasSend;
    }


    public SGImageEditItem getEditItem() {
        return editItem;
    }


    public void setHasSend(boolean hasSend) {
        this.hasSend = hasSend;
    }

    public String getPatchFileName(){
        return String.format("%s.bin",editItem.getHexUID());
    }

}
