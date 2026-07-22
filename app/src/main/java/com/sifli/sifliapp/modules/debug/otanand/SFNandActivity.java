package com.sifli.sifliapp.modules.debug.otanand;

import static com.sifli.siflidfu.Protocol.IMAGE_ID_CTRL;
import static com.sifli.siflidfu.Protocol.IMAGE_ID_NAND_RES;
import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_LOG;
import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_PROGRESS;
import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_STATE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_PROGRESS;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_PROGRESS_TYPE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_STATE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_STATE_RESULT;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_LOG_MESSAGE;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.debug.ota.ISFOtaImageAdapterCallback;
import com.sifli.sifliapp.modules.debug.ota.SFOtaImageAdapter;
import com.sifli.sifliapp.modules.debug.ota.SFOtaImageItem;
import com.sifli.sifliapp.modules.debug.ota.SFOtaImageTypeItem;

import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.sifliapp.utils.StringUtil;
import com.sifli.sifliapp.utils.speedview.SpeedView;

import com.sifli.siflicore.log.SFLog;

import com.sifli.siflidfu.DFUImagePath;
import com.sifli.siflidfu.ISifliDFUService;
import com.sifli.siflidfu.Protocol;
import com.sifli.siflidfu.SifliDFUService;


import java.io.File;
import java.util.ArrayList;

public class SFNandActivity extends AppCompatActivity implements View.OnClickListener, ISFOtaImageAdapterCallback {
    private final static String TAG = "SFNandActivity";
    public final static String EXTRA_BLE_DEVICE = "EXTRA_BLE_DEVICE";
    private final static int COMMAND_SELECT_RES_FILE = 1;
    private final static int COMMAND_SELECT_CTRL_FILE = 2;
    private final static int COMMAND_SELECT_IMAGE_FILE = 3;

    private TextView macAddressTv;
    private TextView logTv;
    private ScrollView logSv;
    private TextView progressTv;
    private ProgressBar progressBar;

    private Button selectResBtn;
    private Button selectCtrlBtn;
    private Button selectImageBtn;

    private TextView resTv;
    private TextView ctrlTv;
    private ListView imageListView;

    private Button startBtn;
    private Button resumeBtn;
    private Button stopBtn;
    private EditText frequencyEt;
    private TextView speedTv;

    private String resFilePath;
    private String ctrlFilePath;
    private String targetMac = "FF:FF:79:DA:77:C8";//525

    private ArrayList<SFOtaImageTypeItem> imageTypeItems;
    private ArrayList<SFOtaImageItem> dataSource;
    private SFOtaImageAdapter imageAdapter;
    private BasePopupView rightMenuPop = null;
    private SFOtaImageItem currentImageItem;
//    private SFOtaManager otaManager;
    private StringBuilder logBuilder;
    private SpeedView speedView;

    private BroadcastReceiver localBroadcastReceiver;
    private SifliDFUService.SifliDFUBinder mBinder;
    private ISifliDFUService sifliDFUService;
    // 定义一个布尔值，用来标记服务是否绑定
    private boolean isBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (SifliDFUService.SifliDFUBinder) service;
            sifliDFUService = mBinder.getDfuService();
            isBound = true;
            Log.i(TAG,"onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"onServiceDisconnected");
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sfnand);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setupView();
        this.bindEvent();
        this.init();

        this.imageAdapter = new SFOtaImageAdapter(this,this.dataSource,this);
        this.imageListView.setAdapter(this.imageAdapter);
        this.speedView = new SpeedView();

