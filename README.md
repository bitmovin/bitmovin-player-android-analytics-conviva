# Bitmovin Player Conviva Analytics Integration
This is an open-source project to enable the use of a third-party component (Conviva) with the Bitmovin Player Android SDK.

## Maintenance and Update
This project is not part of a regular maintenance or update schedule and is updated once yearly to conform with the latest product versions. For additional update requests, please take a look at the guidance further below.

## Contributions to this project
As an open-source project, we are pleased to accept any and all changes, updates and fixes from the community wishing to use this project. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to contribute.

## Reporting player bugs
If you come across a bug related to the player, please raise this through your support ticketing system.

## Need more help?
Should you want some help updating this project (update, modify, fix or otherwise) and cant contribute for any reason, please raise your request to your bitmovin account team, who can discuss your request.

## Support and SLA Disclaimer
As an open-source project and not a core product offering, any request, issue or query related to this project is excluded from any SLA and Support terms that a customer might have with either Bitmovin or another third-party service provider or Company contributing to this project. Any and all updates are purely at the contributor's discretion.

Thank you for your contributions!

## Limitations
- Tracking multiple sources within a Playlist, and related use cases, introduced in Player Android SDK version `v3` are not supported.

## Compatibility

The 2.x.x versions of the Conviva Analytics Integration depends on `BitmovinPlayer` version `>= 3.0.0`.

Note: `BitmovinPlayer` version `2.x.x` is not supported anymore. Please upgrade to `BitmovinPlayer` version `3.x.x`.

# Getting Started

## Integrating into your project

Add this to your top level `build.gradle`

```
allprojects {
  repositories {
    maven {
      url  'https://artifacts.bitmovin.com/artifactory/public-releases'
    }
  }
}
```

And these lines to your main project
```
dependencies {
  implementation 'com.conviva.sdk:conviva-core-sdk:4.0.35' // <-- conviva sdk
  implementation 'com.bitmovin.analytics:conviva:2.2.0'
}
```

Add the following permissions

```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

For more information about permissions and collected network types please look at the [Conviva Documentation](https://community.conviva.com/site/global/platforms/android/android_sdk/taskref/index.gsp#report_network_metrics).

## Examples

The following example create a ConvivaAnalyticsIntegration object and attaches at Bitmovin Native SDK to it

#### Basic Conviva Reporting

```java
// Create your ConvivaConfiguration object
ConvivaConfiguration convivaConfig = new ConvivaConfig(
    "ConvivaExample_BitmovinPlayer",
    "ViewerId1");

// Create ConvivaAnalyticsIntegration
convivaAnalyticsIntegration = new ConvivaAnalyticsIntegration(bitmovinPlayer, "YOUR-CUSTOMER-KEY", getApplicationContext(), convivaConfig);

// Add a new source item
SourceConfig sourceConfig = new SourceConfig("STREAM-URL", SourceType.Dash);
sourceConfig.setTitle("Asset Name"); // Important to set the Asset Name as it's required by Conviva
Source source = Source.create(sourceConfig);

// load source
bitmovinPlayer.load(source);
```

#### Optional Configuration Parameters
```java

convivaConfig.setDebugLoggingEnabled(true);
convivaConfig.setCustomData(customMapOfKeyValuePairs);

```

#### Content Metadata handling

If you want to override some content metadata attributes you can do so by adding the following:

```java
MetadataOverrides metadata = new MetadataOverrides();
metadata.setApplicationName("Bitmovin Android Conviva integration example app");
metadata.setViewerId("awesomeViewerId");
Map<String, String> customInternTags = new HashMap<>();
customInternTags.put("contentType", "Episode");
metadata.setCustom(customInternTags);

// …
// Initialize ConvivaAnalyticsIntegration
// …

convivaAnalyticsIntegration.updateContentMetadata(metadata);
```

Those values will be cleaned up after the session is closed.

#### Consecutive playback
	
If you want to use the same player instance for multiple playback, just load a new source with player.load(…). The integration will close the active session.
	
```java
player.load(…);
```

#### Background handling

If your app stops playback when entering background conviva suggests to end the active session. Since the integration can't know if your app supports background playback this can't be done automatically.

A session can be ended using following method call:

`convivaAnalyticsIntegration.endSession()`
Since the `BitmovinPlayer` automatically pauses the video if no background playback is configured the session creation after the app is in foreground again is handled automatically.

#### Clean up

At end of app instance lifecycle, the convivaAnalyticsIntegration should be released:

```java
@Override
protected void onDestroy() {
    bitmovinPlayerView.onDestroy();
    convivaAnalyticsIntegration.release();
    super.onDestroy();
}
```

A [full example app](https://github.com/bitmovin/bitmovin-player-android-analytics-conviva/tree/master/ConvivaExampleApp) can be seen in the github repo

