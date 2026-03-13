package com.sifli.sifliapp.modules.debug;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sifli.sifliapp.MainActivity;
import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.debug.guibuilder.WatchfaceEditorActivity;
import com.sifli.sifliapp.modules.debug.ota.SFOtaV3Activity;
import com.sifli.sifliapp.modules.debug.pushsdkfile.PushSDKFileActivity;
import com.sifli.sifliapp.modules.devicescan.DeviceScanActivity;
import com.sifli.sifliapp.modules.pushapp.AppResActivity;
import com.sifli.sifliapp.modules.setting.PushAppSettingActivity;
import com.sifli.sifliapp.modules.user.SFUser;
import com.sifli.sifliapp.utils.StringUtil;
import com.sifli.siflicore.log.SFLog;

public class DebugHomeActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "PushAppSettingActivity";
    private View backView;
    private RelativeLayout otaV3Rl;
    private RelativeLayout pushAppResRl;
    private RelativeLayout searchDeviceRl;
    private RelativeLayout sdkFilePushRl;
    private RelativeLayout guibuilderSdkRl;
    private TextView macTv;
    private String targetMac;
    private ActivityResultLauncher<Intent> searchLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_home_activty);
        this.setupView();
        this.bindEvent();
        this.init();
    }

    private  void setupView(){
        this.backView = findViewById(R.id.sf_debug_home_back_iv);
        this.otaV3Rl = findViewById(R.id.sf_debug_home_otav3_rl);
        this.pushAppResRl = findViewById(R.id.sf_debug_home_push_app_res_rl);
        this.searchDeviceRl = findViewById(R.id.sf_debug_home_search_device_rl);
        this.sdkFilePushRl = findViewById(R.id.sf_debug_home_push_sdk_file_rl);
        this.guibuilderSdkRl = findViewById(R.id.sf_debug_home_guibuilder_sdk_demo_rl);
        this.macTv = findViewById(R.id.sf_debug_home_mac_tv);
    }

    private void bindEvent(){
        this.backView.setOnClickListener(this);
        this.otaV3Rl.setOnClickListener(this);
        this.pushAppResRl.setOnClickListener(this);
        this.searchDeviceRl.setOnClickListener(this);
        this.sdkFilePushRl.setOnClickListener(this);
        this.guibuilderSdkRl.setOnClickListener(this);
    }

    private void init(){
        this.createSearchLauncher();
        this.targetMac = SFUser.getInstance().getMac();
        if(this.targetMac != null){

            this.onQrResult(this.targetMac);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId == R.id.sf_debug_home_back_iv){
            SFLog.i(TAG,"go back");
            this.finish();
        }else if(viewId == R.id.sf_debug_home_otav3_rl){
            onOtaV3RlTouch();
        }else if(viewId == R.id.sf_debug_home_push_app_res_rl){
            onPushAppResRlTouch();
        }else if(viewId == R.id.sf_debug_home_search_device_rl){
            onSearchDeviceRlTouch();
        }else if(viewId == R.id.sf_debug_home_push_sdk_file_rl){
            onPushSDKFileRlTouch();
        }else if(viewId == R.id.sf_debug_home_guibuilder_sdk_demo_rl){
            onGUIBuilderSdkDemoRlTouch();
        }
    }

    private void onOtaV3RlTouch(){
        if(StringUtil.isNullOrEmpty(this.targetMac)){
            this.toast("请先搜索设备");
            return;
        }
        Intent intent = new Intent(DebugHomeActivity.this, SFOtaV3Activity.class);
        intent.putExtra(SFOtaV3Activity.EXTRA_BLE_DEVICE, this.targetMac);
        startActivity(intent);
    }

    private void onPushAppResRlTouch(){
        if(StringUtil.isNullOrEmpty(this.targetMac)){
            this.toast("请先搜索设备");
            return;
        }
        Intent intent = new Intent(DebugHomeActivity.this, AppResActivity.class);
        intent.putExtra(AppResActivity.EXTRA_BLE_DEVICE, this.targetMac);
        startActivity(intent);
    }

    private void onPushSDKFileRlTouch(){
        if(StringUtil.isNullOrEmpty(this.targetMac)){
            this.toast("请先搜索设备");
            return;
        }
        Intent intent = new Intent(DebugHomeActivity.this, PushSDKFileActivity.class);
        intent.putExtra(PushSDKFileActivity.EXTRA_BLE_DEVICE, this.targetMac);
        startActivity(intent);
    }

    private void onGUIBuilderSdkDemoRlTouch(){
        if(StringUtil.isNullOrEmpty(this.targetMac)){
            this.toast("请先搜索设备");
            return;
        }
        Intent intent = new Intent(DebugHomeActivity.this, WatchfaceEditorActivity.class);
        intent.putExtra(WatchfaceEditorActivity.EXTRA_BLE_DEVICE, this.targetMac);
        startActivity(intent);
    }

    private void onSearchDeviceRlTouch(){
        this.searchLauncher.launch(new Intent(DebugHomeActivity.this, DeviceScanActivity.class));
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

    private void onQrResult(String qrStr) {
        this.macTv.setText(qrStr);
        this.targetMac = qrStr;
    }

    private void toast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}