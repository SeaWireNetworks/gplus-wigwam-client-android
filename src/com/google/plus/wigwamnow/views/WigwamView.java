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

package com.google.plus.wigwamnow.views;

import com.google.plus.wigwamnow.R;
import com.google.plus.wigwamnow.WigwamNow;
import com.google.plus.wigwamnow.models.Wigwam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

/**
 * Custom View to display a Wigwam Object. Shows the wigwam picture overlayed with the name of the
 * wigwam and other optional information such as price and description.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class WigwamView extends RelativeLayout {

    private static final String TAG = WigwamView.class.getSimpleName();

    /** Context where the view is displayed **/
    private final Context mContext;
    
    /** Boolean to determine if the wigwam's description should be shown **/
    private boolean mShowDescription;
    
    /** Boolean to determine if the wigwam's price should be shown **/
    private boolean mShowPrice;
    
    /** Base layout for the view **/
    private RelativeLayout mBaseLayout;
    
    /** Text view to display the wigwam's name **/
    private TextView mTitleView;
    
    /** Text view to display the wigwam's description **/
    private TextView mDescriptionView;
    
    /** Text view to display the wigwam's price **/
    private TextView mPriceView;
    
    /**
     * See {@link WigwamView#WigwamView(Context, AttributeSet)}
     */
    public WigwamView(Context context) {
        this(context, null);
    }

    /**
     * Initialize the view, hiding and showing the price and description as necessary.
     * 
     * @param context the {@link Context} in which the view is displayed.
     * @param attrs attributes of the view.
     */
    public WigwamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a =
                context.getTheme().obtainStyledAttributes(attrs, R.styleable.WigwamView, 0, 0);

        try {
            mShowDescription = a.getBoolean(R.styleable.WigwamView_showDescription, true);
            mShowPrice = a.getBoolean(R.styleable.WigwamView_showPrice, true);
        } finally {
            a.recycle();
        }

        mContext = context;

        // Set (width, height)
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        View.inflate(context, R.layout.wigwam_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBaseLayout = (RelativeLayout) findViewById(R.id.base_wigwam_layout);
        mTitleView = (TextView) findViewById(R.id.listing_title);
        mDescriptionView = (TextView) findViewById(R.id.listing_description);
        mPriceView = (TextView) findViewById(R.id.listing_price);
        setVisibility();
    }

    /**
     * Use the {@link ImageLoader} to download the image for the {@link Wigwam}, either over the
     * network or from the cache.
     * 
     * @param src the URL where the image is located.
     */
    private void downloadImage(String src) {
        ImageLoader imageLoader = WigwamNow.getImageLoader();
        ImageContainer imgcont = imageLoader.get(src, new ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // Image error, just stick with the default mountains image here,
                // rather than attempt a retry
                Log.e(TAG, error.toString());
            }

            @Override
            public void onResponse(ImageContainer container, boolean isImmediate) {
                Bitmap bitmap = container.getBitmap();
                if (bitmap != null) {
                    setImage(bitmap);
                }
            }

        });

    }

    /**
     * Sets the background to a Bitmap.
     * 
     * @param bitmap a {@link Bitmap} to set as the background image for the WigwamView.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void setImage(Bitmap bitmap) {
        BitmapDrawable bmd = new BitmapDrawable(mContext.getResources(), bitmap);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // On JellyBean or higher, setBackgroundDrawable is deprecated
            mBaseLayout.setBackground(bmd);
        } else {
            // Support Android 2.2+
            mBaseLayout.setBackgroundDrawable(bmd);
        }
    }

    /**
     * Set the visibility for sub views based on {@link #mShowDescription} and {@link #mShowPrice}
     */
    private void setVisibility() {
        int descriptionVisibility = mShowDescription ? View.VISIBLE : View.GONE;
        mDescriptionView.setVisibility(descriptionVisibility);
        int priceVisibility = mShowPrice ? View.VISIBLE : View.GONE;
        mPriceView.setVisibility(priceVisibility);
    }

    /**
     * Determine if the view is displaying the {@link Wigwam}'s description.
     * 
     * @return true if description showing, false otherwise.
     */
    public boolean isShowDescription() {
        return mShowDescription;
    }

    /**
     * Set the visibility of the {@link Wigwam}'s description.
     * 
     * @param showDescription true if description should be shown, false otherwise.
     */
    public void setShowDescription(boolean showDescription) {
        mShowDescription = showDescription;
        setVisibility();
        invalidate();
        requestLayout();
    }

    /**
     * Determine if the view is displaying the {@link Wigwam}'s price.
     * 
     * @return true if price showing, false otherwise.
     */
    public boolean isShowPrice() {
        return mShowPrice;
    }

    /**
     * Set the visibility of the {@link Wigwam}'s price.
     * 
     * @param showPrice true if price should be shown, false otherwise.
     */
    public void setShowPrice(boolean showPrice) {
        mShowPrice = showPrice;
        setVisibility();
        invalidate();
        requestLayout();
    }

    /**
     * Populates the child views with information from a {@link Wigwam} object.
     * 
     * @param wigwam the {@link Wigwam} with the information for the WigwamView.
     */
    public void fillWithWigwam(Wigwam wigwam) {
        mTitleView.setText(wigwam.getName());
        mDescriptionView.setText(wigwam.getDescription());
        String priceString = "$" + wigwam.getPrice() + "/night";
        mPriceView.setText(priceString);
        if (wigwam.getSrc() != null) {
            downloadImage(wigwam.getSrc());
        }
    }

}

