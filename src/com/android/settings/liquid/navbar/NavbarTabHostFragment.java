/*
 * Copyright (C) 2015 The Fusion Project
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

package com.android.settings.liquid.navbar;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;
import com.android.settings.R;

public class NavbarTabHostFragment extends Fragment implements OnTabChangeListener {

    private FragmentTabHost mTabHost;

    static String sLastTab;

    public NavbarTabHostFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.container_material);

        mTabHost.addTab(mTabHost.newTabSpec("rearrange").setIndicator(getString(R.string.navbar_tab_arrange)),
                ArrangeNavbarFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("settings").setIndicator(getString(R.string.navbar_tab_settings)),
                NavbarSettingsFragment.class, null);

        mTabHost.setOnTabChangedListener(this);
        return mTabHost;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sLastTab != null) {
            mTabHost.setCurrentTabByTag(sLastTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }

    @Override
    public void onTabChanged(String s) {
        sLastTab = s;
    }
}
