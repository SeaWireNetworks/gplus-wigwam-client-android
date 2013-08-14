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
import com.google.android.gms.plus.model.people.Person;
import com.google.plus.wigwamnow.models.Wigwam;
import com.google.plus.wigwamnow.social.SocialProviderConstants;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment shown once the user has logged in. Shows personalized header and list of all available
 * {@link Wigwam}s.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class SelectionFragment extends Fragment implements PlusClient.OnPersonLoadedListener {

    private static final String TAG = SelectionFragment.class.getSimpleName();
    
    /** Request code for when new Facebook permissions are requested **/
    private static final int REAUTH_ACTIVITY_CODE = 100;
    
    /** Regular expression to parse deep links to Wigwams, from Facebook or Google+ **/
    private static final Pattern DEEPLINK_PATTERN = Pattern.compile("/wigwams/([0-9]+)");

    /** Image view that displays the current user's Facebook profile picture **/
    private ProfilePictureView mFbProfilePictureView;
    
    /** Image view that displays the current user's Google+ profile picture **/
    private NetworkImageView mPlusProfilePictureView;
    
    /** Text view to display the current user's name **/
    private TextView mUserNameView;
    
    /** Helper to manage the lifecycle of the Facebook {@link Session} **/
    private UiLifecycleHelper mUiHelper;
    
    /** Callback for when the Facebook {@link Session} state changes **/
    private Session.StatusCallback mCallback = new Session.StatusCallback() {
        @Override
        public void call(
                final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
    /** Adapter for the list of {@link Wigwam}s **/
    private WigwamArrayAdapter mAdapter;
    
    /** List view for displaying the list of {@link Wigwam}s **/
    private ListView mList;
    
    /** Progress dialog for when data is loading over the network **/
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHelper = new UiLifecycleHelper(getActivity(), mCallback);
        mUiHelper.onCreate(savedInstanceState);
    }
    
    /**
     * Parses any incoming Facebook or Google+ Deep Link and directs to the appropriate activity.
     */
    protected void parseDeepLink() {
        String deepLinkString = null;
        // Check for data on the intent, which could contain a Deep Link
        Uri targetUri = getActivity().getIntent().getData();
        if (targetUri != null) {
            try {
                deepLinkString = URLDecoder.decode(targetUri.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error parsing deep link " + targetUri.toString());
            }
        }
        if (deepLinkString != null) {
            // There has been some data passed with this intent, see if it matches the deep link
            // regular expression.
            Matcher m = DEEPLINK_PATTERN.matcher(deepLinkString);
            if (m.find()) {
                // Extract the ID of the Deep-Linked wigwam.  The first group of the regex will find
                // any number appearing after the string "/wigwams/"
                String idString = deepLinkString.substring(m.start(1), m.end(1));
                int wigwamId = Integer.parseInt(idString);
                loadWigwamFromId(wigwamId);
            }
        }
    }

    /**
     * Loads a Wigwam from the server based on its numeric id. When finished, launches the
     * WigwamDetailActivity for the loaded Wigwam (if successful).
     *
     * @param wigwamId the id of the wigwam to load
     */
    private void loadWigwamFromId(int wigwamId) {
        // Show the progress dialog
        mProgressDialog = ProgressDialog.show(getActivity(), "", "Loading wigwam...", true);

        String host = getActivity().getResources().getString(R.string.external_host);
        String path = host + "/wigwams/" + Integer.toString(wigwamId) + ".json";
        // Asynchronously load the wigwam from the server.
        StringRequest sr =
                new StringRequest(path, new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Convert to Java Object
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            // Decode the JSON Wigwam to a POJO and launch the detail view as if
                            // the Wigwam was selected from a list
                            Wigwam wigwam = mapper.readValue(response, Wigwam.class);
                            MainActivity host = (MainActivity) getActivity();
                            mProgressDialog.dismiss();
                            host.wigwamSelected(wigwam);
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
                        mProgressDialog.dismiss();
                        Toast.makeText(getActivity(), "Failed to load Wigwam.", Toast.LENGTH_SHORT)
                                .show();
                        Log.e(TAG, error.toString());
                    }
                });
        WigwamNow.getQueue().add(sr);
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiHelper.onResume();
    }
    
    @Override
    public void onHiddenChanged(boolean isHidden) {
        if (!isHidden) {
            personalizeView();
        }
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
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.selection, container, false);
        // Display the user's name and picture, from either social provider
        mFbProfilePictureView = (ProfilePictureView) view.findViewById(R.id.fb_profile_pic);
        mFbProfilePictureView.setCropped(true);
        mPlusProfilePictureView = (NetworkImageView) view.findViewById(R.id.plus_profile_pic);
        mUserNameView = (TextView) view.findViewById(R.id.selection_user_name);
        
        mList = (ListView) view.findViewById(R.id.stream_list_view);
        mList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Wigwam selected = mAdapter.getItem(position);
                MainActivity host = (MainActivity) getActivity();
                host.wigwamSelected(selected);
            }

        });
        
        loadWigwams();

        return view;
    }

    /**
     * Populates the {@link ListView} with Wigwams from the server.  Fetches the JSON description
     * of all Wigwams asynchronously and then parses it as a list of POJOs.
     */
    public void loadWigwams() {
        String host = getResources().getString(R.string.external_host);
        String path = host + "/wigwams.json";

        // Download list of Wigwams
        StringRequest sr =
                new StringRequest(path, new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Convert to Java Objects
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            Wigwam[] values = mapper.readValue(response, Wigwam[].class);
                            populateList(values);
                        } catch (JsonParseException e) {
                            Log.e(TAG, "Unable to parse JSON response", e);
                        } catch (JsonMappingException e) {
                            Log.e(TAG, "Unable to map to JSON", e);
                        } catch (IOException e) {
                            Log.e(TAG, "IOException in Wigwam loading", e);
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
     * Populate the {@link ListView} with an array of {@link Wigwam}s.
     * 
     * @param values an array of {@link Wigwam} objects to display.
     */
    private void populateList(Wigwam[] values) {
        mAdapter = new WigwamArrayAdapter(getActivity(), values);
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            mUiHelper.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    /**
     * Personalize the top bar with a user's picture and name, from either provider.
     */
    private void personalizeView() {
        MainActivity host = (MainActivity) getActivity();
        int current = host.currentProvider();
        if (current == SocialProviderConstants.FACEBOOK) {
            // Make Facebook personalization request
            makeMeRequest(Session.getActiveSession());
        }
    }
    
    @Override
    public void onPersonLoaded(ConnectionResult status, Person person) {
        if (status.getErrorCode() == ConnectionResult.SUCCESS) {
            // The user's personal information has been successfully loaded from Google+
            mUserNameView.setText(person.getDisplayName());
            mFbProfilePictureView.setVisibility(View.GONE);
            mPlusProfilePictureView.setVisibility(View.VISIBLE);
            mPlusProfilePictureView.setImageUrl(person.getImage().getUrl(), 
                    WigwamNow.getImageLoader());
        }
    }

    /**
     * Make request for Facebook User data
     *
     * @param session the active Facebook {@link Session}.
     */
    private void makeMeRequest(final Session session) {
        if (session == null || !session.isOpened()) {
            Log.e(TAG, "Error: Session not valid");
            return;
        }
        
        // Get user data and define a response callback
        Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {

            @Override
            public void onCompleted(GraphUser user, Response response) {
                // If response successful
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        // Set user name and profile picture
                        mPlusProfilePictureView.setVisibility(View.GONE);
                        mFbProfilePictureView.setVisibility(View.VISIBLE);
                        mFbProfilePictureView.setProfileId(user.getId());
                        mUserNameView.setText(user.getName());
                    }
                }
                if (response.getError() != null) {
                    Toast.makeText(getActivity(), 
                            "Error retrieving user info", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, response.getError().toString());
                }
            }

        });
        request.executeAsync();
    }

    /**
     * Callback when the state of the Facebook Session changes.
     *
     * @param session the {@link Session} that changed.
     * @param state the current state of the {@link Session}.
     * @param exception any exception which occurred.
     */
    private void onSessionStateChange(
            final Session session, SessionState state, Exception exception) {
        if (session != null && session.isOpened()) {
            // Get user data
            personalizeView();
        } else {
            // Return to login screen
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.showLoginFragment();
        }
    }

}

