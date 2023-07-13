# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Changed
- Updated BitmovinPlayer to 3.39.0
- Updated Kotlin to 1.7.0
- Updated compileSdkVersion to 33

### Added
- New `release(Boolean releaseConvivaSdk)` function allows for `ConvivaAnalyticsIntegration` to be reused, when called
with `releaseConvivaSdk = false`.

## [2.1.4]
### Fixed

- Send buffering event to Conviva on seek/timeshift

## [2.1.3]
### Fixed

- Fixed remaining unit tests

## [2.1.2]
### Fixed

- Fixed a bug where MetadataOverrides#assetName was forgotten upon replaying a certain asset

## [2.1.1]
### Changed

- Instead of checking BuildConfig.DEBUG, check whether config contains values to be set.

## [2.1.0]
### Added

- This version features an update to the Conviva SDK to 4.0.20. This update adds API breaking changes.

### Removed
- Constructor: `ConvivaAnalyticsIntegration(Player player, String customerKey, Context context, ConvivaConfig config, Client client)`

### Changed
- `ConvivaAnalytics` is renamed to `ConvivaAnalyticsIntegration`
- 'ConvivaAnalyticsIntegration' method `pauseTracking()' is now 'pauseTracking(Boolean _isBumper)',
allowing specification of reason for pausing tracking, if desired.

## [2.0.1]
### Fixed
- Fixed an issue with one of the constructors not passing right parameter

### Known Issues
- Playlist API available in V3 player SDK is not supported yet.

## [2.0.0]
### Added
- This is first version using Bitmovin Player Android V3 SDK. This is not backward compatible with V2 player SDK.

### Changed
- `ConvivaConfiguration` is renamed to `ConvivaConfig`

### Known Issues
- Playlist API available in V3 player SDK is not supported yet.

## [1.1.5]
### Added
- Support for reporting timeshift events for live content playback.

## [1.1.4] (2021-01-08) : First changelog entry. Consider this as baseline.
### Added
- Support to override values for internally defined custom metdata keys.

### Fixed
- App loses custom metadata when backgrounded.
