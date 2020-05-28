# bitmovin-player-android-analytics-conviva
Integration of the Bitmovin Android Player SDK with the Conviva Analytics

# Limitations
Our conviva integration does not support ad tracking yet. It will be added in a future release

# Getting Started

## Integrating into your project

Add this to your top level `build.gradle`

```
allprojects {
  repositories {
    maven {
      url  'http://bitmovin.bintray.com/maven' 
    }
  }
}
```

And this lines to your main project
```
dependencies {
  implementation 'com.conviva.sdk:conviva-core-sdk:2.145.1' // <-- conviva sdk
  implementation 'com.bitmovin.analytics:conviva:1.1.1'
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

The following example create a ConvivaAnalytics object and attaches at Bitmovin Native SDK to it

#### Basic Conviva Reporting

```java
// Create a new source configuration
SourceConfiguration sourceConfiguration = new SourceConfiguration();

// Add a new source item
DASHSource dashSource = new DASHSource("STREAM-URL");
SourceItem sourceItem = new SourceItem(dashSource);
sourceItem.setTitle("Asset Name"); // Important to set the Asset Name as it's required by Conviva
sourceConfiguration.addSourceItem(sourceItem);

// Create your ConvivaConfiguration object
ConvivaConfiguration convivaConfig = new ConvivaConfiguration(
    "ConvivaExample_BitmovinPlayer",
    "ViewerId1");

// Create ConvivaAnalytics
convivaAnalytics = new ConvivaAnalytics(bitmovinPlayer, "YOUR-CUSTOMER-KEY", getApplicationContext(), convivaConfig);

// load source using the created source configuration
bitmovinPlayer.load(sourceConfiguration);
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
// Initialize ConvivaAnalytics
// …

convivaAnalytics.updateContentMetadata(metadata);
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

`convivaAnalytics.endSession()`
Since the `BitmovinPlayer` automatically pauses the video if no background playback is configured the session creation after the app is in foreground again is handled automatically.

A [full example app](https://github.com/bitmovin/bitmovin-player-android-analytics-conviva/tree/master/ConvivaExampleApp) can be seen in the github repo 
