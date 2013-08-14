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

package com.google.plus.wigwamnow.models;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

/**
 * POJO to represent a listing for a {@link Wigwam}. Models the listing object on the server.
 *
 * @author samstern@google.com (Sam Stern)
 */
public class Listing {

    /** The Date when the listing starts **/
    @JsonProperty("start_date")
    private Date mStartDate;
    
    /** The Date when the listing ends **/
    @JsonProperty("end_date")
    private Date mEndDate;

    public Listing() {}

    /**
     * @return the Date when the listing starts
     */
    public Date getStartDate() {
        return mStartDate;
    }

    /**
     * @param startDate the Date when the listing starts
     */
    public void setStartDate(Date startDate) {
        mStartDate = startDate;
    }

    /**
     * @return the Date when the listing ends
     */
    public Date getEndDate() {
        return mEndDate;
    }

    /**
     * @param endDate the Date when the listing ends
     */
    public void setEndDate(Date endDate) {
        mEndDate = endDate;
    }

}

