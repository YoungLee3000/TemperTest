package com.nlscan.android.tempertest;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyRVAdapter extends RecyclerView.Adapter<MyRVAdapter.MyTVHolder> {

    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private  List<PersonInfo> mData;
    private static final double LOWER_TEMPER = 35.5;
    private static final double BASE_TEMPER = 37.3;

    public MyRVAdapter(Context context, List<PersonInfo> dataList) {
        mLayoutInflater = LayoutInflater.from(context);
        mContext = context;

        mData = dataList;
    }

    @Override
    public MyRVAdapter.MyTVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyRVAdapter.MyTVHolder(mLayoutInflater.inflate(R.layout.text_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final MyRVAdapter.MyTVHolder holder, int pos) {
        holder.view.setBackgroundColor(pos % 2 == 0 ?mContext.getResources().getColor(R.color.gray):
                                                     mContext.getResources().getColor(R.color.white));
        double currentTemper = 0.0;
        try {
            currentTemper = Double.valueOf(mData.get(pos).getTemperVal()) ;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (currentTemper > BASE_TEMPER) {
            holder.mTemperView.setTextColor(mContext.getResources().getColor(R.color.red));
        }
        else{
            holder.mTemperView.setTextColor(currentTemper < LOWER_TEMPER ?
                    mContext.getResources().getColor(R.color.black)
                    :mContext.getResources().getColor(R.color.green) );
        }



//        if (pos ==0){
//            holder.mIndexView.setText("序号");
//        }
//        else {
            holder.mIndexView.setText(""+ (pos+1));

            if (pos == mData.size() - 1){
                holder.view.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
            }
//        }


        holder.mNameView.setText(mData.get(pos).getName());
        holder.mCertView.setText(mData.get(pos).getCertificate());
        holder.mTemperView.setText(mData.get(pos).getTemperVal() + "℃");
        holder.mDateView.setText(mData.get(pos).getDate());
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    class MyTVHolder extends RecyclerView.ViewHolder {
        TextView mIndexView;
        TextView mNameView;
        TextView mCertView;
        TextView mTemperView;
        TextView mDateView;
        View view;

        MyTVHolder(View itemView) {
            super(itemView);
            mIndexView = (TextView) itemView.findViewById(R.id.tv_item_index);
            mNameView = (TextView) itemView.findViewById(R.id.tv_item_name);
            mCertView = (TextView) itemView.findViewById(R.id.tv_item_cert);
            mTemperView = (TextView) itemView.findViewById(R.id.tv_item_temper);
            mDateView = (TextView) itemView.findViewById(R.id.tv_item_date);
            view = itemView;
        }
    }
}