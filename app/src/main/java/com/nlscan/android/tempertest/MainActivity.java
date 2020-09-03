package com.nlscan.android.tempertest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.scan.ScanManager;
import com.nlscan.android.tempertest.util.Constants;
import com.nlscan.android.tempertest.util.PostUtil;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {


    private static final int CHANGE_SUCCESS = 1;
    private static final int CHANGE_TEMP_TEMPER = 2;
    private static final int CHANGE_FAILURE = 3;
    private static final int CHANGE_BROAD = 4;
    private static final int CHANGE_TEMPER = 5;
    private static final int CHANGE_DISAPPEAR = 6;
    private static final int CHANGE_DIALOG_ON = 7;
    private static final int CHANGE_DIALOG_OFF = 8;

    private long exitTime = 0;

    //控件列表

    @BindView(R.id.ll_person_left)
    RelativeLayout llPersonLeft;

    @BindView(R.id.ll_camera_notice)
    LinearLayout llCameraNotice;

    @BindView(R.id.ll_sensor_notice)
    LinearLayout llSensorNotice;

    @BindView(R.id.rv_person_list)
    RecyclerView rvInfoResult;


    @BindView(R.id.tv_info_tips)
    TextView tvInfoTips;

    @BindView(R.id.iil_name)
    InfoItemLayout item_name;

    @BindView(R.id.iil_certificate)
    InfoItemLayout item_certificate;

    @BindView(R.id.iil_temper)
    InfoItemLayout item_temper;

    @BindView(R.id.iil_time)
    InfoItemLayout item_time;

    @BindView(R.id.img_program)
    ImageView imgProgram;

    @BindView(R.id.iil_total)
    InfoItemLayout item_total;

    @BindView(R.id.iil_normal)
    InfoItemLayout item_normal;

    @BindView(R.id.iil_except)
    InfoItemLayout item_except;

    //服务器地址
    private boolean mIfJson = true;
    private String dataUrl = "http://www.nlsmall.com/emsExpress/support.do?barcodeQuery";


    //handler
    private MyHandler gMyHandler = new MyHandler(this);

    //对话框
    private ProgressDialog mDialog;

    //扫码广播相关
    private ScanManager mScanManager;
    private static final String ACTION_SCAN_RESULT = "nlscan.action.SCANNER_RESULT";


    //身份信息显示相关
    private long lastShowTime = 0L;
    private String currentName = "";
    private String currentCert = "";
    private String currentTime = "";
    private List<PersonInfo> dataList = new ArrayList<>();
    MyRVAdapter myRVAdapter;

    //温度显示相关
    private SoundPool soundPool;
    DecimalFormat gDf =  new  DecimalFormat(  "0.0" );
    AlertDialog gAlertDialog;
    private boolean ifDestroy =false;
    private boolean ifShow = false;
    private boolean ifSendDisappear = false;
    private static final double BASE_TEMPER = 37.3;
    private static final double LOWER_TEMPER = 35.5;
    private Handler mHandler;
    private static final int INTERVAL_TIME = 500;
    private static final int AVE_NUM = 5;

    private LinkedList<Double> gTemperList;
    private List<Boolean> sendFlagList;

    private double gCurrentTemper = 0.0;
    private double gRealTemper = 0.0;
    private int gNameReadCount = 0;
    private int gTotalCount = 0;
    private int gNormalCount = 0;
    private int gExceptCount = 0;
    private TemperModel temperModel = new TemperModel();

    /**
     * 普通广播
     */
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            final String scanResult_1=intent.getStringExtra("SCAN_BARCODE1");
            final String scanStatus=intent.getStringExtra("SCAN_STATE");

            if("ok".equals(scanStatus)){
                Message meg = Message.obtain();
                meg.what = CHANGE_BROAD;
                meg.obj = scanResult_1;
                gMyHandler.sendMessage(meg);
            }else{
//                Toast.makeText(MainActivity.this,"扫码失败",Toast.LENGTH_SHORT).show();
            }

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);// 横屏

        barcodeSet();
        registerResultReceiver();
        temperModel.initSerial();
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
               if(!ifDestroy){
                   gCurrentTemper = temperModel.currentTemper()[2];
//                   if (gTemperList.size() >= AVE_NUM) gTemperList.poll();
                   if (gCurrentTemper < LOWER_TEMPER - 4.0) gCurrentTemper = LOWER_TEMPER -0.5;
//                   gTemperList.add(gCurrentTemper);
                   if (ifShow) gMyHandler.sendEmptyMessage(CHANGE_TEMP_TEMPER);
                   mHandler.postDelayed(this,INTERVAL_TIME);
               }

            }
        },500);
    }



    /**
     * 设置返回键功能为退出
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * 延迟退出效果
     */
    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            System.exit(0);
        }
    }



    /**
     * 条码设置
     */
    private void barcodeSet(){
        //初始化扫码配置
        mScanManager = ScanManager.getInstance();
        Intent intentConfig = new Intent("ACTION_BAR_SCANCFG");
        intentConfig.putExtra("EXTRA_SCAN_MODE", 3);//广播输出
        intentConfig.putExtra("EXTRA_OUTPUT_EDITOR_ACTION_ENABLE", 0);//不输出软键盘
        sendBroadcast(intentConfig);

        //初始化温度数据
        gTemperList = new LinkedList<>();
        for (int i=0; i<AVE_NUM; i++){
            gTemperList.add(BASE_TEMPER);
        }

        //结束周期标记
        sendFlagList = new ArrayList<>();

        //设置控件显示状态
        initVisible();
        //初始化列表
        myRVAdapter = new MyRVAdapter(this,dataList);
        rvInfoResult.setAdapter(myRVAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        rvInfoResult.setLayoutManager(linearLayoutManager);
//        dataList.add(new PersonInfo("姓名","身份证","体温","登记时间"));
//        myRVAdapter.notifyDataSetChanged();
        //初始化蜂鸣器
        soundPool = new SoundPool(10, AudioManager.STREAM_RING, 5);
        soundPool.load(this, R.raw.beep51, 1);

    }



    /**
     * 关闭进度条
     */
    protected void  cancelDialog(){
        if (mDialog != null){
            mDialog.dismiss();
        }
    }


    /**
     * 显示进度条
     * @param message
     */
    protected void showLoadingWindow(String message)
    {


        if(mDialog != null && mDialog.isShowing())
            return ;

        mDialog = new ProgressDialog(MainActivity.this) ;
        mDialog.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL);// 设置进度条的形式为圆形转动的进度条
        mDialog.setCancelable(true);// 设置是否可以通过点击Back键取消
        mDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
        // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
        mDialog.setMessage(message);
        mDialog.show();
    }



    /**
     * 查询身份信息并显示
     * @param queryCode
     */
    private void queryNameInfo(String queryCode){
        final String para = queryCode;
        showLoadingWindow("获取数据中");

        new Thread()
        {
            @Override
            public void run()
            {

                Map<String,String> map = new HashMap<>();
                Date d1 = new Date(System.currentTimeMillis());
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                map.put("barcodedata",para);
                map.put("optime",df.format(d1));

                String response = PostUtil.sendPost(
                        dataUrl,map,"utf-8",mIfJson);
                Log.d(Constants.TAG,response);

                String resultStr = PostUtil.parseJsonStr(response,"result",false);
                String personInfo = PostUtil.parseJsonStr(response,Constants.FIELD_INFO,true);
                if ("0000".equals(resultStr)){
                    currentName = PostUtil.parseJsonStr(personInfo,Constants.FIELD_NAME,false);
                    currentCert = PostUtil.parseJsonStr(personInfo,Constants.FIELD_CERTI,false);
                }
                Message toastMeg = Message.obtain();
                String megStr = PostUtil.parseJsonStr(response,"msg",false);
                toastMeg.obj = "".equals(megStr) ? "无效码" : megStr;
                if(megStr != null && megStr.length() > 3 && megStr.substring(megStr.length()-3,
                        megStr.length()-1).equals("过期"))   toastMeg.obj = "二维码过期";
                toastMeg.what = "0000".equals(resultStr) ?
                        CHANGE_SUCCESS : CHANGE_FAILURE;
                gMyHandler.sendMessage(toastMeg);


            }

        }.start();
    }


    /**
     *显示信息
     */
    private void showNameInfo(){


//        ifSendDisappear = false;
        gNameReadCount++;
        int gCurrentNameCount = gNameReadCount;
        sendFlagList.add(false);
        mScanManager.setScanEnable(false);
        tvInfoTips.setText(getResources().getString(R.string.info_tips));
        item_certificate.setItemVisibility(View.VISIBLE);
        item_name.setItemVisibility(View.VISIBLE);
        imgProgram.setVisibility(View.INVISIBLE);
        llPersonLeft.setBackgroundColor(getResources().getColor(R.color.yellow));
        llCameraNotice.setVisibility(View.INVISIBLE);
        llSensorNotice.setVisibility(View.VISIBLE);

        item_name.setContainText(hideStr(currentName,1,1));
        item_certificate.setContainText(hideStr(currentCert,4,2));



        //获取当前温度
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                gMyHandler.sendEmptyMessage(CHANGE_TEMPER);
            }
        },500);


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!sendFlagList.get(gCurrentNameCount-1))
                    gMyHandler.sendEmptyMessage(CHANGE_DISAPPEAR);
            }
        },10000);

    }


    /**
     * 显示温度变化
     */
    private void showTempeChange(){

        gCurrentTemper = temperModel.currentTemper()[2];

        if (gCurrentTemper > LOWER_TEMPER && "".equals(item_temper.getContainText()) && ifShow){
            try {
                gRealTemper = Double.valueOf(gDf.format(gCurrentTemper));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                gRealTemper = gCurrentTemper;
            }
            item_temper.setContainText(""+gRealTemper + "℃");
            item_temper.setContainColor(gRealTemper > BASE_TEMPER ?
                    getResources().getColor(R.color.red) : getResources().getColor(R.color.green));
            if (gRealTemper > BASE_TEMPER){
                int playId =    performSound();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        soundPool.stop(playId);
                    }
                },3000);
                showDialog(""+gRealTemper + "℃" + ", 体温异常!!!",Color.RED);
            }
            else{
                showDialog(""+gRealTemper + "℃" + ", 体温正常",Color.GREEN);

            }

            Date d1 = new Date(System.currentTimeMillis());
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            currentTime = df.format(d1);
            item_time.setContainText(currentTime);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    gMyHandler.sendEmptyMessage(CHANGE_DIALOG_OFF);
                }
            },1500);
                gMyHandler.sendEmptyMessage(CHANGE_DISAPPEAR);

        }


    }

    /**
     * 发出警报
     */
    private int performSound(){
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // 获取最大音量值
        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_RING);
        // 不断获取当前的音量值
        float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_RING);
        //最终影响音量
