/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.plus.wigwamnow;

import com.google.plus.wigwamnow.models.Wigwam;
import com.google.plus.wigwamnow.views.WigwamView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * ArrayAdapter for ListView of {@link Wigwam} objects, each displayed in a {@link WigwamView}.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class WigwamArrayAdapter extends ArrayAdapter<Wigwam> {

    /** Context where this adapter is being used **/
    private final Context mContext;
    
    /** List of {@link Wigwam} object that this adapter is managing **/
    private ArrayList<Wigwam> mValues;

    /**
     * Creates a new WigwamAdapter from a context and array of Wigwams.
     *
     * @param context the context containing the relevant ListView.
     * @param values the array of Wigwams to be displayed in the list.
     */
    public WigwamArrayAdapter(Context context, Wigwam[] values) {
        super(context, R.layout.wigwam_view);
        mContext = context;
        mValues = new ArrayList<Wigwam>(Arrays.asList(values));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View wigwamView = convertView;
        if (wigwamView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            wigwamView = inflater.inflate(R.layout.wigwam_list_item, parent, false);
        }
        WigwamView subView = (WigwamView) wigwamView.findViewById(R.id.item_sub_view);
        subView.fillWithWigwam(mValues.get(position));
        return wigwamView;
    }

    @Override
    public int getCount() {
        return mValues.size();
    }

    @Override
    public Wigwam getItem(int position) {
        return mValues.get(position);
    }

}

