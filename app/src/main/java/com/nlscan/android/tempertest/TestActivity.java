package com.nlscan.android.tempertest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TestActivity extends AppCompatActivity {


    @BindView(R.id.text_rel)
    TextView textView;

    private boolean ifDestroy =false;

    DecimalFormat gDf =  new  DecimalFormat(  "0.0" );


    private Handler mHandler;
    private TemperModel temperModel = new TemperModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);

        temperModel.initSerial();
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!ifDestroy){
                    double gCurrentTemper = temperModel.currentTemper()[2];
                    double ta = temperModel.currentTemper()[0];
                    double tfore = temperModel.currentTemper()[1];

                    StringBuilder sb = new StringBuilder("");
                    sb.append("铜线温度：");
                    sb.append(gDf.format(ta) + "℃, ");
                    sb.append("表面温度：");
                    sb.append( gDf.format(tfore)+ "℃, ");
                    sb.append("人体温度：");
                    sb.append(gDf.format(gCurrentTemper) + "℃");
//
                    textView.setText(sb.toString() );
                    mHandler.postDelayed(this,500);
                }

            }
        },500);


    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        ifDestroy = true;
    }



}