//        float volumnRatio = audioCurrentVolumn/audioMaxVolumn;
        float volumnRatio = 1.0f;
        return soundPool.play(1, volumnRatio, volumnRatio, 0, -1, 1);
    }


    /**
     * 显示弹出窗
     * @param meg
     */
    private void showDialog(String meg,int colorID){
        gAlertDialog = new AlertDialog.Builder(this).
                setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gAlertDialog.dismiss();
                    }
                }).create();
        gAlertDialog.setTitle("测温结果");

        gAlertDialog.setMessage(meg);
        gAlertDialog.show();

        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(gAlertDialog);
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            mMessageView.setTextColor(colorID);
            mMessageView.setTextSize(25);
            mMessageView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }



    }

    /**
     * 关闭弹出窗
     */
    private void cancelWindowDialog(){
        if (gAlertDialog != null)
            gAlertDialog.dismiss();
    }



    /**
     * 开启温度显示
     */
    private void showTemper(){
        ifShow = true;
        item_temper.setItemVisibility(View.VISIBLE);
        item_time.setItemVisibility(View.VISIBLE);
    }


    /**
     * 取消信息显示并统计
     */
    private void setDisappear(){




//        if (gAlertDialog != null)
//            gAlertDialog.dismiss();

        sendFlagList.set(gNameReadCount-1,true);

        ifShow = false;

        if (gRealTemper < LOWER_TEMPER)
            Toast.makeText(this,"未检测到人体体温，请再次扫码后重新检测",Toast.LENGTH_SHORT).show();


        //统计人数


        if (gRealTemper >=LOWER_TEMPER ){
            gTotalCount++;
            if (gRealTemper > BASE_TEMPER){
                gExceptCount++;
            }
            else{
                gNormalCount++;
            }
        }




        if (gRealTemper >= LOWER_TEMPER ){
            PersonInfo personInfo = new PersonInfo(hideStr(currentName,1,1),
                    hideStr(currentCert,4,2), gDf.format(gRealTemper), currentTime);
            dataList.add(personInfo);
            myRVAdapter.notifyDataSetChanged();
            rvInfoResult.scrollToPosition(dataList.size()-1);
        }


        item_total.setContainText(""+gTotalCount);
        item_normal.setContainText(""+gNormalCount);
        item_except.setContainText(""+gExceptCount);

        initVisible();

        mScanManager.setScanEnable(true);
        currentTime = "";
        gRealTemper = 0.0;
//        ifSendDisappear = true;


    }


    /**
     * 初始化控件的显示状态
     */
    private void initVisible(){

        item_name.setContainText("");
        item_certificate.setContainText("");
        item_temper.setContainText("");
        item_time.setContainText("");

        tvInfoTips.setText(getResources().getString(R.string.program_tips));
        item_temper.setItemVisibility(View.INVISIBLE);
        item_name.setItemVisibility(View.INVISIBLE);
        item_certificate.setItemVisibility(View.INVISIBLE);
        item_time.setItemVisibility(View.INVISIBLE);
        imgProgram.setVisibility(View.VISIBLE);
        llPersonLeft.setBackgroundColor(getResources().getColor(R.color.white));
        llCameraNotice.setVisibility(View.VISIBLE);
        llSensorNotice.setVisibility(View.INVISIBLE);

    }


    /**
     * 返回温度平均值
     * @param linkedList
     * @return
     */
    private double getTemperAve(LinkedList<Double> linkedList){
        double resultTemper = 0.0;
        if (linkedList != null && linkedList.size() == AVE_NUM) {
            for (int i = 0; i < AVE_NUM; i++) {
                resultTemper += linkedList.get(i) / AVE_NUM;
            }
        }
        return resultTemper;
    }

    /**
     * 隐藏字符串的中间字符
     * @return
     */
    private String hideStr(String str,int head, int tail){

        if (str == null) return  "";
        StringBuilder sb = new StringBuilder("");
        int size = str.length();
        if (size < 2)  return str;

        if (size == 2) return    str.charAt(0) + "*";

        int step1 = head < size ? head : 1;
        int step2 = tail  < size ?  tail : 1 ;
        int hideCount = size - step1 - step2;
        if (hideCount < 0) {
            hideCount = size -2;
            step1 = 1;
            step2 =1;
        }
        sb.append(str.substring(0,step1));
        for (int i=0; i<hideCount; i++) sb.append("*");
        return sb.append(str.substring(size-step2,size)).toString();

    }

    /**
     * 获取当前温度
     */
    private void getTemper(){

    }

    /**
     * 界面唤醒时注册广播
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerResultReceiver();
    }

    /**
     * 界面销毁时注销广播
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ifDestroy = true;
        unRegisterResultReceiver();
    }



    //注册广播
    private void registerResultReceiver() {
        try {
            IntentFilter scanFilter = new IntentFilter(ACTION_SCAN_RESULT);
            registerReceiver(mScanReceiver,scanFilter);

        } catch (Exception e) {
        }

    }

    //注销广播
    private void unRegisterResultReceiver() {
        try {
            unregisterReceiver(mScanReceiver);
        } catch (Exception e) {
        }

    }






    /**
     * 静态Handler
     */
    static class MyHandler extends Handler {

        private SoftReference<MainActivity> mySoftReference;

        public MyHandler(MainActivity mainActivity) {
            this.mySoftReference = new SoftReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg){
            final MainActivity mainActivity = mySoftReference.get();
            String str = (String) msg.obj;
            switch (msg.what) {

                case CHANGE_SUCCESS:
                    mainActivity.cancelDialog();
                    mainActivity.showNameInfo();
//                    Toast.makeText(mainActivity,"查询成功",Toast.LENGTH_SHORT).show();
                    break;
                case CHANGE_FAILURE:
                    mainActivity.cancelDialog();
                    Toast.makeText(mainActivity,str,Toast.LENGTH_SHORT).show();
                    break;
                case CHANGE_TEMP_TEMPER:
                    mainActivity.showTempeChange();
                    break;
                case CHANGE_TEMPER:
                    mainActivity.showTemper();
                    break;
                case CHANGE_BROAD:
                    mainActivity.queryNameInfo(str);
                    break;
                case CHANGE_DISAPPEAR:
                    mainActivity.setDisappear();
                    break;
                case CHANGE_DIALOG_OFF:
                    mainActivity.cancelWindowDialog();
                    break;
            }

        }
    }


}
