package com.sifli.sifliapp.modules.debug.otanand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.sifli.siflicore.error.SFError;
import com.sifli.siflicore.log.SFLog;
import com.sifli.siflicore.shell.SFBleShellStatus;
import com.sifli.sifliotasdk.manager.ISFOtaManagerCallback;
import com.sifli.sifliotasdk.manager.SFOTAProgressStage;
import com.sifli.sifliotasdk.manager.SFOtaManager;
import com.sifli.sifliotasdk.model.OTAImageFileInfo;
import com.sifli.sifliotasdk.modules.nand.model.NandImageID;

import java.util.ArrayList;

public class SFNandActivity extends AppCompatActivity implements View.OnClickListener, ISFOtaImageAdapterCallback, ISFOtaManagerCallback {
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
    private SFOtaManager otaManager;
    private StringBuilder logBuilder;
    private SpeedView speedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sfnand);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setupView();
        this.bindEvent();
        this.init();

        this.otaManager = SFOtaManager.getInstance();
        this.otaManager.setCallback(this);
        this.otaManager.init(this.getApplication());
        this.macAddressTv.setText(targetMac);
        this.imageAdapter = new SFOtaImageAdapter(this,this.dataSource,this);
        this.imageListView.setAdapter(this.imageAdapter);
        this.speedView = new SpeedView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.otaManager.stop();
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
        this.logBuilder = new StringBuilder();
        this.imageTypeItems = new ArrayList<>();
        this.dataSource = new ArrayList<>();
        SFOtaImageTypeItem hcpuItem = new SFOtaImageTypeItem();
        hcpuItem.setType(NandImageID.HCPU);
        hcpuItem.setName("HCPU");
        this.imageTypeItems.add(hcpuItem);

        SFOtaImageTypeItem lcpuItem = new SFOtaImageTypeItem();
        lcpuItem.setType(NandImageID.LCPU);
        lcpuItem.setName("LCPU");
        this.imageTypeItems.add(lcpuItem);

        SFOtaImageTypeItem patchItem = new SFOtaImageTypeItem();
        patchItem.setType(NandImageID.PATCH);
        patchItem.setName("PATCH");
        this.imageTypeItems.add(patchItem);

        SFOtaImageTypeItem resItem = new SFOtaImageTypeItem();
        resItem.setType(NandImageID.RES);
        resItem.setName("FS_ROOT");
        this.imageTypeItems.add(resItem);

        SFOtaImageTypeItem LCPU_PATCH = new SFOtaImageTypeItem();
        LCPU_PATCH.setType(NandImageID.LCPU_PATCH);
        LCPU_PATCH.setName("LCPU_PATCH");
        this.imageTypeItems.add(LCPU_PATCH);

        SFOtaImageTypeItem DYN = new SFOtaImageTypeItem();
        DYN.setType(NandImageID.DYN);
        DYN.setName("DYN");
        this.imageTypeItems.add(DYN);

        SFOtaImageTypeItem MUSIC = new SFOtaImageTypeItem();
        MUSIC.setType(NandImageID.MUSIC);
        MUSIC.setName("MUSIC");
        this.imageTypeItems.add(MUSIC);

        SFOtaImageTypeItem PIC = new SFOtaImageTypeItem();
        PIC.setType(NandImageID.PIC);
        PIC.setName("PIC");
        this.imageTypeItems.add(PIC);

        SFOtaImageTypeItem FONT = new SFOtaImageTypeItem();
        FONT.setType(NandImageID.FONT);
        FONT.setName("FONT");
        this.imageTypeItems.add(FONT);

        SFOtaImageTypeItem RING = new SFOtaImageTypeItem();
        RING.setType(NandImageID.RING);
        RING.setName("RING");
        this.imageTypeItems.add(RING);

        SFOtaImageTypeItem LANG = new SFOtaImageTypeItem();
        LANG.setType(NandImageID.LANG);
        LANG.setName("LANG");
        this.imageTypeItems.add(LANG);
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

    @Override
    public void onManagerStatusChanged(int status) {
        SFLog.i(TAG,"onManagerStatusChanged status=" + status);
        this.addLog("onManagerStatusChanged status=" + status);
        this.stopBtn.setEnabled(status == SFBleShellStatus.MODULE_WORKING);
    }

    @Override
    public void onProgress(int stage, long currentBytes, long totalBytes) {
        int progress = 0;
        double percentProgress = 0;
        this.speedView.viewSpeedByCompleteBytes(currentBytes);
        if (totalBytes > 0) {
            double percent = (double) currentBytes / totalBytes;
            progress = (int) (percent * 100);
            percentProgress = percent * 100;
        }
        int finalProgress = progress;
        progressBar.setProgress(finalProgress);
        String stageName = "进度";
        if(stage == SFOTAProgressStage.NAND_RES){
            stageName = "Res";
        }else if(stage == SFOTAProgressStage.NAND_IMAGE){
            stageName = "Image";
        }
        String p = String.format("%.1f",percentProgress);
        progressTv.setText(stageName + ":" + p  + "%");
        SFLog.i(TAG,stageName + ":" + p);

        this.speedTv.setText(this.speedView.getSpeedText(currentBytes,totalBytes));
    }

    @Override
    public void onComplete(boolean success, SFError error) {

        String log = "task complete.success=" + success  + ",error=" + error;

        if(success){
            SFLog.i(TAG,log);

        }else {
            SFLog.e(TAG,log);
        }
        this.addLog(log);
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
            ArrayList<OTAImageFileInfo> imageFiles = new ArrayList<>();
            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                OTAImageFileInfo fileInfo = new OTAImageFileInfo(item.getLocalPath(),item.getImageID());
                imageFiles.add(fileInfo);
            }
            this.addLog("启动ota nand...");
            this.otaManager.startOTANand(this.targetMac,this.resFilePath,this.ctrlFilePath,imageFiles,false,irspFreq);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }

    }

    private void onResumeBtnTouch(){
        try{
            this.speedView.clear();
            String rspFreq = this.frequencyEt.getText().toString();
            int irspFreq = Integer.parseInt(rspFreq);
            ArrayList<OTAImageFileInfo> imageFiles = new ArrayList<>();
            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                OTAImageFileInfo fileInfo = new OTAImageFileInfo(item.getLocalPath(),item.getImageID());
                imageFiles.add(fileInfo);
            }
            this.addLog("启动ota nand,resume = true");
            this.otaManager.startOTANand(this.targetMac,this.resFilePath,this.ctrlFilePath,imageFiles,true,irspFreq);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void onStopBtnTouch(){
        this.addLog("主动停止...");
        this.otaManager.stop();

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