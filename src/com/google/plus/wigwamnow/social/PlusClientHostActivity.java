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

import com.google.android.gms.plus.PlusClient;

/**
 * Interface for all activities that host an {@link PlusClient} or an {@link PlusClientFragment}.
 * 
 * @author Sam Stern (samstern@google.com)
 */
public interface PlusClientHostActivity {
    
    /**
     * Get the {@link PlusClient} that the Activity hosts.  This may directly return an instance
     * variable of the Activity or retrieve the client from a {@link PlusClientFragment}.
     * 
     * @return the {@link PlusClient} that the Activity uses.
     */
    public PlusClient getPlusClient();

}

