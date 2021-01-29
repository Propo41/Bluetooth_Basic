package com.pixeumstudios.bluetoothbasics;


import android.icu.lang.UScript;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceListRecyclerAdapter extends RecyclerView.Adapter<DeviceListRecyclerAdapter.DeviceListViewHolder> {


    private ArrayList<DeviceInfo> mExampleItems;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }


    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ConstraintLayout mRootView;
        public OnItemClickListener customListener;

        public DeviceListViewHolder(View itemView, final OnItemClickListener customListener) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.textView);
            mRootView = itemView.findViewById(R.id.root);
            this.customListener = customListener;
        }
    }

    // to extract the data from the Array list (that we created in out MainActivity file) we create a constructor
    public DeviceListRecyclerAdapter(ArrayList<DeviceInfo> exampleItems) {
        mExampleItems = exampleItems;
    }

    @NonNull
    @Override
    public DeviceListRecyclerAdapter.DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // we need to inflate the views and add those views to the view holder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);

        // now we return the view holder object
        return new DeviceListViewHolder(view, mListener);

    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListRecyclerAdapter.DeviceListViewHolder holder, int position) {

        // we got the data of ArrayList<ExampleItem> from the constructor
        // now what this method does is that it returns the data at position "int position" from the array
        // to bind it to the viewholder
        DeviceInfo currentItem = mExampleItems.get(position);
        holder.mTextView.setText(currentItem.getName());
        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        // we need to return how many items are there in our array list
        return mExampleItems.size();
    }


}