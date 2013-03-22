/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeInternalListener;
import android.preference.PreferenceActivity.Header;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.R;

/**
 * An adapter that returns the {@link Preference} contained in this group.
 * In most cases, this adapter should be the base class for any custom
 * adapters from {@link Preference#getAdapter()}.
 * <p>
 * This adapter obeys the
 * {@link Preference}'s adapter rule (the
 * {@link Adapter#getView(int, View, ViewGroup)} should be used instead of
 * {@link Preference#getView(ViewGroup)} if a {@link Preference} has an
 * adapter via {@link Preference#getAdapter()}).
 * <p>
 * This adapter also propagates data change/invalidated notifications upward.
 * <p>
 * This adapter does not include this {@link PreferenceGroup} in the returned
 * adapter, use {@link PreferenceCategoryAdapter} instead.
 * 
 * @see PreferenceCategoryAdapter
 */
class ShenDuPreferenceGroupAdapter extends BaseAdapter implements OnPreferenceChangeInternalListener {
    
    private static final String TAG = "ShenDuPreferenceGroupAdapter";

    /**
     * The group that we are providing data from.
     */
    private PreferenceGroup mPreferenceGroup;
    
    /**
     * Maps a position into this adapter -> {@link Preference}. These
     * {@link Preference}s don't have to be direct children of this
     * {@link PreferenceGroup}, they can be grand children or younger)
     */
    private List<Preference> mPreferenceList;
    
    /**
     * List of unique Preference and its subclasses' names. This is used to find
     * out how many types of views this adapter can return. Once the count is
     * returned, this cannot be modified (since the ListView only checks the
     * count once--when the adapter is being set). We will not recycle views for
     * Preference subclasses seen after the count has been returned.
     */
    private ArrayList<PreferenceLayout> mPreferenceLayouts;

    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

	private boolean isLight;
    /**
     * Blocks the mPreferenceClassNames from being changed anymore.
     */
    private boolean mHasReturnedViewTypeCount = false;
    
    private volatile boolean mIsSyncing = false;
    
