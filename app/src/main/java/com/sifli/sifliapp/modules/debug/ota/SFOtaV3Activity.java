package com.sifli.sifliapp.modules.debug.ota;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.siflicore.error.SFError;
import com.sifli.siflicore.log.SFLog;
import com.sifli.siflicore.shell.SFBleShellStatus;
import com.sifli.sifliotasdk.manager.ISFOtaV3ManagerCallback;
import com.sifli.sifliotasdk.manager.SFOtaV3Manager;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3BinFileInfo;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3DfuFileType;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3ImageID;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3ResourceFileInfo;
import com.sifli.sifliapp.utils.speedview.*;
import com.sifli.sifliapp.utils.StringUtil;

import java.util.ArrayList;

public class SFOtaV3Activity extends AppCompatActivity implements View.OnClickListener,ISFOtaImageAdapterCallback, ISFOtaV3ManagerCallback {
    private final static String TAG = "SFOtaV3Activity";
    public final static String EXTRA_BLE_DEVICE = "EXTRA_BLE_DEVICE";
    private final static int COMMAND_SELECT_RES_FILE = 1;
    private final static int COMMAND_SELECT_CTRL_FILE = 2;
    private final static int COMMAND_SELECT_IMAGE_FILE = 3;
    private final  static  int COMMAND_MAKE_APP_RES = 4;

    private TextView macAddressTv;
    private TextView logTv;
    private ScrollView logSv;
    private TextView progressTv;
    private ProgressBar progressBar;

    private Button selectResBtn;
    private Button selectCtrlBtn;
    private Button selectImageBtn;
    private Button makeResBinBtn;

    private TextView resTv;
    private TextView ctrlTv;
    private ListView imageListView;

    private Button firmwareStartBtn;
    private Button firmwareResumeBtn;
    private Button resStartBtn;
    private Button resResumeBtn;
    private Button stopBtn;
//    private EditText frequencyEt;
    private TextView speedTv;
    private EditText resOtaTypeEt;
    private CheckBox alignCb;

    private String resFilePath;
    private String ctrlFilePath;
    private String targetMac = "FF:FF:79:DA:77:C8";//525

