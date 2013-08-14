# WigwamNow - Android Client

## Description
WigwamNow is a Ruby on Rails application that demonstrates how to manage Google+ and Facebook integration in the same web application and showcases many of the features of each platform.

## Context
The WigwamNow project is a companion to the article
[Adding Google+ to your Facebook Integration](https://developers.google.com/+/web/facebook) on the Google+ Developers Site.  Please read the article for information on how to integrate Google+ Sign In and other features of the Google+ platform into your existing Facebook application.

## Requirements
1. ActionBarSherlock
2. Facebook Android SDK
3. Google Play Services

## Setup
### Server
See [gplus-wigwam-server](https://github.com/googleplus/gplus-wigwam-server) for information on how to set up the server component of this sample.  The Android client will <strong>not</strong> function without the server.

### Facebook Developers Dashboard
1. Navigate to the app that you configured for the server and click 'Edit App'.
2. Under 'Native Android App' add the Package Name `com.google.plus.wigwamnow` and the Class Name `com.google.plus.wigwamnow.MainActivity` and add your Key Hash.  Enable Facebook Login and Deep Linking.

### Google APIs Console
1. Navigate to the project that you configured for the server.
2. Follow the [Google+ Android Quickstart](https://developers.google.com/+/quickstart/android) to add a Client ID for the Android application to your server project.

### Configure App
1. Use [Facebook's Android SDK documentation](https://developers.facebook.com/docs/getting-started/facebook-sdk-for-android/3.0/), the [Google+ Android Quickstart](https://developers.google.com/+/quickstart/android), and the [ActionBarSherlock](http://actionbarsherlock.com/index.html) documentation to import the Facebook Android SDK, Google Play Services Library, and ActionBarSherlock into your IDE as Android libraries.
2. Clone this project using `git clone https://github.com/googleplus/gplus-wigwam-client-android` and import it into your IDE.
3. Add the three directories from Step 1 as Android library dependencies in your project.
4. In `res/values/strings.xml` replace `YOUR_FACEBOOK_APP_ID` with your Facebook App Id, `YOUR_GOOGLE_CLIENT_ID` with your Android Client ID from the Google APIs console, and `YOUR_SERVER_URL` with the URL to your WigwamNow server.

### Run
1. Install the app on an Android device with Google Play Services.
2. Make sure you have created Wigwams on the server, or the app will be empty!
