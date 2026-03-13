package com.sifli.sifliapp.modules.debug.guibuilder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.interfaces.OnSelectListener;

import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.debug.guibuilder.adapter.ISFGUIBuilderImageAdapterCallback;
import com.sifli.sifliapp.modules.debug.guibuilder.adapter.SFGUIBuilderImageAdapter;
import com.sifli.sifliapp.modules.debug.guibuilder.model.MyWatchfaceEditItem;
import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.sifliapp.utils.FolderHelper;
import com.sifli.sifliapp.utils.speedview.SpeedView;
import com.sifli.siflicore.error.SFError;
import com.sifli.siflicore.error.SFException;
import com.sifli.siflicore.log.SFLog;
import com.sifli.siflicore.util.StringUtil;
import com.sifli.sifliguibuildersdk.editor.SGImageEditItem;
import com.sifli.sifliguibuildersdk.editor.SGWatchfaceEditItem;
import com.sifli.sifliguibuildersdk.editor.maker.ISGResourceZIPMakerCallback;
import com.sifli.sifliguibuildersdk.editor.maker.SGAppResContext;
import com.sifli.sifliguibuildersdk.editor.maker.SGResourceZIPMaker;
import com.sifli.sifliguibuildersdk.manager.SGWorkSpaceManager;
import com.sifli.sifliguibuildersdk.message.ISGPushMessageManagerCallback;
import com.sifli.sifliguibuildersdk.message.SGPushMessageManager;
import com.sifli.sifliguibuildersdk.uitree.LanguageItem;
import com.sifli.sifliguibuildersdk.uitree.SGTheme;
import com.sifli.sifliguibuildersdk.uitree.SGUI;
import com.sifli.sifliotasdk.manager.ISFOtaV3ManagerCallback;
import com.sifli.sifliotasdk.manager.SFOtaV3Manager;
import com.sifli.sifliotasdk.manager.SFTransmissionMode;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3DfuFileType;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3ResourceFileInfo;
import com.sifli.sifliotasdk.modules.sol2.usermodel.OtaV3Type;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WatchfaceEditorActivity extends AppCompatActivity implements View.OnClickListener,
        ISFGUIBuilderImageAdapterCallback,
        ISGResourceZIPMakerCallback,
        ISFOtaV3ManagerCallback,
        ISGPushMessageManagerCallback {

    private final static String TAG = "WatchfaceEditorActivity";
    public final static String EXTRA_BLE_DEVICE = "EXTRA_BLE_DEVICE";
    private final static String WATCH_PATH_TEMP = "dyn/dynamic_app/tool_wf/{app_id}/{uid}.bin";
    private final static int COMMAND_SELECT_FILE = 1;
    private final static int MESSAGE_SWITCH_THEME = 10;
    private final static int MESSAGE_DELETE_RES_PATCH = 11;
    private final static int MESSAGE_DELETE_APP = 12;

    private TextView macAddressTv;
    private TextView logTv;
    private ScrollView logSv;
    private TextView progressTv;
    private ProgressBar progressBar;
    private TextView fileNameTv;
    private TextView speedTv;
    private ImageView previewIv;
    private ListView editListView;
    private BasePopupView rightMenuPop = null;

    private Button selectBtn;
    private Button loadBtn;
//    private Button sendBtn;
    private Button previewBtn;
    private Button languageBtn;
    private Button themeBtn;
    private Button deleteAppBtn;
    private Button clearLogBtn;

    private StringBuilder logBuilder;
    private String filePath;
    private String targetMac = "FF:FF:79:DA:77:C8";//525
    private ArrayList<MyWatchfaceEditItem> dataSource;
//    private ArrayList<MyWatchfaceEditItem> currentSendList;//当前提交给设备进行修改的列表。
    private ArrayList<LanguageItem> languageItems;
    private SFGUIBuilderImageAdapter imageAdapter;

    private SGWorkSpaceManager workSpaceManager;
    private MyWatchfaceEditItem editItem;
    private ActivityResultLauncher<Intent> selectPhotoLauncher;
    private Uri selectedPhotoUri;
    private String photoFilePath;
    private String clipPhotoFilePath;
    private int currentMessage = 0;
    private MyWatchfaceEditItem currentDeleteItem;
    private MyWatchfaceEditItem currentEditItem;

    private SGResourceZIPMaker resourceZIPMaker;
    //    private SFAppResSendManager sendManager;
    private SFOtaV3Manager otaV3Manager;
    private SpeedView speedView;
    private SGPushMessageManager pushMessageManager;
    private KProgressHUD hud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchface_editor);
        this.setupView();
        this.bindEvent();
        this.init();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.pushMessageManager.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == COMMAND_SELECT_FILE) {
            if (data == null) return;
            Uri uri = data.getData();
            Log.d(TAG, "wf uri " + uri);
            this.filePath = FileUtil.getFilePathFromURI(this, uri);
            String fileName = FileUtil.getUrlName(uri, this);
            this.fileNameTv.setText(fileName);
            SFLog.i(TAG, "filepath=" + this.filePath);
        }

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            int outHeight = UCrop.getOutputImageHeight(data);
            int outWidth = UCrop.getOutputImageWidth(data);
            SFLog.i(TAG, "onActivityResult make clip success." + resultUri.getPath());
            SFLog.i(TAG, "outWidth=%d,outHeight=%d", outWidth, outHeight);
            onMakeClipSuccess(resultUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            SFLog.e(TAG, "make clip error." + cropError);
        }
    }

    private void setupView() {
        macAddressTv = findViewById(R.id.sf_guibuilder_mac_tv);
        logTv = findViewById(R.id.sf_guibuilder_log_tv);
        logSv = findViewById(R.id.sf_guibuilder_log_sv);
        progressTv = findViewById(R.id.sf_guibuilder_progress_tv);
        progressBar = findViewById(R.id.sf_guibuilder_progress_pb);
        speedTv = findViewById(R.id.sf_guibuilder_speed_tv);
        fileNameTv = findViewById(R.id.sf_guibuilder_filename_tv);
        previewIv = findViewById(R.id.sf_guibuilder_preview_iv);
        editListView = findViewById(R.id.sf_guibuilder_edit_lv);

        loadBtn = findViewById(R.id.sf_guibuilder_load_btn);
        selectBtn = findViewById(R.id.sf_guibuilder_select_btn);
//        sendBtn = findViewById(R.id.sf_guibuilder_send_btn);
        previewBtn = findViewById(R.id.sf_guibuilder_preview_btn);
        languageBtn = findViewById(R.id.sf_guibuilder_language_btn);
        themeBtn = findViewById(R.id.sf_guibuilder_theme_btn);
        deleteAppBtn = findViewById(R.id.sf_guibuilder_delete_app_btn);
        clearLogBtn = findViewById(R.id.sf_guibuilder_clear_log_btn);
    }

    private void bindEvent() {
        this.selectBtn.setOnClickListener(this);
//        this.sendBtn.setOnClickListener(this);
        this.loadBtn.setOnClickListener(this);
        this.previewBtn.setOnClickListener(this);
        this.languageBtn.setOnClickListener(this);
        this.themeBtn.setOnClickListener(this);
        this.deleteAppBtn.setOnClickListener(this);
        this.clearLogBtn.setOnClickListener(this);
    }

    private void init() {
        this.speedView = new SpeedView();
        String mac = getIntent().getStringExtra(EXTRA_BLE_DEVICE);
        if (mac != null) {
            targetMac = mac;
        }
        this.logBuilder = new StringBuilder();
        this.macAddressTv.setText(targetMac);
        this.workSpaceManager = new SGWorkSpaceManager();
        this.workSpaceManager.init(this.getApplication());


        this.resourceZIPMaker = SGResourceZIPMaker.getInstance();
        this.resourceZIPMaker.setCallback(this);

        this.createSelectPhotoLauncher();

        this.otaV3Manager = SFOtaV3Manager.getInstance();
        this.otaV3Manager.setCallback(this);
        this.otaV3Manager.init(this.getApplication(), SFTransmissionMode.TRANSMISSION_MODE_BLE);

        this.dataSource = new ArrayList<>();
        this.imageAdapter = new SFGUIBuilderImageAdapter(this, this.dataSource, this);
        this.editListView.setAdapter(this.imageAdapter);
        this.languageItems = new ArrayList<>();

        this.pushMessageManager = SGPushMessageManager.getInstance();
        this.pushMessageManager.init(this.getApplication());
        this.pushMessageManager.setCallback(this);
    }

    private void loadLanguageItems() {
        this.languageItems.clear();
        SGUI uiTree = this.workSpaceManager.getUiTree();
        List<LanguageItem> items = uiTree.getLanguageItems();
        if (items == null) return;
        this.languageItems.addAll(items);
    }

    private void reloadEditItems() {
        SFLog.i(TAG, "reloadEditItems");
        try {
            this.dataSource.clear();
            List<SGWatchfaceEditItem> editItems = this.workSpaceManager.getEditItems();
            for (SGWatchfaceEditItem item : editItems) {
                if (item.getClass() == SGImageEditItem.class) {
                    SGImageEditItem imageEditItem = (SGImageEditItem) item;
                    MyWatchfaceEditItem myItem = new MyWatchfaceEditItem(imageEditItem);
                    this.dataSource.add(myItem);
                }
            }
            this.imageAdapter.notifyDataSetChanged();
        } catch (SFException ex) {
            ex.printStackTrace();
            SFLog.e(TAG, "reloadEditItems error." + ex.toString());
        }

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

    private void onPhotoFileSelect(Uri uri) {
        this.selectedPhotoUri = uri;
        this.photoFilePath = FileUtil.getFilePathFromURI(this, uri);
//        String fileName = FileUtil.getUrlName(uri,WatchfaceEditorActivity.this);
//        this.appResFileNameTv.setText(fileName);
        SFLog.i(TAG, "photoFilePath=" + this.photoFilePath);
        Bitmap originImage = this.editItem.getEditItem().getOriginImage();
        int clipWidth = originImage.getWidth();
        int clipHeight = originImage.getHeight();
        String fileName = FileUtil.getUrlName(this.selectedPhotoUri, WatchfaceEditorActivity.this);
        String destinationPath = FolderHelper.getTempPath(this) + "/" + "clip_" + fileName;
        Uri destinationUri = Uri.fromFile(new File(destinationPath));
        UCrop.of(this.selectedPhotoUri, destinationUri)
                .withAspectRatio(clipWidth, clipHeight)
                .withMaxResultSize(clipWidth, clipHeight)
                .start(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.sf_guibuilder_select_btn) {
            this.onSelectFileBtnTouch();
        } else if (viewId == R.id.sf_guibuilder_load_btn) {
            this.onLoadBtnTouch();
        } else if (viewId == R.id.sf_guibuilder_preview_btn) {
            this.onPreviewBtnTouch();
        }  else if (viewId == R.id.sf_guibuilder_language_btn) {
            this.onLanguageBtnTouch();
        } else if(viewId == R.id.sf_guibuilder_theme_btn){
            this.onThemeBtnTouch();
        }else if(viewId == R.id.sf_guibuilder_delete_app_btn){
            this.onDeleteAppBtnTouch();
        }else if(viewId == R.id.sf_guibuilder_clear_log_btn){
            this.onClearLogBtnTouch();
        }
    }

    private void onSelectFileBtnTouch() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_FILE);
    }

    private void onLoadBtnTouch() {
        if (StringUtil.isNullOrEmpty(this.filePath)) {
            makeToast("please select a project file.");
            return;
        }
        try {
            this.workSpaceManager.openProject(this.filePath);
            this.addLog("load project success");
            this.reloadEditItems();
            this.loadLanguageItems();
            this.onPreviewBtnTouch();
            this.updateLanguageAndThemeBtn();
        } catch (Exception ex) {
            ex.printStackTrace();
            SFLog.e(TAG, "load project error. %s", ex);
        }
    }

    private void onPreviewBtnTouch() {
        try {
            Bitmap image = this.workSpaceManager.drawPreviewImage();
            if (image == null) {
                SFLog.e(TAG, "update preview image is null");
                this.addLog("update preview image is null");
                return;
            }
            this.previewIv.setImageBitmap(image);
            this.addLog("update preview image success");
        } catch (Exception ex) {
            ex.printStackTrace();
            SFLog.e(TAG, "update preview image error. %s", ex);
            this.addLog("update preview image error." + ex.getMessage());
        }
    }

