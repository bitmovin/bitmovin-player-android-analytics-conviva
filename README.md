# bitmovin-player-android-analytics-conviva
Integration of the Bitmovin Android Player SDK with the Conviva Analytics

#Getting Started
## Integratiing into your project

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

And this line to your main project
```
dependencies {
    compile 'com.bitmovin.analytics:collector:0.1.0'
}
```

Add the following permissions 

```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

##Examples

The following example create a ConvivaAnalytics object and attaches at Bitmovin Native SDK to it

#### Basic Conviva Reporting

```java
// Create a new source configuration
SourceConfiguration sourceConfiguration = new SourceConfiguration();

// Add a new source item
sourceConfiguration.addSourceItem("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");

// Create your ConvivaConfig object
ConvivaConfig convivaConfig = new ConvivaConfig("e94c66c4c6eb1d888077767e5db0d7b12b15f5b6", "https://rtl-nl-xl-test.testonly.conviva.com/","ConvivaExample_BitmovinPlayer","ViewerId1","Asset1");

// Add optional parameters
convivaConfig.setDebugLoggingEnabled(true);

// Create ConvivaAnalytics
convivaAnalytics = ConvivaAnalytics.getInstance();
convivaAnalytics.attachPlayer(convivaConfig, bitmovinPlayer, getApplicationContext());

// load source using the created source configuration
bitmovinPlayer.load(sourceConfiguration);
```

#### Switching to a new video
Before you switch to a new video, make sure to detach your player from the ConvivaAnalytics object.

```java

convivaAnalytics.detachPlayer();

```

#### Optional Configuration Parameters
```java

convivaConfig.setDebugLoggingEnabled(true);
convivaConfig.setCustomData(customMapOfKeyValuePairs);

```

A [full example app](https://github.com/bitmovin/bitmovin-player-android-analytics-conviva/tree/master/ConvivaExampleApp) can be seen in the github repo 