    private ArrayList<SFOtaImageTypeItem> imageTypeItems;
    private ArrayList<SFOtaImageItem> dataSource;
    private SFOtaImageAdapter imageAdapter;
    private BasePopupView rightMenuPop = null;
    private SFOtaImageItem currentImageItem;
    private SFOtaV3Manager otaManager;
    private StringBuilder logBuilder;
    private SpeedView speedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otav3);
        this.setupView();
        this.bindEvent();
        this.init();

        this.otaManager = SFOtaV3Manager.getInstance();
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
//        frequencyEt = findViewById(R.id.sf_ota_nand_frequency_tv);
        resOtaTypeEt = findViewById(R.id.sf_ota_v3_res_file_type_btn);
        alignCb = findViewById(R.id.sf_ota_v3_res_file_with_byte_align_cb);

        this.firmwareStartBtn = findViewById(R.id.sf_ota_v3_firmware_start_btn);
        this.firmwareResumeBtn = findViewById(R.id.sf_ota_v3_firmware_resume_btn);
        this.resStartBtn = findViewById(R.id.sf_ota_v3_res_start_btn);
        this.resResumeBtn = findViewById(R.id.sf_ota_v3_res_resume_btn);
        stopBtn = findViewById(R.id.sf_ota_v3_stop_btn);
        speedTv = findViewById(R.id.sf_ota_nand_speed_tv);
        this.makeResBinBtn = findViewById(R.id.sf_ota_v3_make_app_res_btn);
    }

    private void bindEvent(){
        this.selectResBtn.setOnClickListener(this);
        this.selectCtrlBtn.setOnClickListener(this);
        this.selectImageBtn.setOnClickListener(this);

        this.firmwareStartBtn.setOnClickListener(this);
        this.firmwareResumeBtn.setOnClickListener(this);
        this.resStartBtn.setOnClickListener(this);
        this.resResumeBtn.setOnClickListener(this);
        this.stopBtn.setOnClickListener(this);
        this.makeResBinBtn.setOnClickListener(this);
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
        hcpuItem.setType(OtaV3ImageID.HCPU);
        hcpuItem.setName("HCPU");
        this.imageTypeItems.add(hcpuItem);

        SFOtaImageTypeItem lcpuItem = new SFOtaImageTypeItem();
        lcpuItem.setType(OtaV3ImageID.LCPU);
        lcpuItem.setName("LCPU");
        this.imageTypeItems.add(lcpuItem);

        SFOtaImageTypeItem patchItem = new SFOtaImageTypeItem();
        patchItem.setType(OtaV3ImageID.LCPU_PATCH);
        patchItem.setName("LCPU_PATCH");
        this.imageTypeItems.add(patchItem);

        SFOtaImageTypeItem NOR_RES_NAND_DYN = new SFOtaImageTypeItem();
        NOR_RES_NAND_DYN.setType(OtaV3ImageID.NOR_RES);
        NOR_RES_NAND_DYN.setName("NOR_RES");
        this.imageTypeItems.add(NOR_RES_NAND_DYN);

        SFOtaImageTypeItem NOR_FONT_NAND_MUSIC = new SFOtaImageTypeItem();
        NOR_FONT_NAND_MUSIC.setType(OtaV3ImageID.NOR_FONT);
        NOR_FONT_NAND_MUSIC.setName("NOR_FONT");
        this.imageTypeItems.add(NOR_FONT_NAND_MUSIC);

        SFOtaImageTypeItem rootItem = new SFOtaImageTypeItem();
        rootItem.setType(OtaV3ImageID.NOR_ROOT);
        rootItem.setName("NOR_ROOT");
        this.imageTypeItems.add(rootItem);

        SFOtaImageTypeItem otaManager = new SFOtaImageTypeItem();
        otaManager.setType(OtaV3ImageID.NOR_OTA_MANAGER);
        otaManager.setName("NOR_OTA_MANAGER");
        this.imageTypeItems.add(otaManager);

        SFOtaImageTypeItem tinyFont = new SFOtaImageTypeItem();
        tinyFont.setType(OtaV3ImageID.NOR_TINY_FONT);
        tinyFont.setName("NOR_TINY_FONT");
        this.imageTypeItems.add(tinyFont);

        SFOtaImageTypeItem nandPic = new SFOtaImageTypeItem();
        nandPic.setType(OtaV3ImageID.NAND_PIC);
        nandPic.setName("NAND_PIC");
        this.imageTypeItems.add(nandPic);

        SFOtaImageTypeItem nandFont = new SFOtaImageTypeItem();
        nandFont.setType(OtaV3ImageID.NAND_FONT);
        nandFont.setName("NAND_FONT");
        this.imageTypeItems.add(nandFont);

        SFOtaImageTypeItem nandLang = new SFOtaImageTypeItem();
        nandLang.setType(OtaV3ImageID.NAND_LANG);
        nandLang.setName("NAND_LANG");
        this.imageTypeItems.add(nandLang);

        SFOtaImageTypeItem nandRing = new SFOtaImageTypeItem();
        nandRing.setType(OtaV3ImageID.NAND_RING);
        nandRing.setName("NAND_RING");
        this.imageTypeItems.add(nandRing);

        SFOtaImageTypeItem nandRoot = new SFOtaImageTypeItem();
        nandRoot.setType(OtaV3ImageID.NAND_ROOT);
        nandRoot.setName("NAND_ROOT");
        this.imageTypeItems.add(nandRoot);

        SFOtaImageTypeItem nandMusic = new SFOtaImageTypeItem();
        nandMusic.setType(OtaV3ImageID.NAND_MUSIC);
        nandMusic.setName("NAND_MUSIC");
        this.imageTypeItems.add(nandMusic);

        SFOtaImageTypeItem nandDyn = new SFOtaImageTypeItem();
        nandDyn.setType(OtaV3ImageID.NAND_DYN);
        nandDyn.setName("NAND_DYN");
        this.imageTypeItems.add(nandDyn);

        SFOtaImageTypeItem nandNym = new SFOtaImageTypeItem();
        nandNym.setType(OtaV3ImageID.NAND_NYM);
        nandNym.setName("NAND_NYM");
        this.imageTypeItems.add(nandNym);
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
        }else if(requestCode == COMMAND_MAKE_APP_RES){
            if(data == null)return;
//            String resBinZipPath = data.getStringExtra(AppResActivity.APP_RES_ZIP_PATH);
//            SFDemoLog.i(TAG,"resBinZipPath:" + resBinZipPath);
//            this.resFilePath = resBinZipPath;
//            String fileName = FileUtil.getFileNameOfPath(this.resFilePath);
//            this.resTv.setText(fileName);
//            SFDemoLog.i(TAG,"filepath=" + this.resFilePath);
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
        }else if(viewId == R.id.sf_ota_v3_res_start_btn){
            this.onResStartBtnTouch();
        }else if(viewId == R.id.sf_ota_v3_res_resume_btn){
            this.onResResumeBtnTouch();
        }else if(viewId == R.id.sf_ota_v3_stop_btn){
            this.onStopBtnTouch();
        }else if(viewId == R.id.sf_ota_v3_firmware_start_btn){
            this.onFirmwareStartBtnTouch();
        }else if(viewId == R.id.sf_ota_v3_firmware_resume_btn){
            this.onFirmwareResumeBtnTouch();
        }else if(viewId == R.id.sf_ota_v3_make_app_res_btn){
//            this.onMakeAppResBtnTouch();
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
    public void onProgress(long currentBytes, long totalBytes) {
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
//        if(stage == SFOTAProgressStage.NAND_RES){
//            stageName = "Res";
//        }else if(stage == SFOTAProgressStage.NAND_IMAGE){
//            stageName = "Image";
//        }
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
                logSv.fullScroll(ScrollView.FOCUS_DOWN);
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

//    private void onMakeAppResBtnTouch(){
//        Intent intent = new Intent(SFOtaV3Activity.this, AppResActivity.class);
//        startActivityForResult(intent,COMMAND_MAKE_APP_RES);
//    }

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

    private void onResStartBtnTouch(){
        try{
            this.speedView.clear();
//            String rspFreq = this.frequencyEt.getText().toString();
//            int irspFreq = Integer.parseInt(rspFreq);
//            ArrayList<OTAImageFileInfo> imageFiles = new ArrayList<>();
//            for (SFOtaImageItem item:this.dataSource) {
//                if(item.getImageID() < 0){
//                    toast("请选择Image File Type");
//                    return;
//                }
//                OTAImageFileInfo fileInfo = new OTAImageFileInfo(item.getLocalPath(),item.getImageID());
//                imageFiles.add(fileInfo);
//            }
            String otaType = this.resOtaTypeEt.getText().toString();
            int iOtaType = Integer.parseInt(otaType);
            boolean align = alignCb.isChecked();
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,align);
            this.addLog("启动ota v3 push res..." + iOtaType);
            this.otaManager.startOtaV3(this.targetMac, iOtaType,resourceFileInfo,false);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }

    }

    private void onResResumeBtnTouch(){
        try{
            this.speedView.clear();
//            String rspFreq = this.frequencyEt.getText().toString();
//            int irspFreq = Integer.parseInt(rspFreq);
//            ArrayList<OTAImageFileInfo> imageFiles = new ArrayList<>();
//            for (SFOtaImageItem item:this.dataSource) {
//                if(item.getImageID() < 0){
//                    toast("请选择Image File Type");
//                    return;
//                }
//                OTAImageFileInfo fileInfo = new OTAImageFileInfo(item.getLocalPath(),item.getImageID());
//                imageFiles.add(fileInfo);
//            }
//            this.addLog("启动ota nand,resume = true");
//            this.otaManager.startOTANand(this.targetMac,this.resFilePath,this.ctrlFilePath,imageFiles,true,irspFreq);
            String otaType = this.resOtaTypeEt.getText().toString();
            int iOtaType = Integer.parseInt(otaType);
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,true);
            this.addLog("启动ota v3 push res..." + iOtaType);
            this.otaManager.startOtaV3(this.targetMac, iOtaType,resourceFileInfo,true);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void onFirmwareStartBtnTouch(){
        try{
            this.speedView.clear();
            this.addLog("启动ota onFirmwareStartBtnTouch...");
            OtaV3ResourceFileInfo resourceFileInfo = null;
            if(!StringUtil.isNullOrEmpty(this.resFilePath)){
                resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,true);
            }

            ArrayList<OtaV3BinFileInfo> binFileInfos = new ArrayList<>();
            if(!StringUtil.isNullOrEmpty(this.ctrlFilePath)){
                OtaV3BinFileInfo ctrlFile = new OtaV3BinFileInfo(OtaV3DfuFileType.OTA_CTRL_FILE,this.ctrlFilePath,-1);
                binFileInfos.add(ctrlFile);
            }

            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                if(!this.isValidHexOffset(item.getHexOffset())){
                    toast(item.getFileName() + "的偏移无效");
                    return;
                }
                OtaV3BinFileInfo binFileInfo = new OtaV3BinFileInfo(OtaV3DfuFileType.OTA_BIN_FILE,item.getLocalPath(),item.getImageID(), item.getHexOffset());
                binFileInfos.add(binFileInfo);
            }

            this.otaManager.startOtaV3(this.targetMac,resourceFileInfo,binFileInfos,false);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void onFirmwareResumeBtnTouch(){
        try{
            this.speedView.clear();
            this.addLog("启动ota onFirmwareResumeBtnTouch...");
            OtaV3ResourceFileInfo resourceFileInfo = null;
            if(!StringUtil.isNullOrEmpty(this.resFilePath)){
                resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,true);
            }
            ArrayList<OtaV3BinFileInfo> binFileInfos = new ArrayList<>();
            if(!StringUtil.isNullOrEmpty(this.ctrlFilePath)){
                OtaV3BinFileInfo ctrlFile = new OtaV3BinFileInfo(OtaV3DfuFileType.OTA_CTRL_FILE,this.ctrlFilePath,-1);
                binFileInfos.add(ctrlFile);
            }
            for (SFOtaImageItem item:this.dataSource) {
                if(item.getImageID() < 0){
                    toast("请选择Image File Type");
                    return;
                }
                if(!this.isValidHexOffset(item.getHexOffset())){
                    toast(item.getFileName() + "的偏移无效");
                    return;
                }
                OtaV3BinFileInfo binFileInfo = new OtaV3BinFileInfo(OtaV3DfuFileType.OTA_BIN_FILE,item.getLocalPath(),item.getImageID(),item.getHexOffset());
                binFileInfos.add(binFileInfo);
            }

            this.otaManager.startOtaV3(this.targetMac,resourceFileInfo,binFileInfos,true);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private boolean isValidHexOffset(String hexOffset){
        if(StringUtil.isNullOrEmpty(hexOffset)){
            return true;
        }
        if(hexOffset.length() > 8){
            return false;
        }
        if(!StringUtil.isHexStr(hexOffset)){
            return false;
        }
        return true;
    }

    private void onStopBtnTouch(){
        this.addLog("主动停止...");
        this.otaManager.userCancel();

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