    private Handler mHandler = new Handler(); 
    
    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            syncMyPreferences();
        }
    };

    private static class PreferenceLayout implements Comparable<PreferenceLayout> {
        private int resId;
        private int widgetResId;
        private String name;

        public int compareTo(PreferenceLayout other) {
            int compareNames = name.compareTo(other.name);
            if (compareNames == 0) {
                if (resId == other.resId) {
                    if (widgetResId == other.widgetResId) {
                        return 0;
                    } else {
                        return widgetResId - other.widgetResId;
                    }
                } else {
                    return resId - other.resId;
                }
            } else {
                return compareNames;
            }
        }
    }

    public ShenDuPreferenceGroupAdapter(PreferenceGroup preferenceGroup ,boolean light) {
        mPreferenceGroup = preferenceGroup;
        // If this group gets or loses any children, let us know
        mPreferenceGroup.setOnPreferenceChangeInternalListener(this);

        mPreferenceList = new ArrayList<Preference>();
        mPreferenceLayouts = new ArrayList<PreferenceLayout>();
		isLight = light;
        syncMyPreferences();
    }

    private void syncMyPreferences() {
        synchronized(this) {
            if (mIsSyncing) {
                return;
            }

            mIsSyncing = true;
        }

        List<Preference> newPreferenceList = new ArrayList<Preference>(mPreferenceList.size());
        flattenPreferenceGroup(newPreferenceList, mPreferenceGroup);
        mPreferenceList = newPreferenceList;
        
        notifyDataSetChanged();

        synchronized(this) {
            mIsSyncing = false;
            notifyAll();
        }
    }
    
    private void flattenPreferenceGroup(List<Preference> preferences, PreferenceGroup group) {
        // TODO: shouldn't always?
        group.sortPreferences();

        final int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            final Preference preference = group.getPreference(i);
            
            preferences.add(preference);
            
            if (!mHasReturnedViewTypeCount && !preference.hasSpecifiedLayout()) {
                addPreferenceClassName(preference);
            }
            
            if (preference instanceof PreferenceGroup) {
                final PreferenceGroup preferenceAsGroup = (PreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }

            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    /**
     * Creates a string that includes the preference name, layout id and widget layout id.
     * If a particular preference type uses 2 different resources, they will be treated as
     * different view types.
     */
    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout in) {
        PreferenceLayout pl = in != null? in : new PreferenceLayout();
        pl.name = preference.getClass().getName();
        pl.resId = preference.getLayoutResource();
        pl.widgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    private void addPreferenceClassName(Preference preference) {
        final PreferenceLayout pl = createPreferenceLayout(preference, null);
        int insertPos = Collections.binarySearch(mPreferenceLayouts, pl);

        // Only insert if it doesn't exist (when it is negative).
        if (insertPos < 0) {
            // Convert to insert index
            insertPos = insertPos * -1 - 1;
            mPreferenceLayouts.add(insertPos, pl);
        }
    }
    
    public int getCount() {
        return mPreferenceList.size();
    }

    public Preference getItem(int position) {
        if (position < 0 || position >= getCount()) return null;
        return mPreferenceList.get(position);
    }

    public long getItemId(int position) {
        if (position < 0 || position >= getCount()) return ListView.INVALID_ROW_ID;
        return this.getItem(position).getId();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final Preference preference = this.getItem(position);
        // Build a PreferenceLayout to compare with known ones that are cacheable.
        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        // If it's not one of the cached ones, set the convertView to null so that 
        // the layout gets re-created by the Preference.
        if (Collections.binarySearch(mPreferenceLayouts, mTempPreferenceLayout) < 0) {
            convertView = null;
        }

			int full=R.drawable.shendu_listitem_select_full;
			int top = R.drawable.shendu_listitem_select_top;
			int bottom = R.drawable.shendu_listitem_select_bottom;
			int middle =R.drawable.shendu_listitem_select_middle;
			int titleColor = com.android.internal.R.color.shendu_adapter_title_text_color;
			int summaryColor = com.android.internal.R.color.shendu_adapter_summary_text_color;
		if(!isLight){
			 full=R.drawable.shendu_listitem_select_full;
			 top = R.drawable.shendu_listitem_select_top;
			 bottom = R.drawable.shendu_listitem_select_bottom;
			 middle =R.drawable.shendu_listitem_select_middle;
			 titleColor = com.android.internal.R.color.shendu_adapter_title_text_color;
			 summaryColor = com.android.internal.R.color.shendu_adapter_summary_text_color;
        } else {
			 full=R.drawable.shendu_listitem_select_full_light;
			 top = R.drawable.shendu_listitem_select_top_light;
			 bottom = R.drawable.shendu_listitem_select_bottom_light;
			 middle =R.drawable.shendu_listitem_select_middle_light;
			 titleColor = com.android.internal.R.color.shendu_adapter_title_text_color_light;
			 summaryColor = com.android.internal.R.color.shendu_adapter_summary_text_color_light;
		}
		View view = preference.getView(convertView, parent);
        //set the background
        if (position == 0) {
            if (position == (getCount() - 1)) {
                view.setBackgroundResource(full);
            } else if (getItem(position + 1) instanceof PreferenceCategory){
                view.setBackgroundResource(full);
            } else {
                view.setBackgroundResource(top);
            }
        } else if (position == (getCount() - 1)) {
            if (getItem(position - 1) instanceof PreferenceCategory) {
                view.setBackgroundResource(full);
            } else {
                view.setBackgroundResource(bottom);
            }
        } else {
            Preference prev = getItem(position - 1);
            Preference next = getItem(position + 1);

            if (prev instanceof PreferenceCategory
                    && next instanceof PreferenceCategory) {
                view.setBackgroundResource(full);
            } else if (prev instanceof PreferenceCategory) {
                view.setBackgroundResource(top);
            } else if (next instanceof PreferenceCategory){
                view.setBackgroundResource(bottom);
            } else {
                view.setBackgroundResource(middle);
            }
        }

        if (preference instanceof PreferenceCategory) {
		LayoutParams lp = view.getLayoutParams();
		lp.height = view.getResources().getDimensionPixelSize(com.android.internal.R.dimen.shendu_listview_category_height);
		view.setLayoutParams(lp);
                view.setBackgroundResource(android.R.color.transparent);
        }

        TextView title = (TextView) view.findViewById(
                com.android.internal.R.id.title);
        TextView summary = (TextView) view.findViewById(
                com.android.internal.R.id.summary);
        if (title != null) {
            try {
                Context context = view.getContext();
                Resources resource = context.getResources();
                XmlResourceParser xrp = resource.getXml(titleColor);
                ColorStateList cl = ColorStateList.createFromXml(resource, xrp);

                title.setTextColor(cl);
                title.setPadding(view.getContext().getResources().getDimensionPixelSize(R.dimen.shendu_listitem_padding), 0, 0, 0);

            } catch (Exception e) {

            }
        }
        if (summary != null) {
            try {
                Context context = view.getContext();
                Resources resource = context.getResources();
                XmlResourceParser xrp = resource.getXml(summaryColor);
                ColorStateList cl = ColorStateList.createFromXml(resource, xrp);

                summary.setTextColor(cl);
                summary.setPadding(view.getContext().getResources().getDimensionPixelSize(R.dimen.shendu_listitem_padding), 0, 0, 0);

            } catch (Exception e) {

            }
        }

        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        if (position < 0 || position >= getCount()) return true;
        return this.getItem(position).isSelectable();
    }

    @Override
    public boolean areAllItemsEnabled() {
        // There should always be a preference group, and these groups are always
        // disabled
        return false;
    }

    public void onPreferenceChange(Preference preference) {
        notifyDataSetChanged();
    }

    public void onPreferenceHierarchyChange(Preference preference) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        if (!mHasReturnedViewTypeCount) {
            mHasReturnedViewTypeCount = true;
        }
        
        final Preference preference = this.getItem(position);
        if (preference.hasSpecifiedLayout()) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        int viewType = Collections.binarySearch(mPreferenceLayouts, mTempPreferenceLayout);
        if (viewType < 0) {
            // This is a class that was seen after we returned the count, so
            // don't recycle it.
            return IGNORE_ITEM_VIEW_TYPE;
        } else {
            return viewType;
        }
    }

    @Override
    public int getViewTypeCount() {
        if (!mHasReturnedViewTypeCount) {
            mHasReturnedViewTypeCount = true;
        }
        
        return Math.max(1, mPreferenceLayouts.size());
    }

}
