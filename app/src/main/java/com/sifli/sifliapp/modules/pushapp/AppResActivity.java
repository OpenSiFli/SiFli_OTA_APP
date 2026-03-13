package com.sifli.sifliapp.modules.pushapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.pushapp.model.AppResBinWriter;
import com.sifli.sifliapp.modules.pushapp.model.AppResClipMaker;
import com.sifli.sifliapp.modules.pushapp.model.AppResContext;
import com.sifli.sifliapp.modules.pushapp.model.AppResZipMaker;
import com.sifli.sifliapp.modules.user.SFLoginResult;
import com.sifli.sifliapp.modules.user.SFUser;
import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.sifliapp.utils.speedview.SpeedView;
import com.sifli.siflicore.error.SFError;
import com.sifli.siflicore.log.SFLog;
import com.sifli.sifliotasdk.manager.ISFOtaV3ManagerCallback;
import com.sifli.sifliotasdk.manager.SFOtaV3Manager;
import com.sifli.sifliotasdk.manager.SFTransmissionMode;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3DfuFileType;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3ResourceFileInfo;
import com.sifli.sifliapp.utils.StringUtil;

public class AppResActivity extends AppCompatActivity implements View.OnClickListener, ISFOtaV3ManagerCallback {

    private final static String TAG = "AppResActivity";
    private final  static  int COMMAND_SELECT_FILE = 1;
    public final static String APP_RES_ZIP_PATH = "APP_RES_ZIP_PATH";
    public final static String EXTRA_BLE_DEVICE = "EXTRA_BLE_DEVICE";

    private Button selectFileBtn;
    private TextView filenameTv;
    private EditText resUIDEt;
    private EditText clipWidthEt;
    private EditText clipHeightEt;

    private EditText ezipColorEt;
    private EditText ezipAlphaEt;
    private EditText ezipRotationEt;
    private EditText ezipBoardTypeEt;
    private EditText watchPathEt;
    private EditText appIdEt;

    private Button makeBtn;
    private Button stopBtn;
    private TextView progressTv;
    private ProgressBar progressBar;
    private TextView speedTv;
    private SpeedView speedView;
    private TextView macAddressTv;
    private TextView moreTv;
    private LinearLayout moreLl;

    private TextView logTv;
    private ScrollView logSv;
    private StringBuilder logBuilder;
    private String filePath;
    private String resBinPath;
    private String resBinZipPath;
    private AppResClipMaker resClipMaker;
    private AppResBinWriter resBinWriter;
    private AppResZipMaker zipMaker;

