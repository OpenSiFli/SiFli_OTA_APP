package com.sifli.sifliapp.modules.debug.guibuilder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.debug.guibuilder.model.MyWatchfaceEditItem;
import com.sifli.siflicore.log.SFLog;

import java.util.List;

/**
 * @author hecq
 * @email 33912760@qq.com
 * create at 2024/1/6
 * description
 */
public class SFGUIBuilderImageAdapter extends BaseAdapter implements  View.OnClickListener {
    private final static String TAG = "SFOtaImageAdapter";
    private List<MyWatchfaceEditItem> list;
    private Context context = null;
    ISFGUIBuilderImageAdapterCallback callback;
    public SFGUIBuilderImageAdapter(Context context, List<MyWatchfaceEditItem> list, ISFGUIBuilderImageAdapterCallback callback){
        this.context = context;
        this.list = list;
        this.callback = callback;
    }
    @Override
    public int getCount() {
        return this.list.size();
    }

    @Override
    public Object getItem(int position) {
        return this.list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mHolder;
        if(convertView == null){
            mHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.row_sf_guibuilder_image_item, null, true);
            mHolder.fileNameTv = (TextView) convertView.findViewById(R.id.sf_guibuilder_item_name_tv);
            mHolder.statusTv = (TextView) convertView.findViewById(R.id.sf_guibuilder_item_status_tv);
            mHolder.originImageIv = (ImageView) convertView.findViewById(R.id.sf_guibuilder_item_origin_image_iv);
            mHolder.patchImageIv = (ImageView) convertView.findViewById(R.id.sf_guibuilder_item_patch_image_iv);
            mHolder.editBtn = (Button) convertView.findViewById(R.id.sf_guibuilder_item_edit_btn);
            mHolder.delBtn = (Button) convertView.findViewById(R.id.sf_guibuilder_item_delete_btn);
            mHolder.sendBtn = (Button) convertView.findViewById(R.id.sf_guibuilder_item_send_btn);
            convertView.setTag(mHolder);
            mHolder.editBtn.setOnClickListener(this);
            mHolder.delBtn.setOnClickListener(this);
            mHolder.sendBtn.setOnClickListener(this);
        }else{
            mHolder = (ViewHolder) convertView.getTag();
        }
        MyWatchfaceEditItem item = list.get(position);
        mHolder.fileNameTv.setText(item.getEditItem().getControlId());
        mHolder.statusTv.setText(this.getStatus(item));
        mHolder.originImageIv.setImageBitmap(item.getEditItem().getOriginImage());
        mHolder.patchImageIv.setImageBitmap(item.getEditItem().getPatchImage());
        mHolder.editBtn.setTag(item);
        mHolder.delBtn.setTag(item);
        mHolder.sendBtn.setTag(item);
        return convertView;
    }

    @Override
    public void onClick(View v) {
        SFLog.i(TAG,"onClick");
        int viewId = v.getId();
        if(viewId == R.id.sf_guibuilder_item_edit_btn){
            if(callback != null){
                callback.onEditBtnTouch((MyWatchfaceEditItem)v.getTag(),v);
            }
        }else if(viewId == R.id.sf_guibuilder_item_delete_btn){
            if(callback != null){
                callback.onDelBtnTouch((MyWatchfaceEditItem)v.getTag(),v);
            }
        }else if(viewId == R.id.sf_guibuilder_item_send_btn){
            if(callback != null){
                callback.onSendBtnTouch((MyWatchfaceEditItem)v.getTag(),v);
            }
        }
    }

    private String getStatus(MyWatchfaceEditItem item){
        String status = "";
        if(item.getEditItem().hasPatch()){
            status = "已修改";
        }
        if(item.isHasSend()){
            status = "已发送";
        }
        return status;
    }

    class ViewHolder{
        private TextView fileNameTv;
        private TextView statusTv;
        private ImageView originImageIv;
        private ImageView patchImageIv;
        private Button editBtn;
        private Button delBtn;
        private Button sendBtn;
    }
}
