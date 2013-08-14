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

import com.google.plus.wigwamnow.models.Wigwam;

import android.app.Activity;
import android.net.Uri;

import java.io.File;

/**
 * A base class to abstract all social actions in the WigwamNow app.  Should be extended once for
 * each Social SocialProviderConstants (Google+, Facebook, etc.) that will be linked to the app.
 * 
 * @author Sam Stern (samstern@google.com)
 */
public abstract class SocialProvider {
    
    /**
     * Enumeration of possible features that a SocialProvider can implement.  Each has an
     * associated method.
     */
    public enum SocialFeature {
        SHARE,
        STRUCTURED_SHARE,
        RENT,
        POST_PHOTO,
        HYBRID_AUTH
    }
    
    /**
     * Create the right {@link SocialProvider} subclass object, based on the value of a
     * {@link SocialProviderConstants} enum value.
     * 
     * @param provider the {@link SocialProviderConstants} for which the method should instantiate a
     * {@link SocialProvider}.
     * @return a subclass of the {@link SocialProvider} class, such as {@link GoogleProvider}.
     */
    public static SocialProvider get(int provider) {
        if (provider == SocialProviderConstants.GOOGLE) {
            return new GoogleProvider();
        } else if (provider == SocialProviderConstants.FACEBOOK) {
            return new FacebookProvider();
        } else {
            return null;
        }
    }
    
    /**
     * Determine if a SocialProvider supports a given SocialFeature.
     * 
     * @param feature the SocialFeature in question.
     * @return true if the action is supported, false otherwise.
     */
    public abstract boolean supports(SocialFeature feature);
    
    /**
     * Share a {@link Wigwam} in the user's social feed.
     * 
     * @param wigwam the {@link Wigwam} to share.
     * @param activity the {@link Activity} where the action was initiated.
     * @return true if success, false otherwise.
     */
    public abstract boolean share(Wigwam wigwam, Activity activity);
    
    /**
     * Create a rental action on the user's social graph.
     * 
     * @param wigwam the {@link Wigwam} to rent.
     * @param activity the {@link Activity} where the action was initiated.
     * @return true if success, false otherwise.
     */
    public abstract boolean rent(Wigwam wigwam, Activity activity);
    
    /**
     * Create a share action on the user's social graph.
     * 
     * @param wigwam the {@link Wigwam} to share.
     * @param activity the {@link Activity} where the action was initiated.
     * @return true if success, false otherwise.
     */
    public abstract boolean structuredShare(Wigwam wigwam, Activity activity);
    
    /**
     * Initiate authorization with the server.
     * 
     * @param activity the {@link Activity} where the action was initiated.
     */
    public abstract void hybridAuth(final Activity activity);
    
    /**
     * Post a photo to the user's albums.
     * 
     * @param photoUri a {@link Uri} pointing to the {@link File} where the photo is located.
     * @param activity the {@link Activity} where the action was initiated.
     * @return true if success, false otherwise
     */
    public abstract boolean postPhoto(Uri photoUri, Activity activity);
    
    /**
     * @return the display name of this SocialProvider, such as "Google+" or "Facebook".
     */
    public abstract String getName();
    
    /**
     * Exception to be thrown when a provider is asked to perform an unsupported feature.  Meant
     * to encourage use of the supports(...) method before calling a feature.
     */
    class UnsupportedFeatureException extends RuntimeException {
        
        public UnsupportedFeatureException(SocialFeature feature) {
            super("Unsupported: " + feature.name());
        }
        
    }
    
}

