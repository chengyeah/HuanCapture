package com.huan.capture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import eskit.sdk.support.messenger.client.bean.EsDevice;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private Context mContext;
    private OnItemClickListener mListener;
    private final List<EsDevice> mAdapterList = new ArrayList<>();

    public DeviceAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DeviceAdapter.ViewHolder holder, int position) {
        if (!mAdapterList.isEmpty()) {
            holder.tvDeviceName.setText(mAdapterList.get(position).getDeviceName());
            holder.tvDeviceIp.setText(mAdapterList.get(position).getDeviceIp() + ":" + mAdapterList.get(position).getDevicePort());
            holder.llItemBox.setOnClickListener(view -> {
                mListener.onItemClick(mAdapterList.get(position));
            });
        }
    }

    @Override
    public int getItemCount() {
        return mAdapterList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<EsDevice> mList) {
        mAdapterList.clear();
        this.mAdapterList.addAll(mList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDeviceName;
        private final TextView tvDeviceIp;
        private final LinearLayout llItemBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceIp = itemView.findViewById(R.id.tvDeviceIp);
            llItemBox = itemView.findViewById(R.id.llItemBox);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(EsDevice device);
    }
}