        localBroadcastReceiver = new SFNandActivity.LocalBroadcastReceiver();
        registerDfuLocalBroadcast();
        // 创建一个 Intent 对象，指定要绑定的服务
        Intent serviceIntent = new Intent(this, SifliDFUService.class);
        // 调用 bindService() 方法，传入 Intent 对象，ServiceConnection 对象，和绑定模式
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(localBroadcastReceiver);
        if(isBound){
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void setupView(){
        macAddressTv = findViewById(R.id.sf_ota_nand_mac_tv);
        logTv = findViewById(R.id.sf_ota_nand_log_tv);
        logSv = findViewById(R.id.sf_ota_nand_log_sv);
        progressTv = findViewById(R.id.sf_ota_nand_progress_tv);
        progressBar = findViewById(R.id.sf_ota_nand_progress_pb);

        selectResBtn = findViewById(R.id.sf_ota_nand_select_res_btn);
        selectCtrlBtn = findViewById(R.id.sf_ota_nand_select_ctrl_btn);
        selectImageBtn = findViewById(R.id.sf_ota_nand_select_image_btn);

        resTv = findViewById(R.id.sf_ota_nand_res_tv);
        ctrlTv = findViewById(R.id.sf_ota_nand_ctrl_tv);
        imageListView =findViewById(R.id.sf_ota_nand_image_lv);
        frequencyEt = findViewById(R.id.sf_ota_nand_frequency_tv);

        startBtn = findViewById(R.id.sf_ota_nand_start_btn);
        resumeBtn = findViewById(R.id.sf_ota_nand_resume_btn);
        stopBtn = findViewById(R.id.sf_ota_nand_stop_btn);
        speedTv = findViewById(R.id.sf_ota_nand_speed_tv);
    }

    private void bindEvent(){
        this.selectResBtn.setOnClickListener(this);
        this.selectCtrlBtn.setOnClickListener(this);
        this.selectImageBtn.setOnClickListener(this);

        this.startBtn.setOnClickListener(this);
        this.resumeBtn.setOnClickListener(this);
        this.stopBtn.setOnClickListener(this);

    }

    private void init(){
        String mac = getIntent().getStringExtra(EXTRA_BLE_DEVICE);
        if(mac != null){
            targetMac = mac;
        }
        this.macAddressTv.setText(targetMac);
        this.logBuilder = new StringBuilder();
        this.imageTypeItems = new ArrayList<>();
        this.dataSource = new ArrayList<>();
        SFOtaImageTypeItem hcpuItem = new SFOtaImageTypeItem();
        hcpuItem.setType(Protocol.IMAGE_ID_HCPU);
        hcpuItem.setName("HCPU");
        this.imageTypeItems.add(hcpuItem);

        SFOtaImageTypeItem lcpuItem = new SFOtaImageTypeItem();
        lcpuItem.setType(Protocol.IMAGE_ID_LCPU);
        lcpuItem.setName("LCPU");
        this.imageTypeItems.add(lcpuItem);

        SFOtaImageTypeItem patchItem = new SFOtaImageTypeItem();
        patchItem.setType(Protocol.IMAGE_ID_NOR_LCPU_PATCH);
        patchItem.setName("PATCH");
        this.imageTypeItems.add(patchItem);

        SFOtaImageTypeItem resItem = new SFOtaImageTypeItem();
        resItem.setType(Protocol.IMAGE_ID_RES);
        resItem.setName("FS_ROOT");
        this.imageTypeItems.add(resItem);

        SFOtaImageTypeItem LCPU_PATCH = new SFOtaImageTypeItem();
        LCPU_PATCH.setType(Protocol.IMAGE_ID_NAND_LCPU_PATCH);
        LCPU_PATCH.setName("LCPU_PATCH");
        this.imageTypeItems.add(LCPU_PATCH);

        SFOtaImageTypeItem DYN = new SFOtaImageTypeItem();
        DYN.setType(Protocol.IMAGE_ID_DYN);
        DYN.setName("DYN");
        this.imageTypeItems.add(DYN);

        SFOtaImageTypeItem MUSIC = new SFOtaImageTypeItem();
        MUSIC.setType(Protocol.IMAGE_ID_MUSIC);
        MUSIC.setName("MUSIC");
        this.imageTypeItems.add(MUSIC);

        SFOtaImageTypeItem PIC = new SFOtaImageTypeItem();
        PIC.setType(Protocol.IMAGE_ID_NAND_PIC);
        PIC.setName("PIC");
        this.imageTypeItems.add(PIC);

        SFOtaImageTypeItem FONT = new SFOtaImageTypeItem();
        FONT.setType(Protocol.IMAGE_ID_NAND_FONT);
        FONT.setName("FONT");
        this.imageTypeItems.add(FONT);

        SFOtaImageTypeItem RING = new SFOtaImageTypeItem();
        RING.setType(Protocol.IMAGE_ID_NAND_RING);
        RING.setName("RING");
        this.imageTypeItems.add(RING);

        SFOtaImageTypeItem LANG = new SFOtaImageTypeItem();
        LANG.setType(Protocol.IMAGE_ID_NAND_LANG);
        LANG.setName("LANG");
        this.imageTypeItems.add(LANG);
    }

    class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BROADCAST_DFU_PROGRESS:
                    int progress = intent.getIntExtra(EXTRA_DFU_PROGRESS, 0);
                    int type = intent.getIntExtra(EXTRA_DFU_PROGRESS_TYPE, 0);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progress);
                            progressTv.setText("pro" + progress);
                        }
                    });
                    //Log.i(TAG, "dfu progress " + progress);
                    break;
                case BROADCAST_DFU_LOG:
                    String log = intent.getStringExtra(EXTRA_LOG_MESSAGE);
                    addLog(log);
                    SFLog.i(TAG,log);
                    //Log.d(TAG, "DFU LOG - " + DFULog);