    private SFOtaV3Manager otaManager;
    private String targetMac = "FF:FF:79:DA:77:C8";//525
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_res);
        this.setupView();
        this.bindEvent();
        this.init();
    }

    private void setupView(){
        selectFileBtn = findViewById(R.id.sf_app_res_file_select_btn);
        resUIDEt = findViewById(R.id.sf_app_res_UID_et);
        clipWidthEt = findViewById(R.id.sf_app_res_width_et);
        clipHeightEt = findViewById(R.id.sf_app_res_height_et);
        ezipColorEt = findViewById(R.id.sf_app_res_ezip_color_et);
        ezipAlphaEt = findViewById(R.id.sf_app_res_ezip_alpha_et);
        ezipRotationEt = findViewById(R.id.sf_app_res_ezip_rotation_et);
        ezipBoardTypeEt = findViewById(R.id.sf_app_res_ezip_boardType_et);
        filenameTv = findViewById(R.id.sf_app_res_file_filename_tv);
        watchPathEt = findViewById(R.id.sf_app_res_watch_path_et);
        appIdEt = findViewById(R.id.sf_app_res_app_id_et);

        makeBtn = findViewById(R.id.sf_app_res_make_btn);
        stopBtn = findViewById(R.id.sf_app_res_stop_btn);

        logTv = findViewById(R.id.sf_app_res_log_tv);
        logSv = findViewById(R.id.sf_app_res_log_sv);

        progressTv = findViewById(R.id.sf_app_res_progress_tv);
        progressBar = findViewById(R.id.sf_app_res_progress_pb);
        speedTv = findViewById(R.id.sf_app_res_speed_tv);
        macAddressTv = findViewById(R.id.sf_app_res_mac_tv);

        moreTv = findViewById(R.id.sf_app_res_more_tv);
        moreLl = findViewById(R.id.sf_app_res_more_ll);
    }

    private void bindEvent(){
        this.selectFileBtn.setOnClickListener(this);
        this.makeBtn.setOnClickListener(this);
        this.stopBtn.setOnClickListener(this);
        this.moreTv.setOnClickListener(this);
    }


    private void init(){
        String mac = getIntent().getStringExtra(EXTRA_BLE_DEVICE);
        if(mac != null){
            targetMac = mac;
        }
        this.macAddressTv.setText(targetMac);
        this.logBuilder = new StringBuilder();
        this.speedView = new SpeedView();

        this.otaManager = SFOtaV3Manager.getInstance();
        this.otaManager.setCallback(this);
        this.otaManager.init(this.getApplication(), SFTransmissionMode.TRANSMISSION_MODE_BLE);

        SFLoginResult loginResult = SFUser.getInstance().getUserEntity();
        if (loginResult != null) {

            if (loginResult.getAppId() != null) this.appIdEt.setText(loginResult.getAppId());
            if (loginResult.getResUID() != null) this.resUIDEt.setText(loginResult.getResUID());
        }
    }

    private void addLog(String log){
        this.logBuilder.append(log+ "\n");
        if(logTv == null)return;
        if(logSv == null)return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTv.setText(logBuilder.toString());
                logSv.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId == R.id.sf_app_res_file_select_btn){
            this.onSelectFileBtnTouch();
        }else if(viewId == R.id.sf_app_res_make_btn){
            this.onMakeBtnTouch();
        }else if(viewId == R.id.sf_app_res_stop_btn){
            this.onStopBtnTouch();
        }else if(viewId == R.id.sf_app_res_more_tv){
            this.showMoreParam();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == COMMAND_SELECT_FILE){
            if(data == null)return;
            Uri uri = data.getData();
            Log.d(TAG, "wf uri " + uri);
            this.filePath = FileUtil.getFilePathFromURI(this,uri);
            String fileName = FileUtil.getUrlName(uri,this);
            this.filenameTv.setText(fileName);
            SFLog.i(TAG,"filepath=" + this.filePath);
        }
    }

    private  void showMoreParam(){
        if(this.moreLl.getVisibility() == View.VISIBLE){
            this.moreLl.setVisibility(View.GONE);
        }else{
            this.moreLl.setVisibility(View.VISIBLE);
        }
    }

    private void onSelectFileBtnTouch(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/png");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_FILE);
    }

    private void onMakeBtnTouch() {
        if (StringUtil.isNullOrEmpty(this.filePath)) {
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
        String watchPathFormat = this.watchPathEt.getText().toString();
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

        String eColor = this.ezipColorEt.getText().toString();
        if (StringUtil.isNullOrEmpty(eColor)) {
            this.toast("请输入ezip Color");
            return;
        }
        try {
            String alphaStr = this.ezipAlphaEt.getText().toString();
            Integer alpha = Integer.parseInt(alphaStr);

            String rotationStr = this.ezipRotationEt.getText().toString();
            Integer rotation = Integer.parseInt(rotationStr);

            String boardTypeStr = this.ezipBoardTypeEt.getText().toString();
            Integer boardType = Integer.parseInt(boardTypeStr);

            AppResContext resContext = new AppResContext();
            resContext.setWatchPath(watchPath);
            resContext.setImageFilePath(this.filePath);
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
            this.addLog("resBin:" + this.resBinPath);
            this.printFileSummary(this.resBinPath);
             this.resBinZipPath = zipMaker.makeZip(resBinWriter.getResZipFoler());

            this.addLog("resZip:" + this.resBinZipPath);
            this.startPush();
        } catch (Exception e) {
            SFLog.e(TAG,"onMakeBtnTouch error." + e.toString());
            this.addLog(e.getMessage());
            this.toast(e.getMessage());
        }


    }

    private void startPush(){
        this.addLog("start push...");
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
            String otaType = "13";
            int iOtaType = Integer.parseInt(otaType);
            boolean align = true;
            OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE,this.resBinZipPath,align);
            this.addLog("启动ota v3 push res..." + iOtaType);
            this.otaManager.startOtaV3(this.targetMac, iOtaType,resourceFileInfo,false);
        }catch (Exception ex){
            ex.printStackTrace();
            this.addLog(ex.getMessage());
            toast("异常:" + ex.getMessage());
        }
    }


    private void onStopBtnTouch(){
        this.addLog("主动停止...");
        this.otaManager.userCancel();

    }
    private void onApplyBtnTouch(){
        if(StringUtil.isNullOrEmpty(this.resBinZipPath)){
            this.toast("请先生成res bin zip");
            return;
        }
        // 创建 Intent 并设置结果数据
        Intent resultIntent = new Intent();
        resultIntent.putExtra(APP_RES_ZIP_PATH, this.resBinZipPath);

        // 设置结果并结束 Activity
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void toast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show();
    }

    private void printFileSummary(String filePath){
        byte[] fileData = com.sifli.siflicore.util.FileUtil.getFileData(filePath);
        if(fileData != null){
            String summary = com.sifli.siflicore.util.ByteUtil.hexSummary(fileData);
            SFLog.i(TAG,"printFileSummary:" + summary);
        }
    }

    @Override
    public void onManagerStatusChanged(int status) {
        SFLog.i(TAG,"onManagerStatusChanged status=" + status);
        this.addLog("onManagerStatusChanged status=" + status);
//        this.stopBtn.setEnabled(status == SFBleShellStatus.MODULE_WORKING);
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
}