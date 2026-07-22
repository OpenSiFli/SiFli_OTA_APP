package com.sifli.sifliapp.modules.debug.pushsdkfile;

import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_LOG;
import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_PROGRESS;
import static com.sifli.siflidfu.SifliDFUService.BROADCAST_DFU_STATE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_PROGRESS;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_PROGRESS_TYPE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_STATE;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_DFU_STATE_RESULT;
import static com.sifli.siflidfu.SifliDFUService.EXTRA_LOG_MESSAGE;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sifli.sifliapp.R;
import com.sifli.sifliapp.modules.debug.ota.SFOtaImageItem;
import com.sifli.sifliapp.utils.FileUtil;
import com.sifli.siflicore.log.SFLog;
import com.sifli.siflicore.util.StringUtil;
import com.sifli.siflidfu.DFUImagePath;
import com.sifli.siflidfu.ISifliDFUService;
import com.sifli.siflidfu.SifliDFUService;

import java.util.ArrayList;

public class PushSDKFileActivity extends AppCompatActivity implements View.OnClickListener{
    private final static String TAG = "PushSDKFileActivity";
    public final static String EXTRA_BLE_DEVICE = "EXTRA_BLE_DEVICE";
    private final static int COMMAND_SELECT_RES_FILE = 1;

    private TextView macAddressTv;
    private TextView logTv;
    private ScrollView logSv;
    private TextView progressTv;
    private ProgressBar progressBar;
    private TextView resTv;

    private Button selectResBtn;
    private Button startBtn;
    private Button stopBtn;

    private StringBuilder logBuilder;
    private String targetMac = "FF:FF:79:DA:77:C8";//525
    private String offlineFilePath;

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
        setContentView(R.layout.activity_push_sdkfile);
        this.setupView();
        this.bindEvent();
        this.init();
        localBroadcastReceiver = new LocalBroadcastReceiver();
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
        macAddressTv = findViewById(R.id.sf_push_sdk_mac_tv);
        logTv = findViewById(R.id.sf_push_sdk_log_tv);
        logSv = findViewById(R.id.sf_push_sdk_log_sv);
        progressTv = findViewById(R.id.sf_push_sdk_progress_tv);
        progressBar = findViewById(R.id.sf_push_sdk_progress_pb);
        resTv = findViewById(R.id.sf_push_sdk_res_tv);

        selectResBtn = findViewById(R.id.sf_push_sdk_select_file_btn);
        startBtn = findViewById(R.id.sf_push_sdk_start_btn);
        stopBtn = findViewById(R.id.sf_push_sdk_stop_btn);
    }

    private void bindEvent(){
        selectResBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
    }

    private void init(){
        String mac = getIntent().getStringExtra(EXTRA_BLE_DEVICE);
        if(mac != null){
            targetMac = mac;
        }
        macAddressTv.setText(mac);
        this.logBuilder = new StringBuilder();
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
                    //Log.d(TAG, "DFU LOG - " + DFULog);
//                    updateLogText(DFULog);
                    break;
                case BROADCAST_DFU_STATE:
                    int dfuState = intent.getIntExtra(EXTRA_DFU_STATE, 0);
                    int dfuStateResult = intent.getIntExtra(EXTRA_DFU_STATE_RESULT, 0);
                    String msg = "dfuState=" + dfuState + ",dfuStateResult=" + dfuStateResult;
                    SFLog.i(TAG,msg);
                    addLog(msg);
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

    private void addLog(String log){
        if(this.logBuilder == null || this.logBuilder.length() > 1500){
            this.logBuilder = new StringBuilder();
        }
        String ts = com.sifli.sifliapp.utils.StringUtil.getTimeStr();
        this.logBuilder.append(ts + log+ "\n");

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
        if(viewId == R.id.sf_push_sdk_select_file_btn){
            onSelectResBtnTouch();
        }else if(viewId == R.id.sf_push_sdk_start_btn){
            onStartBtnTouch();
        }else if(viewId == R.id.sf_push_sdk_stop_btn){
            onStopBtnTouch();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null)return;
        if(requestCode == COMMAND_SELECT_RES_FILE){
            Uri uri = data.getData();
            Log.d(TAG, "res uri " + uri);
            this.offlineFilePath = FileUtil.getFilePathFromURI(this,uri);
            String fileName = FileUtil.getUrlName(uri,this);
            this.resTv.setText(fileName);
            SFLog.i(TAG,"filepath=" + this.offlineFilePath);
        }
    }

    private void onSelectResBtnTouch(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, COMMAND_SELECT_RES_FILE);
    }

    private void onStartBtnTouch(){
        SFLog.i(TAG,"onStartBtnTouch");
        String bluetoothAddress = this.targetMac;
       if(StringUtil.isNullOrEmpty(bluetoothAddress)){
           this.toast("Bluetooth Mac address is Required");
           return;
       }

       if(StringUtil.isNullOrEmpty(this.offlineFilePath)){
           this.toast("offlineFilePath is Required");
           return;
       }

        sifliDFUService.startActionDFUNorOffline(this,bluetoothAddress,null,this.offlineFilePath);
    }

    private void onStopBtnTouch(){
        SFLog.i(TAG,"onStopBtnTouch");
//        Intent intent = new Intent(this, SifliDFUService.class);
//        this.stopService(intent);
        this.finish();
    }

    private void toast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}