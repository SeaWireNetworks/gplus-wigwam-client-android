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

import com.google.plus.wigwamnow.R;
import com.google.plus.wigwamnow.WigwamDetailActivity;
import com.google.plus.wigwamnow.WigwamNow;
import com.google.plus.wigwamnow.models.Wigwam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.OpenGraphAction;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * An implementation of {@link SocialProvider} to handle all logic associated with Facebook APIs
 * and Facebook social actions.
 * 
 * @author Sam Stern (samstern@google.com)
 */
public class FacebookProvider extends SocialProvider {

    public static String TAG = FacebookProvider.class.getSimpleName();
    
    /** Request code for getting a new Facebook permission **/
    private static final int REAUTH_ACTIVITY_CODE = 200;
    
    /** Graph API path for renting a {@link Wigwam} **/
    private static final String RENT_ACTION_PATH ="me/wigwamnow:rent";
    
    /** Graph API path for sharing a {@link Wigwam} **/
    private static final String SHARE_ACTION_PATH = "me/wigwamnow:share";
    
    /** Display name **/
    private static final String NAME = "Facebook";
    
    /** Uri for the Facebook mobile website **/
    private static final Uri M_FACEBOOK_URL = Uri.parse("http://m.facebook.com");
    
    /** List of permissions needed for posting to Facebook **/
    private static final List<String> PERMISSIONS = Arrays.asList("publish_stream");

    /** Array of SocialFeatures that this SocialProvider implements **/
    public static SocialFeature[] sSupportedFeatures = {
        SocialFeature.SHARE,
        SocialFeature.STRUCTURED_SHARE,
        SocialFeature.RENT,
        SocialFeature.HYBRID_AUTH,
        SocialFeature.POST_PHOTO
    };
    
    /** Progress dialog display during long-running actions **/
    private ProgressDialog mProgressDialog;

    @Override
    public boolean supports(SocialFeature feature) {
        return (Arrays.asList(sSupportedFeatures).contains(feature));
    }

    /**
     * Share a {@link Wigwam} on the user's timeline using the Feed Dialog.
     */
    @Override
    public boolean share(Wigwam wigwam, Activity activity) {
        final Activity hostActivity = activity;
        
        Session session = Session.getActiveSession();
        if (!hasPublishPermissions()) {
            requestPublishPermissions(session, activity);
            return false;
        }

        final Wigwam toShare = wigwam;
        Bundle postParams = new Bundle();
        postParams.putString("name", wigwam.getName());
        postParams.putString("caption", wigwam.getDescription());
        String host = activity.getResources().getString(R.string.external_host);
        postParams.putString("link", host + "/wigwams/" + Integer.toString(wigwam.getId()));
        postParams.putString("message", "Check out this wigwam!");
        postParams.putString("picture", wigwam.getSrc());

        WebDialog feedDialog = new WebDialog.FeedDialogBuilder(
                hostActivity, session, postParams).setOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, FacebookException error) {
                if (error == null) {
                    final String postId = values.getString("post_id");
                    if (postId != null) {
                        Toast.makeText(hostActivity, "Published to timeline",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).build();
        feedDialog.show();
        
        return true;
    }

    /**
     * Create a rental action for the {@link Wigwam} on the Open Graph.
     */
    @Override
    public boolean rent(Wigwam wigwam, Activity activity) {
        // Write an OpenGraph action to Facebook
        Session session = Session.getActiveSession();

        if (session == null || !session.isOpened()) {
            return false;
        }

        if (!hasPublishPermissions()) {
            // Get user's permission to post OG Actions
            requestPublishPermissions(session, activity);
            return false;
        }

        String postingString = activity.getResources().getString(R.string.posting);
        showProgressDialog(postingString, activity);

        final Wigwam toShare = wigwam;
        final Activity hostActivity = activity;
        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... params) {
                RentAction rentAction = GraphObject.Factory.create(RentAction.class);
                WigwamGraphObject wigwamObject = GraphObject.Factory.create(
                        WigwamGraphObject.class);
                // Set wigwam URL
                String host = hostActivity.getResources().getString(R.string.external_host);
                String wigwamUrl = host + "/wigwams/" + toShare.getId().toString();
                wigwamObject.setUrl(wigwamUrl);
                // Add wigwam
                rentAction.setWigwam(wigwamObject);
                // Post to OpenGraph
                Request request = new Request(
                        Session.getActiveSession(), RENT_ACTION_PATH, null, HttpMethod.POST);
                request.setGraphObject(rentAction);
                return request.executeAndWait();
            }

            @Override
            protected void onPostExecute(Response response) {
                onPostActionResponse(response, hostActivity);
            }

        };
        task.execute();
        
        return true;
    }
    