//                    updateLogText(DFULog);
                    break;
                case BROADCAST_DFU_STATE:
                    int dfuState = intent.getIntExtra(EXTRA_DFU_STATE, 0);
                    int dfuStateResult = intent.getIntExtra(EXTRA_DFU_STATE_RESULT, 0);
                    String msg = "dfuState=" + dfuState + ",dfuStateResult=" + dfuStateResult;
                    SFLog.i(TAG,msg);
//                    addLog(msg);
                    break;
            }
        }
    }
    private void registerDfuLocalBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_DFU_LOG);
        intentFilter.addAction(BROADCAST_DFU_STATE);
        intentFilter.addAction(BROADCAST_DFU_PROGRESS);
        // more action

        registerLocalReceiver(localBroadcastReceiver, intentFilter);
    }
    public void registerLocalReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(receiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null)return;
        if(requestCode == COMMAND_SELECT_RES_FILE){
            Uri uri = data.getData();
            Log.d(TAG, "res uri " + uri);
            this.resFilePath = FileUtil.getFilePathFromURI(this,uri);
            String fileName = FileUtil.getUrlName(uri,this);
            this.resTv.setText(fileName);
            SFLog.i(TAG,"filepath=" + this.resFilePath);
        }else if(requestCode == COMMAND_SELECT_CTRL_FILE){
            Uri uri = data.getData();
            Log.d(TAG, "ctrl uri " + uri);
            this.ctrlFilePath = FileUtil.getFilePathFromURI(this,uri);
            String fileName = FileUtil.getUrlName(uri,this);
            this.ctrlTv.setText(fileName);
        }else if(requestCode == COMMAND_SELECT_IMAGE_FILE){
            Uri uri = data.getData();
            Log.d(TAG, "ctrl uri " + uri);
            String imageFilePath = FileUtil.getFilePathFromURI(this,uri);
            String fileName = FileUtil.getUrlName(uri,this);
            SFOtaImageItem item = new SFOtaImageItem();
            item.setLocalPath(imageFilePath);
            item.setFileName(fileName);
            this.dataSource.add(item);
            this.imageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId == R.id.sf_ota_nand_select_res_btn){
            this.onSelectResBtnTouch();
        }else if(viewId == R.id.sf_ota_nand_select_ctrl_btn){
            this.onSelectCtrlBtnTouch();
        }else if(viewId == R.id.sf_ota_nand_select_image_btn){
            this.onSelectImageBtnTouch();
        }else if(viewId == R.id.sf_ota_nand_start_btn){
            this.onStartBtnTouch();
        }else if(viewId == R.id.sf_ota_nand_resume_btn){
            this.onResumeBtnTouch();
        }else if(viewId == R.id.sf_ota_nand_stop_btn){
            this.onStopBtnTouch();
        }
    }

    @Override
    public void onSelectTypeBtnTouch(SFOtaImageItem item,View v) {
        SFLog.i(TAG,"onSelectTypeBtnTouch");
        this.currentImageItem = item;
        this.showImageTypeMenu(item, v);
    }

    @Override
    public void onRemoveBtnTouch(SFOtaImageItem item, View view) {
        this.dataSource.remove(item);
        this.imageAdapter.notifyDataSetChanged();
    }


    private void addLog(String log){
        String ts = StringUtil.getTimeStr();
        this.logBuilder.append(ts + log+ "\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTv.setText(logBuilder.toString());
                logSv.post(()-> logSv.fullScroll(ScrollView.FOCUS_DOWN));
            }
        });
    }

    //region Private
    private void onSelectResBtnTouch(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_RES_FILE);
    }

    private void onSelectCtrlBtnTouch(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_CTRL_FILE);
    }
    private void onSelectImageBtnTouch(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_IMAGE_FILE);
    }

    private void onStartBtnTouch(){
        try{
            this.speedView.clear();
            String rspFreq = this.frequencyEt.getText().toString();
            int irspFreq = Integer.parseInt(rspFreq);
            ArrayList<DFUImagePath> paths = new ArrayList<>();
            if(!StringUtil.isNullOrEmpty(this.ctrlFilePath)){
                Uri ctrlUri = this.makeFileUri(this.ctrlFilePath);
                DFUImagePath ctrlImagePath = new DFUImagePath(null, ctrlUri, IMAGE_ID_CTRL);
                paths.add(ctrlImagePath);
            }


            if(!StringUtil.isNullOrEmpty(this.resFilePath)){
                Uri resUri = this.makeFileUri(this.resFilePath);
                DFUImagePath resImagePath = new DFUImagePath(null, resUri, IMAGE_ID_NAND_RES);
                paths.add(resImagePath);
            }


            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                Uri imageUri = this.makeFileUri(item.getLocalPath());
                DFUImagePath imagePath = new DFUImagePath(null, imageUri, item.getImageID());
                paths.add(imagePath);

            }
            this.addLog("启动ota nand...");
            this.sifliDFUService.startActionDFUNand(this,targetMac,paths,0,irspFreq);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }

    }

    private Uri makeFileUri(String path) {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        return uri;
    }

    private void onResumeBtnTouch(){
        try{
            this.speedView.clear();
            String rspFreq = this.frequencyEt.getText().toString();
            int irspFreq = Integer.parseInt(rspFreq);
            ArrayList<DFUImagePath> paths = new ArrayList<>();
            if(!StringUtil.isNullOrEmpty(this.ctrlFilePath)){
                Uri ctrlUri = this.makeFileUri(this.ctrlFilePath);
                DFUImagePath ctrlImagePath = new DFUImagePath(null, ctrlUri, IMAGE_ID_CTRL);
                paths.add(ctrlImagePath);
            }

            if(!StringUtil.isNullOrEmpty(this.resFilePath)){
                Uri resUri = this.makeFileUri(this.resFilePath);
                DFUImagePath resImagePath = new DFUImagePath(null, resUri, IMAGE_ID_NAND_RES);
                paths.add(resImagePath);
            }


            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                Uri imageUri = this.makeFileUri(item.getLocalPath());
                DFUImagePath imagePath = new DFUImagePath(null, imageUri, item.getImageID());
                paths.add(imagePath);

            }
            this.addLog("启动ota nand...");
            this.sifliDFUService.startActionDFUNand(this,targetMac,paths,1,irspFreq);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void onStopBtnTouch(){
        SFLog.i(TAG,"主动停止...");
        this.addLog("主动停止...");
//        Intent intent = new Intent(this, SifliDFUService.class);
//        this.stopService(intent);
        this.finish();

    }

    private void toast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    private void showImageTypeMenu(SFOtaImageItem item,View v){
        SFLog.i(TAG,"showImageTypeMenu");
        String[] src = new String[imageTypeItems.size()];
        for (int i = 0; i < imageTypeItems.size(); i++) {
            src[i] = imageTypeItems.get(i).getName();
        }

        this.rightMenuPop = new XPopup.Builder(this)
                .atView(v)
//                .isViewMode(true)      //开启View实现
                .isRequestFocus(false) //不强制焦点
//                .isClickThrough(true)  //点击透传
                .hasShadowBg(false)
                .popupAnimation(PopupAnimation.ScaleAlphaFromCenter)
                .asAttachList(src, null, new OnSelectListener() {
                    @Override
                    public void onSelect(int position, String text) {
                        SFLog.i(TAG,"showImageTypeMenu onSelect position=" + position + ",text=" + text);
                        applyImageTypeSelection(position);
                    }
                });
        this.rightMenuPop.show();
    }

    private void applyImageTypeSelection(int position){
        SFLog.i(TAG,"applyImageTypeSelection");
        SFOtaImageTypeItem typeItem = this.imageTypeItems.get(position);
        if(this.currentImageItem != null){
            this.currentImageItem.setImageID(typeItem.getType());
            this.currentImageItem.setImageIDName(typeItem.getName());
            this.imageAdapter.notifyDataSetChanged();
        }
    }




    //endregion
}