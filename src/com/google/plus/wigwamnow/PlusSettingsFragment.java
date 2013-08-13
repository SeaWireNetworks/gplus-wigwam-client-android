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

import com.google.plus.wigwamnow.social.PlusAuthActivity;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Settings {@link ListFragment} for Google+ Sign In.  Allows for sign out and disconnect.
 * 
 * @author samstern
 */
public class PlusSettingsFragment extends ListFragment {

    /** Index for the sign out list item **/
    private static final int SIGN_OUT = 0;
    
    /** Index for the disconnect list item **/
    private static final int DISCONNECT = 1;
    
    /** List of all options in the menu, in order **/
    private String[] mMenuOptions;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMenuOptions = getResources().getStringArray(R.array.plus_settings);
        
        // Display each element of OPTIONS as a list item
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, mMenuOptions);
        setListAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onListItemClick(ListView list, View view, int pos, long id) {
        // Check that the host activity implements signIn, signOut, etc.
        if (!(getActivity() instanceof PlusAuthActivity)) {
            throw new IllegalArgumentException(
                    "Host of this Fragment must implement PlusAuthActivity");
        }  
        PlusAuthActivity host = (PlusAuthActivity) getActivity();
        // Dispatch the correct action
        if (pos == SIGN_OUT) {
            host.signOut();
        } else if (pos == DISCONNECT) {
            host.disconnect();
        }
    }
    
}

