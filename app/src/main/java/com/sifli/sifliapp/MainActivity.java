package com.sifli.sifliapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.king.view.arcseekbar.ArcSeekBar;
import com.sifli.sifliapp.modules.devicescan.DeviceScanActivity;
import com.sifli.sifliapp.modules.setting.PushAppSettingActivity;

import com.sifli.sifliapp.modules.user.SFLoginResult;
import com.sifli.sifliapp.modules.user.SFUser;
import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.sifliapp.utils.FolderHelper;
import com.sifli.sifliapp.utils.StringUtil;
import com.sifli.sifliapp.utils.speedview.SpeedView;
import com.sifli.siflicore.error.SFError;
import com.sifli.siflicore.log.SFLog;
import com.sifli.sifliotasdk.manager.ISFOtaV3ManagerCallback;
import com.sifli.sifliotasdk.manager.SFOtaV3Manager;
import com.sifli.sifliapp.modules.pushapp.model.*;
import com.sifli.sifliotasdk.manager.SFTransmissionMode;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3DfuFileType;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3ResourceFileInfo;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3Type;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ISFOtaV3ManagerCallback {
    private final static String TAG = "MainActivity";
    private final static int PUSH_TYPE_WATCHFACE = 9;
    private final static int PUSH_TYPE_APP = 12;
    private final static int PUSH_TYPE_APP_RES = 13;

    private final static int COMMAND_SELECT_RES_FILE = 1;

    private ArcSeekBar arcSeekBar;
    private Button searchDeviceBtn;
    private Button selectFileBtn;
    private RadioButton pushAppRadio;
    private RadioButton pushAppResRadio;
    private RadioButton pushWatchfaceRadio;
    private TextView appFilenameTv;
    private TextView appResFileNameTv;
    private EditText appIdEt;
    private EditText resUIDEt;
    private Button sendBtn;
    private TextView macTv;
    private RadioGroup pushTypeRg;
    private ActivityResultLauncher<Intent> searchLauncher;
    private ActivityResultLauncher<Intent> selectFileLauncher;
    private ActivityResultLauncher<Intent> selectPhotoLauncher;

    private RadioGroup.OnCheckedChangeListener pushTypeCheckedChangeListener;
    private LinearLayout appContentLL;
    private LinearLayout appResContentLL;
    private ImageView settingView;
    private TextView msgTv;
    private EditText clipWidthEt;
    private EditText clipHeightEt;
    private Button clipBtn;

    private String targetMac;
    private int pushType = PUSH_TYPE_APP;

    private SFOtaV3Manager otaManager;
    private SpeedView speedView;
    private String resFilePath;

    //选图片推送资源
    private Uri selectedPhotoUri;
    private String photoFilePath;
    private String resBinPath;
    private String resBinZipPath;
    private AppResClipMaker resClipMaker;
    private AppResBinWriter resBinWriter;
    private AppResZipMaker zipMaker;
    private EzipSetting ezipSetting;
    private String watchPathFormat = "gui_tool/tool_app/{app_id}/{uid}.bin";

    private final String[] PERMISSIONS_CALL = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setupView();
        this.bindEvent();
        this.init();
        this.checkCallPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SFLog.i(TAG,"onResume");
        this.otaManager.setCallback(this);
        this.targetMac = SFUser.getInstance().getMac();
        if(this.targetMac != null){

            this.onQrResult(this.targetMac);
        }
    }

    private  void setupView(){
        this.arcSeekBar = findViewById(R.id.sf_push_app_arcSeekBar);
        this.searchDeviceBtn = findViewById(R.id.sf_push_app_search_device_btn);
        this.selectFileBtn = findViewById(R.id.sf_push_app_select_file_btn);
        this.pushAppRadio = findViewById(R.id.sf_push_app_radio);
        this.pushAppResRadio = findViewById(R.id.sf_push_app_res_radio);
        this.pushWatchfaceRadio = findViewById(R.id.sf_push_watchface_radio);
        this.appFilenameTv = findViewById(R.id.sf_push_app_filename_tv);
        this.appResFileNameTv = findViewById(R.id.sf_push_app_res_image_name_tv);
        this.appIdEt = findViewById(R.id.sf_push_app_res_app_id_et);
        this.resUIDEt = findViewById(R.id.sf_push_app_res_UID_et);
        this.sendBtn = findViewById(R.id.sf_push_app_send_btn);
        this.macTv = findViewById(R.id.sf_push_app_mac_address_tv);
        this.pushTypeRg = findViewById(R.id.sf_push_app_type_rg);
        this.appContentLL = findViewById(R.id.sf_push_app_content_ll);
        this.appResContentLL = findViewById(R.id.sf_push_app_res_content_ll);
        this.settingView = findViewById(R.id.sf_push_app_setting_iv);
        this.msgTv = findViewById(R.id.sf_push_app_msg_tv);
        this.clipWidthEt = findViewById(R.id.sf_push_app_clip_width_et);
        this.clipHeightEt = findViewById(R.id.sf_push_app_clip_height_et);
        this.clipBtn = findViewById(R.id.sf_push_app_clip_btn);
//        FontAwesome.Icon.faw_chevron_left

    }

    private void bindEvent(){
        this.searchDeviceBtn.setOnClickListener(this);
        this.selectFileBtn.setOnClickListener(this);
        this.sendBtn.setOnClickListener(this);
        this.settingView.setOnClickListener(this);
        this.clipBtn.setOnClickListener(this);
        this.createPushTypeChangedListener();

    }

    private void init(){
        SFUser.getInstance().setSharedPreferences(this.getPreferences(Context.MODE_PRIVATE));
        SFUser.getInstance().load();
        this.speedView = new SpeedView();
        this.otaManager = SFOtaV3Manager.getInstance();
        this.otaManager.setCallback(this);
        this.otaManager.init(this.getApplication(), SFTransmissionMode.TRANSMISSION_MODE_BLE);
        this.ezipSetting = new EzipSetting();

        createSearchLauncher();
        createSelectFileLauncher();
        createSelectPhotoLauncher();
        this.updatePushTypeUI();
        SFLoginResult loginResult = SFUser.getInstance().getUserEntity();
        if (loginResult != null) {
            if (loginResult.getMac() != null) this.targetMac = loginResult.getMac();
            if (loginResult.getAppId() != null) this.appIdEt.setText(loginResult.getAppId());
            if (loginResult.getResUID() != null) this.resUIDEt.setText(loginResult.getResUID());
        }

        if(this.targetMac != null){

            this.onQrResult(this.targetMac);
        }
    }

    private void checkCallPermission () {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            SFLog.w(TAG, "checkCallPermission...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_CALL, 123);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            int outHeight = UCrop.getOutputImageHeight(data);
            int outWidth = UCrop.getOutputImageWidth(data);
            SFLog.i(TAG,"onActivityResult make clip success." + resultUri.getPath());
            SFLog.i(TAG,"outWidth=%d,outHeight=%d",outWidth,outHeight);
            onMakeClipSuccess(resultUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            SFLog.e(TAG,"make clip error." + cropError);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId == R.id.sf_push_app_search_device_btn){
            this.onSearchBtnTouch();
        }else if(viewId == R.id.sf_push_app_select_file_btn){
            this.onSelectFileBtnTouch();
        }else if(viewId == R.id.sf_push_app_send_btn){
            this.onSendBtnTouch();
        }else if(viewId == R.id.sf_push_app_setting_iv){
            this.onSettingBtnTouch();
        }else if(viewId == R.id.sf_push_app_clip_btn){
            this.onCutBtnTouch();
        }
    }

    private void onSearchBtnTouch(){
        this.searchLauncher.launch(new Intent(MainActivity.this, DeviceScanActivity.class));
    }

    private void createSearchLauncher() {
        this.searchLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                if (resultCode == RESULT_OK) {
                    String macStr = data.getStringExtra(DeviceScanActivity.EXTRA_BLE_DEVICE);
                    SFUser.getInstance().saveMac(macStr);
                    onQrResult(macStr);
                }
            }
        });
    }
    private void createSelectFileLauncher() {
        this.selectFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    SFLog.d(TAG, "res uri " + uri);
                    onResFileSelect(uri);
                }
            }
        });
    }

    private void onResFileSelect(Uri uri){
        this.resFilePath = FileUtil.getFilePathFromURI(this,uri);
        String fileName = FileUtil.getUrlName(uri,MainActivity.this);
        this.appFilenameTv.setText(fileName);
        SFLog.i(TAG,"filepath=" + this.resFilePath);
    }

    private void createSelectPhotoLauncher() {
        this.selectPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    SFLog.d(TAG, "photo uri " + uri);
                    onPhotoFileSelect(uri);
                }
            }
        });
    }

    private void onPhotoFileSelect(Uri uri){
        this.selectedPhotoUri = uri;
        this.photoFilePath = FileUtil.getFilePathFromURI(this,uri);
        String fileName = FileUtil.getUrlName(uri,MainActivity.this);
        this.appResFileNameTv.setText(fileName);
        SFLog.i(TAG,"photoFilePath=" + this.photoFilePath);
    }

    private void onCutBtnTouch(){
        SFLog.i(TAG,"onCutBtnTouch");
        if(this.selectedPhotoUri == null){
            this.toast("请先选择图片");
            return;
        }
        try{
            String clipWidthStr = this.clipWidthEt.getText().toString();
            Integer clipWidth = Integer.parseInt(clipWidthStr);

            String clipHeightStr = this.clipHeightEt.getText().toString();
            Integer clipHeight = Integer.parseInt(clipHeightStr);
            if(clipWidth <=0 || clipHeight <= 0){
                this.toast("请输入正确的尺寸");
                return;
            }

            String fileName = FileUtil.getUrlName(this.selectedPhotoUri,MainActivity.this);
            String destinationPath = FolderHelper.getTempPath(this) + "/" + "clip_" + fileName;
            Uri destinationUri = Uri.fromFile(new File(destinationPath));
            UCrop.of(this.selectedPhotoUri, destinationUri)
                    .withAspectRatio(clipWidth, clipHeight)
                    .withMaxResultSize(clipWidth, clipHeight)
                    .start(this);
        }catch (Exception ex){
            SFLog.e(TAG,"onCutBtnTouch error." + ex.getMessage());
            ex.printStackTrace();
            toast(ex.getMessage());
        }

    }

    private void onMakeClipSuccess(Uri resultUri){
        SFLog.i(TAG,"onMakeClipSuccess");
        this.photoFilePath = FileUtil.getFilePathFromURI(this,resultUri);
        String fileName = FileUtil.getFileName(resultUri);

        this.appResFileNameTv.setText(fileName);
        SFLog.i(TAG,"photoFilePath=%s,filename=%s", this.photoFilePath,fileName);

    }


    private  void createPushTypeChangedListener(){
        this.pushTypeCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.sf_push_app_radio){
                    pushType = PUSH_TYPE_APP;
                }else if(checkedId == R.id.sf_push_app_res_radio){
                    pushType = PUSH_TYPE_APP_RES;
                }else if(checkedId == R.id.sf_push_watchface_radio){
                    pushType = PUSH_TYPE_WATCHFACE;
                }
                updatePushTypeUI();
            }
        };
        this.pushTypeRg.setOnCheckedChangeListener(this.pushTypeCheckedChangeListener);
    }

    private void onQrResult(String qrStr) {
        this.macTv.setText(qrStr);
        this.targetMac = qrStr;
    }
    private  void updatePushTypeUI(){
        if(pushType == PUSH_TYPE_APP || pushType == PUSH_TYPE_WATCHFACE){
            this.appContentLL.setVisibility(View.VISIBLE);
            this.appResContentLL.setVisibility(View.GONE);
        }else if(pushType == PUSH_TYPE_APP_RES){
            this.appContentLL.setVisibility(View.GONE);
            this.appResContentLL.setVisibility(View.VISIBLE);
        }
    }

    private void onSettingBtnTouch() {
        Intent intent = new Intent(MainActivity.this, PushAppSettingActivity.class);
//        intent.putExtra(SFRpcTestActivity.EXTRA_BLE_DEVICE, this.targetMac);
        startActivity(intent);
    }

    private void onSelectFileBtnTouch(){
        if(pushType == PUSH_TYPE_APP || pushType == PUSH_TYPE_WATCHFACE){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            this.selectFileLauncher.launch(intent);
        }else if(pushType == PUSH_TYPE_APP_RES){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/png");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            this.selectPhotoLauncher.launch(intent);
        }
    }

    private void onSendBtnTouch() {
        SFLog.i(TAG,"onSendBtnTouch");
        if(StringUtil.isNullOrEmpty(targetMac)){
            SFLog.i(TAG,"target mac is null or empty.");
            toast("请先选择设备");
            return;
        }
        this.arcSeekBar.setProgress(0);
        if (pushType == PUSH_TYPE_APP) {
            sendApp();
        } else if (pushType == PUSH_TYPE_APP_RES) {
            sendAppRes();
        }else if(pushType == PUSH_TYPE_WATCHFACE){
            sendWatchface();
        }
    }

    private void sendApp(){
        SFLog.i(TAG,"sendApp %s",targetMac);
        try{

            if(this.resFilePath == null || this.resFilePath.isEmpty()){
                SFLog.i(TAG,"sendApp resFilePath is null");
                toast("请先选择文件");
                return;
            }
            this.speedView.clear();
            String otaType = "12";
            int iOtaType = Integer.parseInt(otaType);
            boolean align = false;
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,align);
            SFLog.i(TAG,"启动ota v3 push res..." + iOtaType);
            this.otaManager.startOtaV3(this.targetMac, iOtaType,resourceFileInfo,false);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void sendWatchface(){
        SFLog.i(TAG,"send watchface %s",targetMac);
        try{

            if(this.resFilePath == null || this.resFilePath.isEmpty()){
                SFLog.i(TAG,"send watchface resFilePath is null");
                toast("请先选择文件");
                return;
            }
            this.speedView.clear();
//            String otaType = "12";
//            int iOtaType = Integer.parseInt(otaType);
            boolean align = false;
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resFilePath,align);
            SFLog.i(TAG,"启动ota v3 push res..." + OtaV3Type.OTA_SIFLI_WATCHFACE);
            this.otaManager.startOtaV3(this.targetMac, OtaV3Type.OTA_SIFLI_WATCHFACE,resourceFileInfo,false);
        }catch (Exception ex){
            ex.printStackTrace();
            toast("异常:" + ex.getMessage());
        }
    }

    private void sendAppRes(){
        SFLog.i(TAG,"sendAppRes");
        if (StringUtil.isNullOrEmpty(this.photoFilePath)) {
            this.toast("请选择图片文件");
            return;
        }
        String appIdStr = this.appIdEt.getText().toString();
        if (StringUtil.isNullOrEmpty(appIdStr)) {
            this.toast("请输入app_id");
            return;
        }
        String uidStr = this.resUIDEt.getText().toString();
        if (StringUtil.isNullOrEmpty(uidStr)) {
            this.toast("请输入资源的UID");
            return;
        }
        SFUser.getInstance().saveAppIdAndResUID(appIdStr,uidStr);
        String watchPath = watchPathFormat.replace("{app_id}",appIdStr);
        watchPath = watchPath.replace("{uid}",uidStr);
        if(StringUtil.isNullOrEmpty(watchPath)){
            this.toast("请输入手表路径");
            return;
        }
        SFLog.i(TAG,"watchpath:%s",watchPath);
        String clipWidthStr = this.clipWidthEt.getText().toString();
        Integer clipWidth = Integer.parseInt(clipWidthStr);

        String clipHeightStr = this.clipHeightEt.getText().toString();
        Integer clipHeight = Integer.parseInt(clipHeightStr);
        if(clipWidth <=0 || clipHeight <= 0){
            this.toast("请输入正确的尺寸");
            return;
        }

        String eColor = ezipSetting.getColor();
        if (StringUtil.isNullOrEmpty(eColor)) {
            this.toast("请输入ezip Color");
            return;
        }
        try {
//            String alphaStr = this.ezipAlphaEt.getText().toString();
            Integer alpha = ezipSetting.getNoAlpha();

//            String rotationStr = this.ezipRotationEt.getText().toString();
            Integer rotation = ezipSetting.getNoRotation();

//            String boardTypeStr = this.ezipBoardTypeEt.getText().toString();
            Integer boardType = ezipSetting.getBoardType();

            AppResContext resContext = new AppResContext();
            resContext.setWatchPath(watchPath);
            resContext.setImageFilePath(this.photoFilePath);
            resContext.setHexUID(uidStr);
            resContext.setClipWidth(clipWidth);
            resContext.setClipHeight(clipHeight);
            resContext.setEzipColor(eColor);
            resContext.setEzipAlpha(alpha);
            resContext.setEzipRotation(rotation);
            resContext.setEzipBoardType(boardType);
            resContext.setAppId(appIdStr);
            resClipMaker = new AppResClipMaker(resContext);
            resBinWriter = new AppResBinWriter(this.getApplication(),resContext);
            zipMaker = new AppResZipMaker(this.getApplication());
            Bitmap bitmap = resClipMaker.makeClip();
            byte[] pngData = resClipMaker.makePngData(bitmap);
            byte[] ezipResult = resClipMaker.pngToEzip(pngData);
            this.resBinPath = resBinWriter.makeResBin(ezipResult);
            SFLog.i(TAG,"resBin:" + this.resBinPath);
            this.printFileSummary(this.resBinPath);
            this.resBinZipPath = zipMaker.makeZip(resBinWriter.getResZipFoler());

            SFLog.i(TAG,"resZip:" + this.resBinZipPath);
            this.startPushAppRes();
        } catch (Exception e) {
            SFLog.e(TAG,"onMakeBtnTouch error." + e.toString());

            this.toast(e.getMessage());
        }
    }

    private void startPushAppRes(){
        SFLog.i(TAG,"start push app res...");
        try{
            this.speedView.clear();
            String otaType = "13";
            int iOtaType = Integer.parseInt(otaType);
            boolean align = true;
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resBinZipPath,align);
            SFLog.i(TAG,"启动ota v3 push res..." + iOtaType);
            this.otaManager.startOtaV3(this.targetMac, iOtaType,resourceFileInfo,false);
        }catch (Exception ex){
            ex.printStackTrace();
            SFLog.e(TAG,"startPushAppRes error." +ex.getMessage());
            toast("异常:" + ex.getMessage());
        }
    }

    private void printFileSummary(String filePath){
        byte[] fileData = com.sifli.siflicore.util.FileUtil.getFileData(filePath);
        if(fileData != null){
            String summary = com.sifli.siflicore.util.ByteUtil.hexSummary(fileData);
            SFLog.i(TAG,"printFileSummary:" + summary);
        }
    }

    private void toast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
    //region ISFOtaV3ManagerCallback
    @Override
    public void onManagerStatusChanged(int status) {
        SFLog.i(TAG,"onManagerStatusChanged status=" + status);
        if(status == 1){
            this.msgTv.setText("连接中...");
        }else if(status == 2){
            this.msgTv.setText("正在传输...");
        }
        boolean sendAlbe = status == 0;
        this.sendBtn.setEnabled(sendAlbe);
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
        this.arcSeekBar.setProgress(finalProgress);
        String stageName = "进度";

        String p = String.format("%.1f",percentProgress);

        SFLog.i(TAG,stageName + ":" + p);

//        this.speedTv.setText(this.speedView.getSpeedText(currentBytes,totalBytes));
    }



    @Override
    public void onComplete(boolean success, SFError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleComplete(success,error);
            }
        });

//        this.addLog(log);
    }

    private  void handleComplete(boolean success,SFError error){
        String log = "task complete.success=" + success  + ",error=" + error;

        if(success){
            SFLog.i(TAG,log);
            this.msgTv.setText("传输完成");
        }else {
            SFLog.e(TAG,log);
            if(error != null) this.msgTv.setText(error.toString());
        }
    }
    //endregion
}