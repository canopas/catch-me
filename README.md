<a href="https://canopas.com/contact"><img src="./screenshots/cta_banner.png" width="100%"></a>

If you are interested in building apps or designing products, please let us know. We'd love to hear from you!

<a href="https://canopas.com/contact"><img src="./screenshots/cta_btn.png"></a>

<img src="./screenshots/cover_image.png" />

# YourSpace
#### Stay connected, Anywhere!

An android application for Family safety and location sharing.
Keep your loved ones safe with YourSpace, the go-to app for family safety.
Stay connected with Real-Time Location Sharing.

Download now and elevate your family's safety today!


<a href="https://play.google.com/store/apps/details?id="><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height=60px /></a>

## Status: 🚧 In progress 🚧
YourSpace is still in development, and certain screens have not been fully implemented yet.
We are actively working on completing these features to enhance the overall functionality of the platform.

## Screenshots

<img src="./screenshots/yourspace_ss_1.png" height="540" /> <img src="./screenshots/yourspace_ss_2.png" height="540" /> 
<img src="./screenshots/yourspace_ss_3.png" height="540" /> <img src="./screenshots/yourspace_ss_4.png" height="540" /> 

## Requirements
Make sure you have the latest stable version of Android Studio installed.
You can then proceed by either cloning this repository or importing the project directly into Android Studio, following the steps provided in the [documentation](https://developer.android.com/jetpack/compose/setup#sample).

### Google Maps SDK
To enable the MapView functionality, obtaining an API key as instructed in the [documentation](https://developers.google.com/maps/documentation/android-sdk/get-api-key) is required. This key should then be included in the local.properties file as follows:

```
MAPS_API_KEY=your_map_api_key
```

### Firebase Setup
To enable Firebase services, you will need to create a new project in the [Firebase Console](https://console.firebase.google.com/).
Use the `applicationId` value specified in the `app/build.gradle` file of the app as the Android package name.
Once the project is created, you will need to add the `google-services.json` file to the app module.
For more information, refer to the [Firebase documentation](https://firebase.google.com/docs/android/setup).

YourSpace uses the following Firebase services, Make sure you enable them in your Firebase project:
- Authentication (Phone, Google)
- Firestore (To store user data)

## Features

We are currently in the process of implementing the core features of our system, with plans to incorporate additional features shortly.
While many of the features listed below are either not yet implemented or are still undergoing refinement to ensure stability, we are working diligently to finalize them soon.


## Tech stack

YourSpace utilizes the latest Android technologies and adheres to industry best practices. Below is the current tech stack used in the development process:

- MVVM Architecture
- Jetpack Compose
- Kotlin
- Coroutines + Flow
- Jetpack Navigation
- DataStore
- Hilt

## Contribution
Currently, we are not accepting any contributions.

## Credits
YourSpace is owned and maintained by the [Canopas team](https://canopas.com/). You can follow them on Twitter at [@canopassoftware](https://twitter.com/canopassoftware) for project updates and releases.

Let us know if you are interested in building Apps or Designing Products.


## License
All the code is available under the MIT license. See [LICENSE]("").
