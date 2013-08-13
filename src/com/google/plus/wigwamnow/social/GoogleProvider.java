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

package com.google.plus.wigwamnow.social;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.moments.ItemScope;
import com.google.android.gms.plus.model.moments.Moment;
import com.google.plus.wigwamnow.MainActivity;
import com.google.plus.wigwamnow.R;
import com.google.plus.wigwamnow.WigwamDetailActivity;
import com.google.plus.wigwamnow.WigwamNow;
import com.google.plus.wigwamnow.models.Wigwam;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

/**
 * An implementation of {@link SocialProvider} to handle all logic associated with Google APIs
 * and Google+ social actions.
 * 
 * @author Sam Stern (samstern@google.com)
 */
public class GoogleProvider extends SocialProvider {
   
    public static String TAG = GoogleProvider.class.getSimpleName();
    
    /** Display name **/
    private static final String NAME = "Google+";
   
    /** Request code for identification with {@link PlusClientFragment} **/
    public static int REQUEST_CODE_TOKEN_AUTH = 9001;
   
    /** Boolean to determine if sending hybrid auth is pending **/
    private boolean mPendingCodeSend = false;

    /** Array of SocialFeatures that this SocialProvider implements **/
    public static SocialFeature[] sSupportedFeatures = {
        SocialFeature.SHARE,
        SocialFeature.RENT,
        SocialFeature.HYBRID_AUTH
    };

    @Override
    public boolean supports(SocialFeature feature) {
        return (Arrays.asList(sSupportedFeatures).contains(feature));
    }

    /**
     * Share an interactive post using the native dialog.
     */
    @Override
    public boolean share(Wigwam wigwam, Activity activity) {
        if (!(activity instanceof PlusClientHostActivity)) {
            throw new IllegalArgumentException("Activity must host a PlusClient!");
        }
        
        PlusClientHostActivity hostActivity = (PlusClientHostActivity) activity;
        
        String host = activity.getResources().getString(R.string.external_host);
        Uri wigwamLink = Uri.parse(host + "/wigwams/" + wigwam.getId());
        String title = "Check out " + wigwam.getName() + "!";
        Intent shareIntent = new PlusShare.Builder(activity, hostActivity.getPlusClient())
            .setType("text/plain")
            .setText(title)
            .addCallToAction("RESERVE", wigwamLink,"/wigwams/" + wigwam.getId())
            .setContentDeepLinkId("/wigwams/" + wigwam.getId(),
                    title,
                    wigwam.getDescription(),
                    wigwamLink)
            .setContentUrl(wigwamLink)
            .getIntent();
        activity.startActivityForResult(shareIntent, 0);
        return false;
    }

    /**
     * Create an app activity for renting a {@link Wigwam}.
     */
    @Override
    public boolean rent(Wigwam wigwam, Activity activity) {
        String host = activity.getResources().getString(R.string.external_host);
        // Write an app activity to Google Plus
        ItemScope target = new ItemScope.Builder()
            .setUrl(host + "/wigwams/" + wigwam.getId())
            .build();
        ItemScope result = new ItemScope.Builder()
            .setType("http://schemas.google.com/Reservation")
            .build();
        Moment moment = new Moment.Builder()
            .setType("http://schemas.google.com/ReserveActivity")
            .setTarget(target)
            .setResult(result)
            .build();
        
        PlusClient client = ((WigwamDetailActivity) activity).getPlusClient();
        
        if (client.isConnected()) {
            client.writeMoment(moment);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean structuredShare(Wigwam wigwam, Activity activity) {
        throw new UnsupportedFeatureException(SocialFeature.STRUCTURED_SHARE);
    }

    /**
     * Initiate server-side authorization by sending a one time code to the server.
     */
    @Override
    public void hybridAuth(final Activity activity) {
        // Check that the activity has a PlusClient
        if (!(activity instanceof PlusClientHostActivity)) {
            throw new IllegalArgumentException("Activity must host a PlusClient!");
        }
        
        final PlusClientHostActivity clientHost = (PlusClientHostActivity) activity;
        
        // Create the hybrid authorization resources
        final String clientId = activity.getResources().getString(R.string.plus_client_id);
        final String[] activities = activity.getResources()
                .getStringArray(R.array.visible_activities);
        final String[] scopes = activity.getResources().getStringArray(R.array.plus_scopes);
        final String scopeString = "oauth2:server:client_id:" + clientId + ":api_scope:" +
                TextUtils.join(" ", scopes);

        final Bundle appActivities = new Bundle();
        appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES,
                TextUtils.join(" ", activities));

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            
            final Activity hostActivity = activity;
            
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return GoogleAuthUtil.getToken(
                            hostActivity,
                            clientHost.getPlusClient().getAccountName(),
                            scopeString,
                            appActivities);
                } catch (IOException transientEx) {
                    // Network or server error, try later
                    Log.e(TAG, transientEx.toString(), transientEx);
                    return null;
                } catch (UserRecoverableAuthException e) {
                    // Recover (with e.getIntent())
                    Log.e(TAG, e.toString(), e);
                    Intent recover = e.getIntent();
                    hostActivity.startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                } catch (GoogleAuthException authEx) {
                    // The call is not ever expected to succeed and should not be retried.
                    Log.e(TAG, authEx.toString(), authEx);
                    return null;
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e);
                    throw new RuntimeException(e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(String code) {
                Log.d(TAG, "Authorization code retrieved:" + code);
                if (code != null && !mPendingCodeSend) {
                    mPendingCodeSend = true;
                    initGoogleHybridFlow(code, hostActivity);
                }
            }

        };
        task.execute();

    }
    
    /**
     * Initiate the hybrid authorization flow with Google+ by sending the one time code to the
     * server.  See {@link "https://developers.google.com/+/web/signin/server-side-flow"}
     * 
     * @param code the one time authorization flow, retrieved from {@link GoogleAuthUtil#getToken}
     */
    private void initGoogleHybridFlow(String code, final Activity activity) {
        String host = activity.getResources().getString(R.string.external_host);
        final String endpoint = host + "/auth/gplus/hybrid.json";
        // Send the one time code and the Android redirect uri to the server, so the server can get
        // an access_token
        JSONObject params = new JSONObject();
        try {
            params.put("code", code);
            params.put("redirect_uri", activity.getResources().getString(R.string.redirect_uri));
        } catch (JSONException e1) {
            Log.e(TAG, "JSON Exception", e1);
            return;
        }
        
        JsonObjectRequest jor = new JsonObjectRequest(
                Request.Method.POST, 
                endpoint,
                params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject json) {
                        mPendingCodeSend = false;
                        // TODO(samstern): Refactor into listener
                        ((MainActivity) activity).recordCodeSent(SocialProviderConstants.GOOGLE, 1);
                        Log.i(TAG, json.toString());
                    }     
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        mPendingCodeSend = false;
                        Log.e(TAG, "Code seding error", e);
                    }
                });
        WigwamNow.getQueue().add(jor);
    }

    @Override
    public boolean postPhoto(Uri photoUri, Activity activity) {
        throw new UnsupportedFeatureException(SocialFeature.POST_PHOTO);
    }
    
    @Override
    public String getName() {
        return NAME;
    }

}

