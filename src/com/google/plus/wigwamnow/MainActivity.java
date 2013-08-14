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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusClient.OnAccessRevokedListener;
import com.google.plus.wigwamnow.models.Wigwam;
import com.google.plus.wigwamnow.social.FacebookProvider;
import com.google.plus.wigwamnow.social.GoogleProvider;
import com.google.plus.wigwamnow.social.PlusAuthActivity;
import com.google.plus.wigwamnow.social.PlusClientFragment;
import com.google.plus.wigwamnow.social.PlusClientFragment.OnSignInListener;
import com.google.plus.wigwamnow.social.PlusClientHostActivity;
import com.google.plus.wigwamnow.social.SocialProviderConstants;
import com.google.plus.wigwamnow.social.SocialProvider;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.UserSettingsFragment;

/**
 * MainActivity class for the WigwamNow app. Hosts fragments for the login screen, settings screen,
 * and main wigwam selection screen.
 *
 * @author samstern
 */
public class MainActivity extends FragmentActivity 
    implements OnSignInListener, PlusClientHostActivity, PlusAuthActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    
    /** Fragment index for the {@link SplashFragment} **/
    private static final int SPLASH = 0;
    
    /** Fragment index for the {@link SelectionFragment} **/
    private static final int SELECTION = 1;
    
    /** Fragment index for the Facebook {@link UserSettingsFragment} **/
    private static final int FB_SETTINGS = 2;
    
    /** Fragment index for the {@link PlusSettingsFragment} **/
    private static final int GPLUS_SETTINGS = 3;
    
    /** Count of the total number of {@link Fragment}s hosted **/
    private static final int FRAGMENT_COUNT = GPLUS_SETTINGS + 1;
    
    /** Request code for request identification the {@link PlusClientFragment} **/
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    
    /** Key prefix for storing hybrid auth status in {@link SharedPreferences} **/
    private static final String KEY_CODE_SENT = "CODE_SENT";
    
    /** Key for the last {@link SocialProviderConstants} stored in the {@link SharedPreferences} **/
    private static final String KEY_LAST_PROVIDER = "LAST_PROVIDER";
    
    /** Key for passing a {@link Wigwam} in a {@link Bundle} **/
    protected static final String EXTRA_WIGWAM = "wigwam";
    
    /** Key for passing a {@link SocialProviderConstants} in a {@link Bundle} **/
    protected static final String EXTRA_PROVIDER = "provider";
    
    /** Boolean determining if the {@link Activity} was resumed through {@link #onResume} **/
    private boolean mIsResumed = false;
    
    /** Array of {@link Fragment}s hosted by this activity **/
    private Fragment[] mFragments = new Fragment[FRAGMENT_COUNT];
    
    /** Helper to manage lifecycle of Facebook (@link Session} **/
    private UiLifecycleHelper mUiHelper;
    
    /** Invisible Fragment to manage {@link PlusClient} state **/
    private PlusClientFragment mPlusFragment;
    
    /** The last {@link SocialProviderConstants} stored in {@link SharedPreferences} **/
    private int mLastProvider;

    /**
     * Handler for change in Facebook Session Status
     */
    private Session.StatusCallback mFacebookStatusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
    /** id of the Settings option in the options menu, when applicable **/
    private int mSettingsId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Track Facebook Session Status
        mUiHelper = new UiLifecycleHelper(this, mFacebookStatusCallback);
        mUiHelper.onCreate(savedInstanceState);
        
        // Link to PlusClientFragment
        mPlusFragment = PlusClientFragment.getPlusClientFragment(
                this,
                getResources().getStringArray(R.array.plus_scopes),
                getResources().getStringArray(R.array.visible_activities));
        
        // Reset the last known SocialProviderConstants
        mLastProvider = SocialProviderConstants.NONE;
        
        // Instantiate the four main fragments
        FragmentManager fm = getSupportFragmentManager();
        mFragments[SPLASH] = fm.findFragmentById(R.id.splashFragment);
        mFragments[SELECTION] = fm.findFragmentById(R.id.selectionFragment);
        mFragments[FB_SETTINGS] = fm.findFragmentById(R.id.fbSettingsFragment);
        mFragments[GPLUS_SETTINGS] = fm.findFragmentById(R.id.gplusSettingsFragment);
        
        // Hide all of the fragments, to start
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : mFragments) {
            transaction.hide(fragment);
        }
        transaction.commit();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the user is signed in with any SocialProvider, display the Settings option in the
        // options menu, otherwise clear the menu.
        switch (currentProvider()) {
            case SocialProviderConstants.NONE:
                return false;
            default:
                if (menu.size() == 0) {
                    mSettingsId = menu.add(R.string.settings).getItemId();
                }
                return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // When the Settings option is selected, show the settings Fragment for the SocialProvider.
        if (item.getItemId() == mSettingsId) {
            if (currentProvider() == SocialProviderConstants.FACEBOOK) {
                showFragment(FB_SETTINGS, true);
            } else if (currentProvider() == SocialProviderConstants.GOOGLE) {
                showFragment(GPLUS_SETTINGS, true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mUiHelper.onResume();
        mIsResumed = true;
        // Restore the last known SocialProviderConstants
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        mLastProvider = sharedPreferences.getInt(KEY_LAST_PROVIDER, SocialProviderConstants.NONE);
        // Parse deep link
        SelectionFragment selection = (SelectionFragment) mFragments[SELECTION];
        selection.parseDeepLink();
    }

    @Override
    public void onPause() {
        super.onPause();
        mUiHelper.onPause();
        mIsResumed = false;
        // Save the provider
        saveProvider(currentProvider());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mUiHelper.onActivityResult(requestCode, resultCode, data);
        // Delegate to the Google Plus Fragment
        mPlusFragment.handleOnActivityResult(requestCode, resultCode, data);
        // Initiate Google hybrid authorization flow when successfully signed in
        if (requestCode == GoogleProvider.REQUEST_CODE_TOKEN_AUTH && resultCode == RESULT_OK) {
            sendGoogleAuthToServer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUiHelper.onSaveInstanceState(outState);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (currentProvider() != SocialProviderConstants.NONE) {
            // User is logged in
            showFragment(SELECTION, true);
        } else if (mLastProvider != SocialProviderConstants.NONE ){
            // User is navigating back to this, was logged in when he/she left
            showFragment(SELECTION, true);
        } else {
            // User is not logged in, show the splash screen and request login
            showFragment(SPLASH, false);
        }
    }
    
    /**
     * Save the last social login {@link SocialProviderConstants} used.
     * 
     * @param provider the provider (see {@link SocialProviderConstants}) which the user chose.
     */
    private void saveProvider(int provider) {
        saveInt(KEY_LAST_PROVIDER, provider);
    }
    
    /**
     * Save an integer to the {@link SharedPreferences}
     * 
     * @param key the String key that will be used to retrieve the integer.
     * @param val the integer value to save.
     */
    private void saveInt(String key, int val) {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putInt(key, val);
        editor.commit();
    }
    
    /**
     * Show the {@link SplashFragment}, prompting the user to log in again.
     */
    protected void showLoginFragment() {
        showFragment(SPLASH, false);
    }

    /**
     * Show a given fragment and hide all others.
     *
     * @param fragmentIndex index of the fragment to show such as {@code SPLASH}.
     * @param addToBackStack boolean to control adding to back stack.
     */
    private void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (fragmentIndex >= mFragments.length || fragmentIndex < 0) {
            // Invalid fragment index, take no action
            Log.w(TAG, "Tried to display invalid fragment " + Integer.toString(fragmentIndex));
            return;
        }
        // Hide all other fragments
        for (Fragment frag : mFragments) {
            if (frag == mFragments[fragmentIndex]) {
                transaction.show(frag);
            } else {
                transaction.hide(frag);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        // Update the options menu in the Action Bar (if on JellyBean or above)
        supportInvalidateOptionsMenu();
    }

    /**
     * Callback when the state of the Facebook Session changes.
     *
     * @param session the session which changed.
     * @param state the current state of the session.
     * @param exception any exception which occurred.
     */
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Only make changes if the activity is visible
        if (mIsResumed) {
            FragmentManager manager = getSupportFragmentManager();
            // Get the number of entries in the back stack
            int backStackSize = manager.getBackStackEntryCount();
            // Clear the back stack
            for (int i = 0; i < backStackSize; i++) {
                manager.popBackStack();
            }
            if (state.isOpened()) {
                // Send the authorization information to the web server
                sendFbTokenToServer();
                // Show the authenticated fragment
                showFragment(SELECTION, false);
            } else if (state.isClosed()) {
                // Show the login fragment
                saveProvider(SocialProviderConstants.NONE);
                showFragment(SPLASH, false);
            }
        }
    }
    
    /**
     * Get the current social identity {@link SocialProviderConstants}.
     * 
     * @return the int from {@link SocialProviderConstants} that the user used to log in.
     * <code>SocialProviderConstants.NONE</code> if the user is not currently authenticated.
     */
    public int currentProvider() {
        // TODO(samstern): Refactor to return Social SocialProviderConstants
        if (mPlusFragment.getClient().isConnected()) {
            return SocialProviderConstants.GOOGLE;    
        } else if (Session.getActiveSession() != null && Session.getActiveSession().isOpened()) {
            return SocialProviderConstants.FACEBOOK;
        } else {
            return SocialProviderConstants.NONE;
        }
    }
   
    @Override
    public void signIn() {
        mPlusFragment.signIn(REQUEST_CODE_RESOLVE_ERR);
    }
    
    @Override
    public void signOut() {
        mPlusFragment.signOut();
        showFragment(SPLASH, false);
    }
    
    @Override
    public void disconnect() {
        mPlusFragment.disconnect(new OnAccessRevokedListener() {
            @Override
            public void onAccessRevoked(ConnectionResult status) {
                // Additional disconnect logic here
            } 
        });
        recordCodeSent(SocialProviderConstants.GOOGLE, 0);
        showFragment(SPLASH, false); 
    }
    
    @Override
    public PlusClient getPlusClient() {
        return mPlusFragment.getClient();
    }
    
    /**
     * Retrieve authorization information from the {@link SocialProviderConstants} and send it 
     * to the server.
     */
    protected void sendGoogleAuthToServer() {
        // TODO(samstern): Call this method BEFORE the first connect, not after.
        // TODO(samstern): Refactor into single hybrid auth method
        if (!authSentToServer(SocialProviderConstants.GOOGLE)) {
            SocialProvider google = new GoogleProvider();
            google.hybridAuth(this);
        }
    }
    
    
    /**
     * Send the access_token from the current Facebook {@link Session} to the server.
     */
    private void sendFbTokenToServer() {
        SocialProvider facebook = new FacebookProvider();
        facebook.hybridAuth(this);
    }

    /**
     * Called when a {@link Wigwam} is selected in {@link SelectionFragment}
     *
     * @param wigwam the {@link Wigwam} which was selected from the list.
     */
    protected void wigwamSelected(Wigwam wigwam) {
        newIntent(wigwam);
    }
    
    /**
     * Create an intent for the {@link WigwamDetailActivity} for a {@link Wigwam}.
     * 
     * @param wigwam the selected {@link Wigwam}
     */
    private void newIntent(Wigwam wigwam) {
        Intent intent = new Intent(this, WigwamDetailActivity.class);
        intent.putExtra(EXTRA_WIGWAM, (Parcelable) wigwam);
        intent.putExtra(EXTRA_PROVIDER, currentProvider());

        startActivity(intent);
    }
    
    /**
     * Check {@link SharedPreferences} to see if authorization has already been passed to the server
     * for the current user.
     * 
     * @param provider the social provider {@link SocialProviderConstants} for the current user.
     * @return true if authorization was previously sent, false otherwise.
     */
    private boolean authSentToServer(int provider) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        int codeSent = sharedPreferences.getInt(KEY_CODE_SENT + Integer.toString(provider), 0);

        return codeSent == 1;
    }
    
    /**
     * Record whether the authorization information has already been sent to the server or if
     * it should be sent again on next log in.
     * 
     * @param provider the {@link SocialProviderConstants} for which the code was/wasn't sent.
     * @param status 1 if the code was sent, 0 if it needs to be set next time.
     */
    public void recordCodeSent(int provider, int status) {
        saveInt(KEY_CODE_SENT + Integer.toString(provider), status);
    }

    @Override
    public void onSignedIn(PlusClient plusClient) {
        // Load profile information
        plusClient.loadPerson((SelectionFragment) mFragments[SELECTION], "me");
        if (!authSentToServer(SocialProviderConstants.GOOGLE)) {
            // Get and send the code to the server, but only if the user hasn't done this
            // since the last disconnect
            Log.d(TAG, "SENDING AUTHORIZATION");
            sendGoogleAuthToServer();
        }
        // Show the selection fragment
        showFragment(SELECTION, true);
    }

    @Override
    public void onSignInFailed() {
        saveProvider(SocialProviderConstants.NONE);
    }
}

