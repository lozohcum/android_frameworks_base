
package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mData;
    public Context context;

    public NetworkListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        init();
        this.context = context;
    }

    private void init() {
        mData = new ArrayList<Map<String, Object>>();
    }

    public void addItem(NetworkInfomation mNetworkInfo) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("image", mNetworkInfo.mLevelDrawable);
        map.put("title", mNetworkInfo.mTitleText);
        map.put("info", mNetworkInfo.mConnectText);

        mData.add(map);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void remove(int position) {
        mData.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        NetworkViewHolder viewHolder = null;
        // initize convertView when convertView is null
        if (convertView == null) {
            viewHolder = new NetworkViewHolder();
            convertView = mInflater.inflate(R.layout.network_list_item, null);
            viewHolder.mLevelView = (ImageView) convertView
                    .findViewById(R.id.img);
            viewHolder.mTitle = (TextView) convertView
                    .findViewById(R.id.ssid);
            viewHolder.mConnected = (TextView) convertView
                    .findViewById(R.id.info);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (NetworkViewHolder) convertView.getTag();
        }
        final NetworkViewHolder vHolder = viewHolder;

        vHolder.mLevelView.setImageDrawable(
                (Drawable) mData.get(position).get("image"));
        vHolder.mTitle.setText(
                (String) mData.get(position).get("title"));
        vHolder.mConnected.setText(
                (String) mData.get(position).get("info"));

        return convertView;
    }

    public final class NetworkViewHolder {
        public ImageView mLevelView;
        public TextView mTitle;
        public TextView mConnected;
    }

    public static final class NetworkInfomation {
        public Drawable mLevelDrawable;
        public String mTitleText;
        public String mConnectText;
    }
}
