package com.nlscan.android.tempertest;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import butterknife.BindView;

public class InfoItemLayout extends RelativeLayout {

    private TextView tvTitle,tvContain;





    public InfoItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        //加载自定义布局
        LayoutInflater.from(context).inflate(R.layout.info_item,this);


         tvTitle = (TextView)  findViewById(R.id.tv_title);
         tvContain = (TextView)  findViewById(R.id.tv_contain);

        /*
            每一个属性集合编译之后都会对应一个styleable对象,
            通过styleable对象获取TypedArray typedArray，然后通过键值对获取属性值。
            R.styleable.SettingsItemLayout,SettingsItemLayout对应attrs里面属性集的名称而不是本类的类名
         */
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfoItemLayout);

        String textTitle = typedArray.getString(R.styleable.InfoItemLayout_title_text);
        String textContain = typedArray.getString(R.styleable.InfoItemLayout_contain_text);
        int titleColor = typedArray.getColor(R.styleable.InfoItemLayout_title_color,Color.BLACK);
        int colorID = typedArray.getColor(R.styleable.InfoItemLayout_contain_color, Color.GRAY);
        int visibility = typedArray.getInt(R.styleable.InfoItemLayout_info_visibility,VISIBLE);
        int containBack = typedArray.getColor(R.styleable.InfoItemLayout_contain_back,Color.WHITE);

        float titleSize = typedArray.getDimension(R.styleable.InfoItemLayout_title_size,22.0f);
        float containSize = typedArray.getDimension(R.styleable.InfoItemLayout_contain_size,22.0f);

        typedArray.recycle();

        //将自定义的属性值设置到组件上
        tvTitle.setText(textTitle);
        tvTitle.setTextColor(titleColor);
        tvContain.setText(textContain);
        tvContain.setTextColor(colorID);
        tvTitle.setTextSize(titleSize);
        tvContain.setTextSize(containSize);

        tvTitle.setVisibility(visibility);
        tvContain.setVisibility(visibility);
        tvContain.setBackgroundColor(containBack );

    }



    /*
        设置组件内容，不同参数，字符串或者资源id
     */
    public void setTitleText(String titleText){
        tvTitle.setText(titleText);
    }

    public void setTitleText(int titleText){
        tvTitle.setText(titleText);
    }



    public String getContainText(){return tvContain.getText().toString();}

    public void setContainText(String containText){
        tvContain.setText(containText);
    }

    public void setContainText(int containText){
        tvContain.setText(containText);
    }

    public void setContainColor(int containColor){
        tvContain.setTextColor(containColor);
    }


    public void setTitleColor(int containColor){
        tvTitle.setTextColor(containColor);
    }

    public void setTitleSize(float titleSize){tvTitle.setTextSize(titleSize); }

    public void setContainSize(float containSize){tvTitle.setTextSize(containSize); }


    public void setBack(int colorId){
        tvContain.setBackgroundColor(colorId);
    }

    public  void  setItemVisibility(int visibility){
        tvContain.setVisibility(visibility);
        tvTitle.setVisibility(visibility);
    }







}
