# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- Support for reporting the stack trace in case of error events, if provided by the player
- New `ConvivaAnalyticsIntegration.setAutoEndSession` to not end Conviva session automatically from certain player events if set to false (true by default).

## 2.7.2 - 2024-10-28

### Added
- Default value of `false` for `c3.ad.isSlate` for client side ad insertion per Conviva Custom Ad Manager integration docs

## 2.7.1 - 2024-09-24
### Fixed
- Reporting wrong ad position for mid-roll VMAP ads

## 2.7.0 - 2024-09-05
### Fixed
- Potential integration error shown in Touchstone if the player emits a warning outside of an active Conviva session

### Changed
- Updated Bitmovin Player to `3.81.0`

## 2.6.0 - 2024-08-28
### Added
- `averageBitrate` to reported video metrics
- Possibility to start session tracking without a `Player` instance
  - `ConvivaAnalyticsIntegration(customerKey:config:)` constructor without a `Player`
  - `ConvivaAnalyticsIntegration.attachPlayer()` to attach the `Player` at a later point in the session life-cycle

### Removed
- Unintentionally public initializers from `ConvivaAnalyticsIntegration` which were not intended to be public and only meant for testing

### Changed
- Updated Bitmovin Player to `3.78.2`
- Updated conviva-core to `4.0.39`

## 2.5.0 - 2024-07-05
### Added
- `ConvivaAnalyticsIntegration.ssai` namespace to enable server side ad tracking

### Fixed
- Potential exception when determining the IMA SDK version on ad start

## 2.4.0 - 2024-06-06
### Added
- Ad analytics for ad event reporting

### Changed
- Updated Bitmovin Player to `3.71.0`
- Updated IMA SDK to `3.31.0`
- Updated conviva-core to `4.0.37`
- Increased minimum required `compileSdk` version to `34`
- Increased `compileSdk` and `targetSdkVersion` to `34`
- Increased `minSdkVersion` to `19`
- Ad break started and ended is now reported in `PlayerEvent.AdBreakStarted` and `PlayerEvent.AdBreakFinished`
- Updated Kotlin to `1.9.23`
- Updated Gradle wrapper to `8.2` and AGP to `8.2.2`

### Removed
- Custom event for `AdSkipped` and `AdError`. Replaced by Conviva build in tracking

### Fixed
- The pom file now also includes the `com.bitmovin.player` dependency which was missing before

## 2.3.0 - 2024-05-21
### Added
- New `TimeChanged` callback for reporting Playhead to conviva playback metric. Calculates Live and Vod playback for report.
- New `MetadataOverrides.setAdditionalStandardTags` that allows to set additional standard tags for the session. The List of tags can be found here: [Pre-defined Video and Content Metadata](https://pulse.conviva.com/learning-center/content/sensor_developer_center/sensor_integration/android/android_stream_sensor.htm#PredefinedVideoandContentMetadata)

### Changed
- Updated conviva-core to 4.0.35

## 2.2.0 - 2023-07-18
### Added
- New `release(Boolean releaseConvivaSdk)` function allows for registering a new `ConvivaAnalyticsIntegration` to a
reused `Player`, when called with `releaseConvivaSdk = false` on the previous instance.

### Changed
- Updated BitmovinPlayer to 3.39.0
- Updated Kotlin to 1.7.0
- Updated compileSdkVersion to 33

## 2.1.4 - 2022-09-20
### Fixed

- Send buffering event to Conviva on seek/timeshift

## 2.1.3 - 2022-09-06
### Fixed

- Fixed remaining unit tests

## 2.1.2 - 2022-08-30
### Fixed

- Fixed a bug where MetadataOverrides#assetName was forgotten upon replaying a certain asset

## 2.1.1 - 2022-08-22
### Changed

- Instead of checking BuildConfig.DEBUG, check whether config contains values to be set.

## 2.1.0 - 2022-05-12
### Added

- This version features an update to the Conviva SDK to 4.0.20. This update adds API breaking changes.

### Removed
- Constructor: `ConvivaAnalyticsIntegration(Player player, String customerKey, Context context, ConvivaConfig config, Client client)`

### Changed
- `ConvivaAnalytics` is renamed to `ConvivaAnalyticsIntegration`
- 'ConvivaAnalyticsIntegration' method `pauseTracking()' is now 'pauseTracking(Boolean _isBumper)',
allowing specification of reason for pausing tracking, if desired.

## 2.0.1 - 2022-02-25
### Fixed
- Fixed an issue with one of the constructors not passing right parameter

### Known Issues
- Playlist API available in V3 player SDK is not supported yet.

## 2.0.0 - 2021-11-18
### Added
- This is first version using Bitmovin Player Android V3 SDK. This is not backward compatible with V2 player SDK.

### Changed
- `ConvivaConfiguration` is renamed to `ConvivaConfig`

### Known Issues
- Playlist API available in V3 player SDK is not supported yet.

## 1.1.5 - 2021-06-21
### Added
- Support for reporting timeshift events for live content playback.

## 1.1.4 - 2021-01-08
### Added
- Support to override values for internally defined custom metdata keys.

### Fixed
- App loses custom metadata when backgrounded.
