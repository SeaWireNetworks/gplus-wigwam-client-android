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

import android.os.Parcel;
import android.os.Parcelable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Wigwam model object. POJO modeling Wigwam object on the server.
 *
 * @author samstern@google.com (Sam Stern)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wigwam implements Parcelable {

    /** The wigwam's unique id **/
    @JsonProperty("id")
    private Integer mId;
    
    /** The wigwam's English name **/
    @JsonProperty("name")
    private String mName;
    
    /** A brief description of the wigwam **/
    @JsonProperty("description")
    private String mDescription;
    
    /** The wigwam's price, in dollars per night **/
    @JsonProperty("price")
    private Integer mPrice;
    
    /** The link to a picture of the wigwam **/
    @JsonProperty("src")
    private String mSrc;
    
    /** The street address of the wigwam, ex: 123 Fake Street **/
    @JsonProperty("street")
    private String mStreet;
    
    /** The city in which the wigwam is located **/
    @JsonProperty("city")
    private String mCity;
    
    /** The state in which the wigwam is located **/
    @JsonProperty("state")
    private String mState;
    
    /** The zip code for the wigwam's location **/
    @JsonProperty("zip")
    private String mZip;
    
    /** The wigwam's latitude coordinate **/
    @JsonProperty("lat")
    private Double mLat;
    
    /** The wigwam's longitude coordinate **/
    @JsonProperty("lng")
    private Double mLng;
    
    /** Restore a {@link Wigwam} from a {@link Parcel} **/
    public static Parcelable.Creator<Wigwam> CREATOR = new Parcelable.Creator<Wigwam>() {

        @Override
        public Wigwam createFromParcel(Parcel parcel) {
            Wigwam wigwam = new Wigwam();
            // Read string properties
            String[] stringProps = new String[7];
            parcel.readStringArray(stringProps);
            wigwam.mName = stringProps[0];
            wigwam.mDescription = stringProps[1];
            wigwam.mSrc = stringProps[2];
            wigwam.mStreet = stringProps[3];
            wigwam.mCity = stringProps[4];
            wigwam.mState = stringProps[5];
            wigwam.mZip = stringProps[6];
            
            // Read int properties
            int[] intProps = new int[2];
            parcel.readIntArray(intProps); 
            wigwam.mId = intProps[0];
            wigwam.mPrice = intProps[1];
            
            // Read double properties
            double[] doubleProps = new double[2];
            parcel.readDoubleArray(doubleProps);
            wigwam.mLat = doubleProps[0];
            wigwam.mLng = doubleProps[1];
            
            return wigwam;
        }

        @Override
        public Wigwam[] newArray(int size) {
            return new Wigwam[size];
        }
        
    };

    public Wigwam() {}

    /**
     * @return the name
     */
    public String getName() {
        return mName;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        mDescription = description;
    }

    /**
     * @return the price
     */
    public Integer getPrice() {
        return mPrice;
    }

    /**
     * @param price the price to set
     */
    public void setPrice(Integer price) {
        mPrice = price;
    }

    /**
     * @return the src
     */
    public String getSrc() {
        return mSrc;
    }

    /**
     * @param src the src to set
     */
    public void setSrc(String src) {
        mSrc = src;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return mId;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        mId = id;
    }

    /**
     * @return the street
     */
    public String getStreet() {
        return mStreet;
    }

    /**
     * @param street the street to set
     */
    public void setStreet(String street) {
        mStreet = street;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return mCity;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        mCity = city;
    }

    /**
     * @return the state
     */
    public String getState() {
        return mState;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        mState = state;
    }

    /**
     * @return the zip
     */
    public String getZip() {
        return mZip;
    }

    /**
     * @param zip the zip to set
     */
    public void setZip(String zip) {
        mZip = zip;
    }

    /**
     * @return the lat
     */
    public Double getLat() {
        return mLat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(Double lat) {
        mLat = lat;
    }

    /**
     * @return the lng
     */
    public Double getLng() {
        return mLng;
    }

    /**
     * @param lng the lng to set
     */
    public void setLng(Double lng) {
        mLng = lng;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write all string properties
        dest.writeStringArray(new String[] { this.mName, this.mDescription, this.mSrc,
                this.mStreet, this.mCity, this.mState, this.mZip });
        // Write all the int properties
        dest.writeIntArray(new int[] { this.mId, this.mPrice });
        // Write all the double properties
        dest.writeDoubleArray(new double[] { this.mLat, this.mLng });
    }

}

