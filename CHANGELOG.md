# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- New `TimeChanged` callback for reporting Playhead to conviva playback metric. Calculates Live and Vod playback for report.

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
