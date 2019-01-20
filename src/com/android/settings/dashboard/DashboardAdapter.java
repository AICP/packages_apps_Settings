/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.R.id;
import com.android.settings.dashboard.DashboardData.ConditionHeaderData;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;

import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder>
        implements SummaryLoader.SummaryConsumer, SuggestionAdapter.Callback, LifecycleObserver,
        OnSaveInstanceState {
    public static final String TAG = "DashboardAdapter";
    private static final String STATE_CATEGORY_LIST = "category_list";

    @VisibleForTesting
    static final String STATE_CONDITION_EXPANDED = "condition_expanded";

    private final IconCache mCache;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mFirstFrameDrawn;
    private RecyclerView mRecyclerView;
    private SuggestionAdapter mSuggestionAdapter;

    private int mIconStyle;
    private int mNormalColor;
    private int mAccentColor;

    @VisibleForTesting
    DashboardData mDashboardData;

    private View.OnClickListener mTileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: get rid of setTag/getTag
            mDashboardFeatureProvider.openTileIntent((Activity) mContext, (Tile) v.getTag());
        }
    };

    public DashboardAdapter(Context context, Bundle savedInstanceState,
            List<Condition> conditions, SuggestionControllerMixin suggestionControllerMixin,
            Lifecycle lifecycle) {

        DashboardCategory category = null;
        boolean conditionExpanded = false;

        mContext = context;
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mDashboardFeatureProvider = factory.getDashboardFeatureProvider(context);
        mCache = new IconCache(context);
        mSuggestionAdapter = new SuggestionAdapter(mContext, suggestionControllerMixin,
                savedInstanceState, this /* callback */, lifecycle);

        setHasStableIds(true);

        if (savedInstanceState != null) {
            category = savedInstanceState.getParcelable(STATE_CATEGORY_LIST);
            conditionExpanded = savedInstanceState.getBoolean(
                    STATE_CONDITION_EXPANDED, conditionExpanded);
        }

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mDashboardData = new DashboardData.Builder()
                .setConditions(conditions)
                .setSuggestions(mSuggestionAdapter.getSuggestions())
                .setCategory(category)
                .setConditionExpanded(conditionExpanded)
                .build();

        int[] attrs = new int[] {
            android.R.attr.colorControlNormal,
            android.R.attr.colorAccent,
        };
        TypedArray ta = mContext.getTheme().obtainStyledAttributes(attrs);
        mNormalColor = ta.getColor(0, 0xff808080);
        mAccentColor = ta.getColor(1, 0xff808080);
        ta.recycle();

        mIconStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.THEMING_SETTINGS_DASHBOARD_ICONS, 0);
    }

    public void setSuggestions(List<Suggestion> data) {
        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(data)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setCategory(DashboardCategory category) {
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setCategory called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setCategory(category)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setConditions(List<Condition> conditions) {
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setConditions called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setConditions(conditions)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    @Override
    public void onSuggestionClosed(Suggestion suggestion) {
        final List<Suggestion> list = mDashboardData.getSuggestions();
        if (list == null || list.size() == 0) {
            return;
        }
        if (list.size() == 1) {
            // The only suggestion is dismissed, and the the empty suggestion container will
            // remain as the dashboard item. Need to refresh the dashboard list.
            setSuggestions(null);
        } else {
            list.remove(suggestion);
            setSuggestions(list);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final int position = mDashboardData.getPositionByTile(tile);
        if (position != DashboardData.POSITION_NOT_FOUND) {
            // Since usually tile in parameter and tile in mCategories are same instance,
            // which is hard to be detected by DiffUtil, so we notifyItemChanged directly.
            notifyItemChanged(position, mDashboardData.getItemTypeByPosition(position));
        }
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.condition_header) {
            return new ConditionHeaderHolder(view);
        }
        if (viewType == R.layout.condition_container) {
            return new ConditionContainerHolder(view);
        }
        if (viewType == R.layout.suggestion_container) {
            return new SuggestionContainerHolder(view);
        }
        return new DashboardItemHolder(view);
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final int type = mDashboardData.getItemTypeByPosition(position);
        switch (type) {
            case R.layout.dashboard_tile:
                final Tile tile = (Tile) mDashboardData.getItemEntityByPosition(position);
                onBindTile(holder, tile);
                holder.itemView.setTag(tile);
                holder.itemView.setOnClickListener(mTileClickListener);
                break;
            case R.layout.suggestion_container:
                onBindSuggestion((SuggestionContainerHolder) holder, position);
                break;
            case R.layout.condition_container:
                onBindCondition((ConditionContainerHolder) holder, position);
                break;
            case R.layout.condition_header:
                onBindConditionHeader((ConditionHeaderHolder) holder,
                        (ConditionHeaderData) mDashboardData.getItemEntityByPosition(position));
                break;
            case R.layout.condition_footer:
                holder.itemView.setOnClickListener(v -> {
                    mMetricsFeatureProvider.action(mContext,
                            MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, false);
                    DashboardData prevData = mDashboardData;
                    mDashboardData = new DashboardData.Builder(prevData).
                            setConditionExpanded(false).build();
                    notifyDashboardDataChanged(prevData);
                    scrollToTopOfConditions();
                });
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return mDashboardData.getItemIdByPosition(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mDashboardData.getItemTypeByPosition(position);
    }

    @Override
    public int getItemCount() {
        return mDashboardData.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // save the view so that we can scroll it when expanding/collapsing the suggestion and
        // conditions.
        mRecyclerView = recyclerView;
    }

    public Object getItem(long itemId) {
        return mDashboardData.getItemEntityById(itemId);
    }

    public Suggestion getSuggestion(int position) {
        return mSuggestionAdapter.getSuggestion(position);
    }

    @VisibleForTesting
    void notifyDashboardDataChanged(DashboardData prevData) {
        if (mFirstFrameDrawn && prevData != null) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DashboardData
                    .ItemsDataDiffCallback(prevData.getItemList(), mDashboardData.getItemList()));
            diffResult.dispatchUpdatesTo(this);
        } else {
            mFirstFrameDrawn = true;
            notifyDataSetChanged();
        }
    }

    @VisibleForTesting
    void onBindConditionHeader(final ConditionHeaderHolder holder, ConditionHeaderData data) {
        holder.icon.setImageDrawable(data.conditionIcons.get(0));
        if (data.conditionCount == 1) {
            holder.title.setText(data.title);
            holder.summary.setText(null);
            holder.icons.setVisibility(View.INVISIBLE);
        } else {
            holder.title.setText(null);
            holder.summary.setText(
                    mContext.getString(R.string.condition_summary, data.conditionCount));
            updateConditionIcons(data.conditionIcons, holder.icons);
            holder.icons.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            mMetricsFeatureProvider.action(mContext,
                    MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, true);
            final DashboardData prevData = mDashboardData;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setConditionExpanded(true).build();
            notifyDashboardDataChanged(prevData);
            scrollToTopOfConditions();
        });
    }

    @VisibleForTesting
    void onBindCondition(final ConditionContainerHolder holder, int position) {
        final ConditionAdapter adapter = new ConditionAdapter(mContext,
                (List<Condition>) mDashboardData.getItemEntityByPosition(position),
                mDashboardData.isConditionExpanded());
        adapter.addDismissHandling(holder.data);
        holder.data.setAdapter(adapter);
        holder.data.setLayoutManager(new LinearLayoutManager(mContext));
    }

    @VisibleForTesting
    void onBindSuggestion(final SuggestionContainerHolder holder, int position) {
        // If there is suggestions to show, it will be at position 0 as we don't show the suggestion
        // header anymore.
        final List<Suggestion> suggestions =
                (List<Suggestion>) mDashboardData.getItemEntityByPosition(position);
        if (suggestions != null && suggestions.size() > 0) {
            mSuggestionAdapter.setSuggestions(suggestions);
            holder.data.setAdapter(mSuggestionAdapter);
        }
        final LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        holder.data.setLayoutManager(layoutManager);
    }

    @VisibleForTesting
    void onBindTile(DashboardItemHolder holder, Tile tile) {
        Drawable icon = mCache.getIcon(tile.icon);

        // Clear tint from previous calls
        icon.setTintList(null);

        if (mIconStyle != 0) {
            // Remove round background from icons
            if (icon instanceof LayerDrawable) {
                LayerDrawable lIcon = (LayerDrawable) icon;
                if (lIcon.getNumberOfLayers() == 2) {
                    icon = lIcon.getDrawable(1);
                } else {
                    Log.w(TAG, "Layer drawable has " + lIcon.getNumberOfLayers() + " layers");
                }
            }
            if (mIconStyle == 1) {
                icon = new RoundedHomepageIcon(mContext, icon);
                ((RoundedHomepageIcon) icon).setBackgroundColor(mAccentColor);
            } else if (mIconStyle == 2) {
                icon = new RoundedHomepageIcon(mContext, icon);
                ((RoundedHomepageIcon) icon).setBackgroundColor(mContext.getColor(R.color.settings_icon_oneplus));
                icon.setTint(mAccentColor);
            } else if (mIconStyle == 3) {
                icon.setTint(mNormalColor);
            } else if (mIconStyle == 4) {
                icon.setTint(mAccentColor);
            }
            mCache.updateIcon(tile.icon, icon);
        } else {
            if (!TextUtils.equals(tile.icon.getResPackage(), mContext.getPackageName())
                    && !(icon instanceof RoundedHomepageIcon)) {
                icon = new RoundedHomepageIcon(mContext, icon);
                try {
                    if (tile.metaData != null) {
                        final int colorRes = tile.metaData.getInt(
                                TileUtils.META_DATA_PREFERENCE_ICON_BACKGROUND_HINT, 0 /* default */);
                        if (colorRes != 0) {
                            final int bgColor = mContext.getPackageManager()
                                    .getResourcesForApplication(tile.icon.getResPackage())
                                    .getColor(colorRes, null /* theme */);
                            ((RoundedHomepageIcon) icon).setBackgroundColor(bgColor);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed to set background color for " + tile.intent.getPackage());
                }
                mCache.updateIcon(tile.icon, icon);
            }
        }
        holder.icon.setImageDrawable(icon);
        holder.title.setText(tile.title);
        if (!TextUtils.isEmpty(tile.summary)) {
            holder.summary.setText(tile.summary);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.summary.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final DashboardCategory category = mDashboardData.getCategory();
        if (category != null) {
            outState.putParcelable(STATE_CATEGORY_LIST, category);
        }
        outState.putBoolean(STATE_CONDITION_EXPANDED, mDashboardData.isConditionExpanded());
    }

    private void updateConditionIcons(List<Drawable> icons, ViewGroup parent) {
        if (icons == null || icons.size() < 2) {
            parent.setVisibility(View.INVISIBLE);
            return;
        }
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        parent.removeAllViews();
        for (int i = 1, size = icons.size(); i < size; i++) {
            ImageView icon = (ImageView) inflater.inflate(
                    R.layout.condition_header_icon, parent, false);
            icon.setImageDrawable(icons.get(i));
            parent.addView(icon);
        }
        parent.setVisibility(View.VISIBLE);
    }

    private void scrollToTopOfConditions() {
        mRecyclerView.scrollToPosition(mDashboardData.hasSuggestion() ? 1 : 0);
    }

    public boolean shouldRecreate() {
        int iconStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.THEMING_SETTINGS_DASHBOARD_ICONS, 0);
        return mIconStyle != iconStyle;
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
            summary = itemView.findViewById(android.R.id.summary);
        }
    }

    public static class ConditionHeaderHolder extends DashboardItemHolder {
        public final LinearLayout icons;
        public final ImageView expandIndicator;

        public ConditionHeaderHolder(View itemView) {
            super(itemView);
            icons = itemView.findViewById(id.additional_icons);
            expandIndicator = itemView.findViewById(id.expand_indicator);
        }
    }

    public static class ConditionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public ConditionContainerHolder(View itemView) {
            super(itemView);
            data = itemView.findViewById(id.data);
        }
    }

    public static class SuggestionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public SuggestionContainerHolder(View itemView) {
            super(itemView);
            data = itemView.findViewById(id.suggestion_list);
        }
    }

}
