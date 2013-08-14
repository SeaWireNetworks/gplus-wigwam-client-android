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

import com.google.plus.wigwamnow.network.BitmapCache;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Application class for the WigwamNow app. Hosts the volley request queue and other global state.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class WigwamNow extends Application {

    /** Volley request queue for all network requests initated by the application **/
    private static RequestQueue sReqQueue;
    
    /** Image loader that caches images to disk **/
    private static ImageLoader sImageLoader;
    
    /** The maximum number of bitmaps to keep in the {@link BitmapCache} at any time **/
    private static final int IMAGE_CACHE_SIZE = 40;

    @Override
    public void onCreate() {
        super.onCreate();
        sReqQueue = Volley.newRequestQueue(this);
        sImageLoader = new ImageLoader(sReqQueue, new BitmapCache(IMAGE_CACHE_SIZE));
    }

    public static RequestQueue getQueue() {
        return sReqQueue;
    }

    public static ImageLoader getImageLoader() {
        return sImageLoader;
    }

}

