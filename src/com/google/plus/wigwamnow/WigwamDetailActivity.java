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

import com.google.android.gms.plus.PlusClient;
import com.google.plus.wigwamnow.models.Listing;
import com.google.plus.wigwamnow.models.Wigwam;
import com.google.plus.wigwamnow.social.PlusClientFragment;
import com.google.plus.wigwamnow.social.PlusClientFragment.OnSignInListener;
import com.google.plus.wigwamnow.social.PlusClientHostActivity;
import com.google.plus.wigwamnow.social.SocialProviderConstants;
import com.google.plus.wigwamnow.social.SocialProvider;
import com.google.plus.wigwamnow.social.SocialProvider.SocialFeature;
import com.google.plus.wigwamnow.views.WigwamView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Activity dedicated to a single {@link Wigwam}, launched by selection of a wigwam elsewhere.
 * Shows all {@link Wigwam} info as well as possible actions such as rental and sharing.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class WigwamDetailActivity extends SherlockFragmentActivity 
        implements OnClickListener, OnSignInListener, PlusClientHostActivity {
    
    private static final String TAG = WigwamDetailActivity.class.getSimpleName();

    /** Request code for getting additional Facebook permissions **/
    private static final int REAUTH_ACTIVITY_CODE = 200;
    
    /** Request code for taking a picture with the device camera **/
    private static final int TAKE_PICTURE = 300;
    
    /** Name for temporary photo file when stored **/
    private static final String PHOTO_FILENAME = "temp.jpg";
    
    /** Name for folder where photo files are stored **/
    private static final String PHOTO_FOLDER = "/wigwamnow";

    /** Date format for listing {@link Wigwam} availability **/
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat mListingFormat = new SimpleDateFormat("EEE, MMM d ''yy");

    /** View displaying {@link #mWigwam} **/
    private WigwamView mWigwamView;
    
    /** Text view containing the description of {@link #mWigwam} **/
    private TextView mDescription;
    
    /** Text view containing the price of {@link #mWigwam} **/
    private TextView mPrice;
    
    /** Button to initiate rental of {@link #mWigwam} **/
    private Button mRentButton;
    
    /** Button to initiate a structured share of {@link #mWigwam} **/
    private Button mStructuredShareButton;
    
    /** Button to initiate a 'feed' share of {@link #mWigwam} **/
    private Button mShareButton;
    
    /** Button to initiate posting a photo of {@l"temp.jpg"ink #mWigwam} **/
    private Button mPhotoButton;
    
    /** Progress spinner to display while availability data loads **/
    private ProgressBar mAvailabilitySpinner;
    
    /** Text view to display all of {@link #mWigwam}'s {@link Listing}s **/
    private TextView mListingsView;

    /** The {@link Wigwam} to display in detail **/
    private Wigwam mWigwam;
    
    /** The user's {@link SocialProviderConstants} **/
    private int mProvider;

    /** Fragment to manage a {@link PlusClient} **/
    private PlusClientFragment mPlusFragment;
    
    /** Helper to manage the lifecycle of a Facebook {@link Session} **/
    private UiLifecycleHelper mUiHelper;
    
    /** Callback when the state of the Facebook {@link Session} changes **/
    private Session.StatusCallback mFacebookStatusCallback = new Session.StatusCallback() {
        @Override
        public void call(
                final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
    /** Boolean to determine if the activity is waiting to structured share **/
    private boolean mPendingStructuredShare = false;
    
    /** Boolean to determine if the activity is waiting share **/
    private boolean mPendingShare = false;
    
    /** Boolean to determine if the activity is waiting to post a photo **/
    private boolean mPendingImage = false;
    
    /** A {@link Uri} pointing to an image {@link File} that should be posted **/
    private Uri mPendingImageUri = null;
    
    /** Boolean to determine if the activity is waiting to post a rental **/
    private boolean mPendingRent = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wigwam_detail_view);
        mUiHelper = new UiLifecycleHelper(this, mFacebookStatusCallback);
        mUiHelper.onCreate(savedInstanceState);
        
        mPlusFragment = PlusClientFragment.getPlusClientFragment(
                this,
                getResources().getStringArray(R.array.plus_scopes),
                getResources().getStringArray(R.array.visible_activities));
        
        // Initialize basic layout items
        mWigwamView = (WigwamView) findViewById(R.id.detail_wigwam_view);
        mDescription = (TextView) findViewById(R.id.detail_wigwam_description);
        mPrice = (TextView) findViewById(R.id.detail_wigwam_price);
        mListingsView = (TextView) findViewById(R.id.detail_listings);
        
        // Initialize buttons and their click listeners
        mRentButton = (Button) findViewById(R.id.rent_wigwam_button);
        mRentButton.setOnClickListener(this);
        mStructuredShareButton = (Button) findViewById(R.id.structured_share_wigwam_button);
        mStructuredShareButton.setOnClickListener(this);
        mShareButton = (Button) findViewById(R.id.share_wigwam_button);
        mShareButton.setOnClickListener(this);
        mPhotoButton = (Button) findViewById(R.id.photo_wigwam_button);
        mPhotoButton.setOnClickListener(this);
        mAvailabilitySpinner = (ProgressBar) findViewById(R.id.availability_spinner);
        
        // Populate the Wigwam data
        Intent intent = getIntent();
        Wigwam wigwam = (Wigwam) intent.getParcelableExtra(MainActivity.EXTRA_WIGWAM);
        mWigwam = wigwam;
        mWigwamView.fillWithWigwam(wigwam);
        mDescription.setText(wigwam.getDescription());
        mPrice.setText("$" + wigwam.getPrice() + "/night");
        
        // Customize social action buttons based on SocialProviderConstants
        mProvider = intent.getIntExtra(MainActivity.EXTRA_PROVIDER, 
                SocialProviderConstants.NONE);
        configureButton(mStructuredShareButton, SocialFeature.STRUCTURED_SHARE, "Share on");
        configureButton(mShareButton, SocialFeature.SHARE, "Post to");
        configureButton(mPhotoButton, SocialFeature.POST_PHOTO, "Post photo on");
        
        // Allow up navigation (via ActionBarSherlock)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Request wigwam availability data
        getAvailability();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiHelper.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        mUiHelper.onSaveInstanceState(bundle);
    }

    @Override
    public void onPause() {
        super.onPause();
        mUiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUiHelper.onDestroy();
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rent_wigwam_button:
                rentWigwam();
                break;
            case R.id.structured_share_wigwam_button:
                structuredShareWigwam();
                break;
            case R.id.share_wigwam_button:
                shareWigwam();
                break;
            case R.id.photo_wigwam_button:
                takeWigwamPhoto();
                break;
        }
    }
    
    /**
     * Configure the display of a button based on the current {@link SocialProvider}.  If the
     * current {@link SocialProvider} supports the provided {@link SocialFeature} then the button
     * will be visible and the text will be set to prefix + {@link SocialProvider#getName()}.
     * Otherwise, the button will be made invisible.
     * 
     * @param button the {@link Button} to configure.
     * @param feature the {@link SocialFeature} on which to base the configuration.
     * @param prefix the String to which the {@link SocialProvider}'s name should be added if the
     *  button is made visible.  Should not end in whitespace.
     */
    private void configureButton(Button button, SocialFeature feature, String prefix) {
        SocialProvider provider = SocialProvider.get(mProvider);
        if (provider == null) {
            Log.w(TAG, "NULL PROVIDER");
        }
        if (provider != null && provider.supports(feature)) {
            button.setVisibility(View.VISIBLE);
            button.setText(prefix + " " + provider.getName());
        } else {
            button.setVisibility(View.GONE);
        }
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goToMain();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Navigate back to {@link MainActivity}
     */
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent); 
    }

    /**
     * Get the {@link Listing}s for {@link #mWigwam}
     */
    private void getAvailability() {
        String host = getResources().getString(R.string.external_host);
        String path = host + "/wigwams/" + mWigwam.getId().toString() + "/availability.json";
        // Download Listings
        StringRequest sr =
                new StringRequest(path, new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Convert to Java Objects
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            Listing[] values = mapper.readValue(response, Listing[].class);
                            populateListings(values);
                        } catch (JsonParseException e) {
                            Log.e(TAG, e.toString());
                        } catch (JsonMappingException e) {
                            Log.e(TAG, e.toString());
                        } catch (IOException e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                });
        WigwamNow.getQueue().add(sr);
    }

    /**
     * Populates the availability text once the Listings have been fetched from the server.
     *
     * @param values the array of {@link Listing}s fetched from the server.
     */
    private void populateListings(Listing[] values) {
        mAvailabilitySpinner.setVisibility(View.GONE);
        for (Listing l : values) {
            String listingString = mListingFormat.format(l.getStartDate()) + " - "
                    + mListingFormat.format(l.getEndDate());
            mListingsView.append(listingString + "\n");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            mUiHelper.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode == TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                // Picture was taken and stored, post it to Facebook
                postPictureAtUri(mPendingImageUri);
            }
        }
    }
    
    @Override
    public void onSignedIn(PlusClient plusClient) {
        if (mPendingRent) {
            rentWigwam();
        }
        if (mPendingShare) {
            shareWigwam();
        }
    }

    @Override
    public void onSignInFailed() {
        if (mProvider == SocialProviderConstants.GOOGLE) {
            goToMain();
        }
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (session != null && session.isOpened()) {
            if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
                // Token updated with new permissions
                tokenUpdated();
            } else if (state.equals(SessionState.CLOSED)) {
                // Logged out, redirect to login
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    private void tokenUpdated() {
        if (mPendingStructuredShare) {
            structuredShareWigwam();
        }
        if (mPendingShare) {
            shareWigwam();
        }
        if (mPendingImage) {
            postPictureAtUri(mPendingImageUri);
        }
        if (mPendingRent) {
            rentWigwam();
        }
    }
    
    @Override
    public PlusClient getPlusClient() {
        return mPlusFragment.getClient();
    }
    
    /**
     * Takes a photo with the camera, save it in a {@link File} located at {@link #mPendingImageUri}
     */
    private void takeWigwamPhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        String dir = Environment.getExternalStorageDirectory() + PHOTO_FOLDER;
        File fileDir = new File(dir);
        fileDir.mkdirs();
        
        File file = new File(dir, PHOTO_FILENAME);
        Uri outputFileUri = Uri.fromFile(file);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        
        mPendingImageUri = outputFileUri;
        startActivityForResult(takePictureIntent, TAKE_PICTURE);
    }
    
    /**
     * Posts a picture from the device using {@link SocialProvider#postPhoto}.
     * 
     * @param photoUri absolute {@link Uri} ({@code file://...}) of the photo on the device.
     */
    private void postPictureAtUri(Uri photoUri) {
        mPendingImage = false;
        
        SocialProvider provider = SocialProvider.get(mProvider);
        if (provider.supports(SocialFeature.POST_PHOTO)) {
            boolean result = provider.postPhoto(photoUri, this);
            if (!result) {
                mPendingImage = true;
            }
        } else {
            Toast.makeText(this, "Feature not supported.", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Rent a {@link Wigwam} using {@link SocialProvider#rent}.
     */
    private void rentWigwam() {
        SocialProvider provider = SocialProvider.get(mProvider);
        mPendingRent = false;
        
        if (provider.supports(SocialFeature.RENT)) {
            boolean result = provider.rent(mWigwam, this);
            if (!result) {
                mPendingRent = true;
            }
            String rentSuccess = getResources().getString(R.string.wigwam_rented);
            Toast.makeText(this, rentSuccess, Toast.LENGTH_SHORT).show();
        } else {
            String featureNotSupported = getResources().getString(R.string.feature_not_supported);
            Toast.makeText(this, featureNotSupported, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares a {@link Wigwam} to the {@link SocialProvider} using {@link SocialProvider#share};
     */
    private void shareWigwam() {
        mPendingShare = false;
        
        boolean result = SocialProvider.get(mProvider).share(mWigwam, this);
        if (!result) {
            mPendingShare = true;
        }
    }
    
    /**
     * Shares a {@link Wigwam} via the social graph using {@link SocialProvider#structuredShare}.
     */
    private void structuredShareWigwam() {
        mPendingStructuredShare = false;
        
        boolean result = SocialProvider.get(mProvider).structuredShare(mWigwam, this);
        if (!result) {
            mPendingStructuredShare = true;
        }
    }

}

