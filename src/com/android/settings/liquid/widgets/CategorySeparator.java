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

package com.android.settings.liquid.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.liquid.BaseSetting;

public class CategorySeparator extends LinearLayout {

    public CategorySeparator(Context context) {
        this(context, null);
    }

    public CategorySeparator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategorySeparator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        View.inflate(context, R.layout.category_separator, this);

        if (attrs != null) {
            int title = attrs.getAttributeResourceValue(BaseSetting.NAMESPACE_ANDROID, "title", 0);
            if (title > 0) {
                TextView textView = (TextView) findViewById(R.id.title);
                textView.setText(title);
                textView.setVisibility(View.VISIBLE);
            }
        }
    }

}