    /**
     * Create a share action for the {@link Wigwam} on the Open Graph.
     */
    @Override
    public boolean structuredShare(Wigwam wigwam, Activity activity) {
        
        Session session = Session.getActiveSession();
        if (session == null || !session.isOpened()) {
            return false;
        }
        if (!hasPublishPermissions()) {
            // Get user's permission to post OG Actions
            requestPublishPermissions(session, activity);
            return false;
        }
        
        final Activity hostActivity = activity;
        
        String postingString = activity.getResources().getString(R.string.posting);
        showProgressDialog(postingString, activity);

        final Wigwam toShare = wigwam;
        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... params) {
                ShareAction shareAction = GraphObject.Factory.create(ShareAction.class);
                WigwamGraphObject wigwamObject = GraphObject.Factory.create(WigwamGraphObject.class);
                // Set wigwam URL
                String host = hostActivity.getResources().getString(R.string.external_host);
                String wigwamUrl = host + "/wigwams/" + toShare.getId().toString();
                wigwamObject.setUrl(wigwamUrl);
                // Add wigwam
                shareAction.setWigwam(wigwamObject);
                // Post to OpenGraph
                Request request = new Request(
                        Session.getActiveSession(), SHARE_ACTION_PATH, null, HttpMethod.POST);
                request.setGraphObject(shareAction);
                return request.executeAndWait();
            }