//    private void onSendBtnTouch() {
//
//
//    }

    private ArrayList<MyWatchfaceEditItem> getSendList(){
        ArrayList<MyWatchfaceEditItem> result = new ArrayList<>();
        for (MyWatchfaceEditItem item :this.dataSource) {
            if(item.getEditItem().hasPatch() && !item.isHasSend()){
                result.add(item);
            }
        }
        return result;
    }

    private List<SGImageEditItem> makeSendItems(ArrayList<MyWatchfaceEditItem> sendList){
        ArrayList<SGImageEditItem> result = new ArrayList<>();
        for (MyWatchfaceEditItem item:sendList) {
            result.add(item.getEditItem());
        }
        return result;
    }

    private void startSend(String patchZipPath) {
        SFLog.i(TAG, "startSend");
        if(StringUtil.isNullOrEmpty(patchZipPath)){
            addLog("nothing to send");
            return;
        }else{
            addLog("startSend");
        }

        boolean align = true;
        OtaV3ResourceFileInfo resourceFileInfo = new OtaV3ResourceFileInfo(OtaV3DfuFileType.ZIP_RESOURCE, patchZipPath, align);
        this.otaV3Manager.startOtaV3(this.targetMac, OtaV3Type.OTA_SIFLI_APP_RES, resourceFileInfo, false);
    }


    private void onLanguageBtnTouch() {
        SFLog.i(TAG, "onLanguageBtnTouch");
        String[] src = new String[this.languageItems.size()];
        for (int i = 0; i < this.languageItems.size(); i++) {
            src[i] = this.languageItems.get(i).getName();
        }

        this.rightMenuPop = new XPopup.Builder(this)
                .atView(this.languageBtn)
//                .isViewMode(true)      //开启View实现
                .isRequestFocus(false) //不强制焦点
//                .isClickThrough(true)  //点击透传
                .hasShadowBg(false)
                .popupAnimation(PopupAnimation.ScaleAlphaFromCenter)
                .asAttachList(src, null, new OnSelectListener() {
                    @Override
                    public void onSelect(int position, String text) {
                        SFLog.i(TAG, "showImageTypeMenu onSelect position=" + position + ",text=" + text);
                        applyLanguageItem(position);
                    }
                });
        this.rightMenuPop.show();
    }

    private void onThemeBtnTouch() {
        SFLog.i(TAG, "onThemeBtnTouch");
        SGUI uiTree = this.workSpaceManager.getUiTree();
        if(uiTree == null){
            this.makeToast("please open project file first");
            return;
        }
        List<SGTheme> themes = uiTree.getThems();
        if(themes == null || themes.size() == 0){
            return;
        }
        String[] src = new String[themes.size()];
        for (int i = 0; i < themes.size(); i++) {
            src[i] = themes.get(i).getId();
        }

        this.rightMenuPop = new XPopup.Builder(this)
                .atView(this.languageBtn)
//                .isViewMode(true)      //开启View实现
                .isRequestFocus(false) //不强制焦点
//                .isClickThrough(true)  //点击透传
                .hasShadowBg(false)
                .popupAnimation(PopupAnimation.ScaleAlphaFromCenter)
                .asAttachList(src, null, new OnSelectListener() {
                    @Override
                    public void onSelect(int position, String text) {
                        SFLog.i(TAG, "onThemeBtnTouch onSelect position=" + position + ",text=" + text);
                        applyCurrentTheme(position);
                    }
                });
        this.rightMenuPop.show();
    }

    private void onDeleteAppBtnTouch(){
        if(this.workSpaceManager.getProject() == null){
            this.makeToast("please open project file first");
            return;
        }
        this.showProgressHUD("删除表盘...");
        this.pushMessageManager.sendDeleteAppCmd(this.targetMac,this.workSpaceManager.getProject().getId());
    }

    private void onClearLogBtnTouch(){
        this.logBuilder = new StringBuilder();
        this.logTv.setText("");
    }

    private void applyLanguageItem(int position) {
        LanguageItem item = this.languageItems.get(position);
        SGUI uiTree = this.workSpaceManager.getUiTree();
        uiTree.switchLanguageItemById(item.getId());

        this.onPreviewBtnTouch();
        this.reloadEditItems();
        this.updateLanguageAndThemeBtn();
    }

    private void applyCurrentTheme(int position){
        SGUI uiTree = this.workSpaceManager.getUiTree();
        List<SGTheme> themes = uiTree.getThems();
        if(themes == null || themes.size() == 0){
            return;
        }
        SGTheme theme = themes.get(position);
        this.workSpaceManager.switchTheme(theme.getId());
        this.reloadEditItems();
        this.onPreviewBtnTouch();
        this.showProgressHUD("切换样式..");
        this.pushMessageManager.sendSwitchThemeCmd(this.targetMac,
                this.workSpaceManager.getProject().getId(),
                theme.getId());
        this.currentMessage = MESSAGE_SWITCH_THEME;
        this.updateLanguageAndThemeBtn();
    }

    private void updateLanguageAndThemeBtn(){
        SGUI uiTree = this.workSpaceManager.getUiTree();
        if(uiTree == null)return;
        SGTheme currentTheme = uiTree.getCurrentTheme();
        if(currentTheme != null)this.themeBtn.setText(currentTheme.getId());
        int language = uiTree.getLanguageKey();
        LanguageItem lanItem = uiTree.getLanguageItemById("" + language);
        if(lanItem != null)this.languageBtn.setText(lanItem.getName());
    }


    private void makeToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void addLog(String log) {
        this.logBuilder.append(log + "\n");
        if (logTv == null) return;
        if (logSv == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTv.setText(logBuilder.toString());
                logSv.post(() -> logSv.fullScroll(ScrollView.FOCUS_DOWN));
            }
        });
    }

    private void onMakeClipSuccess(Uri resultUri) {
        SFLog.i(TAG, "onMakeClipSuccess");
        this.clipPhotoFilePath = FileUtil.getFilePathFromURI(this, resultUri);
        String fileName = FileUtil.getFileName(resultUri);

//        this.appResFileNameTv.setText(fileName);
        SFLog.i(TAG, "photoFilePath=%s,filename=%s", this.clipPhotoFilePath, fileName);
        Bitmap image = com.sifli.sifliguibuildersdk.util.FileUtil.readImageByPath(this.clipPhotoFilePath);
        SFLog.i(TAG, "patch image for %s,size width %d,height %d", this.editItem.getEditItem().getControlId(), image.getWidth(), image.getHeight());
        boolean editSuc = this.editItem.getEditItem().setPatchImage(image);
        this.editItem.setHasSend(false);
//        if (editSuc) this.editItem.setHasChange(true);
        this.imageAdapter.notifyDataSetChanged();

        try {
            this.workSpaceManager.makeImagePatch(this.editItem.getEditItem());
            this.onPreviewBtnTouch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showProgressHUD(String statusText){
        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(statusText)
                .setCancellable(false) // 是否可点击外部取消
                .setAnimationSpeed(2) // 动画速度
                .setDimAmount(0.5f) // 背景变暗程度
                .show();
    }

    private void dismissProgressHUD(){
        if (hud != null && hud.isShowing()) {
            hud.dismiss();
            hud = null;
        }
    }

    private void updateProgressHUDText(String newText) {
        if (hud != null && hud.isShowing()) {
            hud.setLabel(newText);
        }
    }

    @Override
    public void onEditBtnTouch(MyWatchfaceEditItem item, View view) {
        SFLog.i(TAG, "onEditBtnTouch");
        this.editItem = item;
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/png");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        this.selectPhotoLauncher.launch(intent);

    }

    @Override
    public void onDelBtnTouch(MyWatchfaceEditItem item, View view) {
        SFLog.i(TAG, "onDelBtnTouch");
        this.currentDeleteItem = item;
        this.currentMessage = MESSAGE_DELETE_RES_PATCH;
        this.showProgressHUD("删除资源补丁...");
        this.pushMessageManager.sendDeleteResPatchCmd(this.targetMac,this.workSpaceManager.getProject().getId(),item.getPatchFileName());

    }

    @Override
    public void onSendBtnTouch(MyWatchfaceEditItem item, View view) {
        SFLog.i(TAG, "onSendBtnTouch item");
        this.currentEditItem = item;
        String appId = this.workSpaceManager.getProject().getId();
        SGAppResContext context = new SGAppResContext(appId, WATCH_PATH_TEMP, this.workSpaceManager);
        context.setEZIPParameter("rgb565", 0, 1, 1);

        if(!item.getEditItem().hasPatch()){
            this.makeToast("请先修改表盘");
            return;
        }
        this.showProgressHUD("制作资源补丁...");
        this.resourceZIPMaker.startMakePathZip(context,item.getEditItem());
    }

    private void onPushDeleteResPatchSuccess(){
        try {
            SFLog.i(TAG,"onPushDeleteResPatchSuccess");
            if(this.currentDeleteItem == null)return;
            MyWatchfaceEditItem item = this.currentDeleteItem;
            this.workSpaceManager.deletePatch(item.getEditItem().getOriginImageName());
            item.getEditItem().deletePatch();
            item.setHasSend(false);
            this.imageAdapter.notifyDataSetChanged();
            this.onPreviewBtnTouch();
        } catch (Exception e) {
            e.printStackTrace();
            SFLog.e(TAG, "onDelBtnTouch error." + e.toString());
        }
    }

    //region ISGResourceZIPMakerCallback
    @Override
    public void onResoureceZipMakerComplete(boolean success, String patchZipPath, SFError error) {
        String msg = String.format("zip maker:onComplete success=%b,error=%s,patchZipPath=%s", success, error, patchZipPath);
        SFLog.i(TAG, msg);
        addLog(msg);
        if (success) {
            this.updateProgressHUDText("发送资源补丁...");
            startSend(patchZipPath);
        }else{
            this.dismissProgressHUD();
        }
    }

    @Override
    public void onResourceZipMakerProgress(int current, int total) {
        SFLog.i(TAG, "zip maker:onProgress current=%d,total=%d", current, total);
    }
    //endregion

    //region ISFOtaV3ManagerCallback
    @Override
    public void onManagerStatusChanged(int status) {
        SFLog.i(TAG, "ota v3 onManagerStatusChanged %d", status);
    }

    @Override
    public void onProgress(long currentBytes, long totalBytes) {
        int progress = 0;
        this.speedView.viewSpeedByCompleteBytes(currentBytes);
        if (totalBytes > 0) {
            double percent = (double) currentBytes / totalBytes;
            progress = (int) (percent * 100);
        }
        int finalProgress = progress;
        this.progressBar.setProgress(finalProgress);
        this.speedTv.setText(speedView.getSpeedText(currentBytes, totalBytes));
    }

    @Override
    public void onComplete(boolean success, SFError error) {
        String msg = String.format("ota v3 manger complete success=%b,error=%s", success, error);
        SFLog.i(TAG, msg);
        this.addLog(msg);
        if(success && this.currentEditItem != null){

                if (this.currentEditItem.getEditItem().hasPatch()) {
                    SFLog.i(TAG,"update item to hasSend id=%s",this.currentEditItem.getEditItem().getControlId());
                    this.currentEditItem.setHasSend(true);
                }

        }
        this.dismissProgressHUD();
        this.imageAdapter.notifyDataSetChanged();
    }


    //endregion

    //region ISGPushMessageManagerCallback
    @Override
    public void onPushMessageManagerStatusChanged(int status) {
        String msg = String.format("onPushMessageManagerStatusChanged %d",status);
        SFLog.i(TAG,msg);
        this.addLog(msg);

    }

    @Override
    public void onPushMessageManagerComplete(boolean success, SFError error) {
        String msg = String.format("onPushMessageManagerComplete %b,error=%s",success,error);
        SFLog.i(TAG,msg);
        this.addLog(msg);
        if(success){
            if(this.currentMessage == MESSAGE_DELETE_RES_PATCH){
                this.onPushDeleteResPatchSuccess();
            }
        }
        this.dismissProgressHUD();
    }
    //endregion
}