            @Override
            protected void onPostExecute(Response response) {
                onPostActionResponse(response, hostActivity);
            }

        };

        task.execute();
        return true;
    }

    /**
     * Determines if the current user has authorized the app with the publish_stream permission.
     * 
     * @return true if the app is authorized to publish on behalf of the user.
     */
    private boolean hasPublishPermissions() {
        Session session = Session.getActiveSession();
        List<String> permissions = session.getPermissions();
        return (permissions.containsAll(PERMISSIONS));
    }

    /** 
     * Send the <code>access_token</code> to the server for authorization.
     */
    @Override
    public void hybridAuth(Activity activity) {
        String host = activity.getResources().getString(R.string.external_host);
        final String endpoint = host + "/auth/facebook/hybrid.json";
        // Send the Facebook access_token, which is portable, to the server alone with the Date
        // when it expires.
        String fbAccessToken = Session.getActiveSession().getAccessToken();
        Date fbAccessTokenExpires = Session.getActiveSession().getExpirationDate();
        JSONObject params = new JSONObject();
        try {
            params.put("access_token", fbAccessToken);
            params.put("expires_at", fbAccessTokenExpires.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception", e);
        }

        JsonObjectRequest jor = new JsonObjectRequest(
                com.android.volley.Request.Method.POST,
                endpoint,
                params,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject json) {
                        Log.i(TAG, json.toString());
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        Log.e(TAG, "FB Auth Error", e);
                    }     
                });
        WigwamNow.getQueue().add(jor);
    }
    
    /**
     * Post a photo from a {@link Uri} to the user's WigwamNow album.
     */
    @Override
    public boolean postPhoto(Uri photoUri, Activity activity) {
        final WigwamDetailActivity wda = (WigwamDetailActivity) activity;
        
        Session session = Session.getActiveSession();       
        if (!hasPublishPermissions()) {
            requestPublishPermissions(session, activity);
            return false;
        }
        
        String postingPhotoString = activity.getResources().getString(R.string.posting_photo);
        showProgressDialog(postingPhotoString, activity);
        
        Request.Callback callback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                onPostActionResponse(response, wda);
            }      
        };
        
        Bitmap bitmap = BitmapFactory.decodeFile(photoUri.getPath());
        Request request = Request.newUploadPhotoRequest(session, bitmap, callback);
        request.executeAsync(); 
        
        return true;
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    /**
     * Handle a Facebook error and display an appropriate dialog to the user.
     *
     * @param error the error to handle.
     */
    private void handleError(FacebookRequestError error, final Activity activity) {
        String dialogBody = null;
        DialogInterface.OnClickListener listener = null;

        if (error == null) {
            // There was no response from the server
            dialogBody = activity.getString(R.string.error_dialog_default_text);
        } else {
            switch (error.getCategory()) {
                case AUTHENTICATION_RETRY:
                    // Tell the user what happened by getting the message id and retry the operation
                    // later
                    String userAction = error.shouldNotifyUser() ? ""
                            : activity.getString(error.getUserActionMessageId());
                    dialogBody = activity.getString(R.string.error_authentication_retry, userAction);

                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Go to the Facebook mobile site
                            Intent intent = new Intent(Intent.ACTION_VIEW, M_FACEBOOK_URL);
                            activity.startActivity(intent);
                        }
                    };
                    break;

                case AUTHENTICATION_REOPEN_SESSION:
                    // Close the session and reopen it
                    dialogBody = activity.getString(R.string.error_authentication_reopen);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Session session = Session.getActiveSession();
                            if (session != null && !session.isClosed()) {
                                session.closeAndClearTokenInformation();
                            }
                        }
                    };
                    break;

                case PERMISSION:
                    // Error related to permissions
                    dialogBody = activity.getString(R.string.error_permission);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // TODO(samstern): Set this in the activity
                            // mPendingStructuredShare = true;
                            requestPublishPermissions(Session.getActiveSession(), activity);
                        }
                    };
                    break;

                case SERVER:
                case THROTTLING:
                    // This error is usually temporary, just ask the user to try again
                    dialogBody = activity.getString(R.string.error_server);
                    break;

                case BAD_REQUEST:
                    // Coding error, ask the user to file a bug if appropriate
                    dialogBody = activity.getString(R.string.error_bad_request, error.getErrorMessage());
                    break;

                case OTHER:
                case CLIENT:
                default:
                    // An unknown issue occurred. Could be a coding error or server side.
                    dialogBody = activity.getString(R.string.error_unknown, error.getErrorMessage());
                    break;
            }

            // Show the error dialog
            new AlertDialog.Builder(activity).setPositiveButton("OK", listener)
                    .setTitle("Error").setMessage(dialogBody).show();
        }
    }

    /**
     * Request permission to publish to the user's stream.
     *
     * @param session the active Facebook session.
     */
    public void requestPublishPermissions(Session session, Activity activity) {
        if (session != null) {
            Session.NewPermissionsRequest request = new Session.NewPermissionsRequest(
                    activity, PERMISSIONS).setRequestCode(REAUTH_ACTIVITY_CODE);
            session.requestNewPublishPermissions(request);
        }
    }
    
    /**
     * Responds to the result of a Facebook post action.
     *
     * @param response the action result.
     */
    public void onPostActionResponse(Response response, Activity activity) {
        // Dismiss the progress spinner if it's showing
        dismissProgressDialog();

        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);
        if (postResponse != null && postResponse.getId() != null) {
            String dialogBody = "Action posted.  ID: " + postResponse.getId();
            new AlertDialog.Builder(activity).setPositiveButton("OK", null)
                    .setTitle("Result").setMessage(dialogBody).show();
        } else {
            FacebookRequestError error = response.getError();
            if (error != null) {
                Log.e(TAG, error.toString());
            }
            handleError(error, activity);
        }
    }
    
    /**
     * Display a String in a progress dialog.
     * 
     * @param message the String to display
     */
    private void showProgressDialog(String message, Activity activity) {
        mProgressDialog = ProgressDialog.show(activity, "", message, true);
    }
    
    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Graph response allowing typed access to an ID.
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }

    /**
     * OpenGraph action for renting a wigwam. "wigwamnow:rent"
     */
    private interface RentAction extends OpenGraphAction {

        public WigwamGraphObject getWigwam();

        public void setWigwam(WigwamGraphObject wigwam);

    }

    /**
     * OpenGraph action for sharing a wigwam. "wigwamnow:share"
     */
    public interface ShareAction extends OpenGraphAction {

        public WigwamGraphObject getWigwam();

        public void setWigwam(WigwamGraphObject wigwam);

    }

    /**
     * GraphObject for a Wigwam. Has external URL and an ID (from server).
     */
    public interface WigwamGraphObject extends GraphObject {

        public String getUrl();

        public void setUrl(String url);

        public String getId();

        public void setId(String id);

    }

}

