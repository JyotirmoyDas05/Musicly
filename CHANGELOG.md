# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.8.0-beta] - 2026-02-20

### Added

- Hybrid HomeScreen blending local offline library and YouTube Music online content (feat: @Jyotirmoy)
- New Quick Picks grid for exploring online music on the Home Screen (feat: @Jyotirmoy)
- Automatic Update Checker with GitHub Release Integration (feat: @Jyotirmoy)
- Direct APK Download & Installation from Updates (feat: @Jyotirmoy)
- Device Architecture Detection for Optimal APK Selection (feat: @Jyotirmoy)
- Update Button in Home Top Bar (when updates available) (feat: @Jyotirmoy)
- Update Bottom Sheet with Release Information (feat: @Jyotirmoy)
- Support for Multiple Device Architectures (feat: @Jyotirmoy)

### Changed

- Upgraded AlbumArtCollage to dynamically interleave local and online songs (refactor: @Jyotirmoy)
- Improved 3-dots menu for albums and songs (feat: @Jyotirmoy)
- Download Progress Tracking & Visual Feedback (feat: @Jyotirmoy)
- Improved albums and Songs Image Cover Resolution (feat: @Jyotirmoy)
- Enabled Filter chips in Search (feat: @Jyotirmoy)
- Improved Library Sync (refactor: @Jyotirmoy)

### Fixed

- Fixed UI jumping and scroll loss when playing songs from search results (fix: @Jyotirmoy)
- Fixed 'Playlist not found' error when accessing online playlists from Home and Explore screens (fix: @Jyotirmoy)
- Resolved Hilt missing binding error for DownloadRepositoryImpl (fix: @Jyotirmoy)
- Fixed Ktor deprecation warning by updating download implementations (fix: @Jyotirmoy)

## [0.7.0-beta] - 2026-02-19

### Added

- Full YouTube Music online playlist integration (feat: @Jyotirmoy)
- Online playlist detail screen with M3 Expressive design (feat: @Jyotirmoy)
- Background playlist song pre-loading for seamless playback (feat: @Jyotirmoy)

### Changed

- Implemented Metrolist-pattern online song playback for proper stream resolution (refactor: @Jyotirmoy)
- Direct MediaItem creation from MediaMetadata for online content (refactor: @Jyotirmoy)
- Improved queue state management for online playlists (feat: @Jyotirmoy)

### Fixed

- Fixed online song playback skipping issues by correcting MediaItem URI format (fix: @Jyotirmoy)
- Resolved YouTubeMediaSourceHelper stream resolution by ensuring proper video ID detection (fix: @Jyotirmoy)
- Fixed MediaItem customCacheKey configuration for proper ResolvingDataSource integration (fix: @Jyotirmoy)
- Resolved build failure due to duplicate native libraries (fix: @Jyotirmoy)
- Improved automated release workflow (fix: @Jyotirmoy)

## [0.6.0-beta] - 2026-02-15

## [0.5.0-beta] - 2026-01-14

### Added

- Implemented 10-band Equalizer and effects suite (feat: @Jyotirmoy)
- Added M3U playlist import/export support (feat/fix: @lostf1sh, @Jyotirmoy)
- Integrated Deezer API for artist images (feat: @lostf1sh)
- Added Gemini AI model selection, system prompt settings, and AI playlist entry point (feat: @lostf1sh, @Jyotirmoy)
- Added sync offset support for lyrics and multi-strategy remote search (feat/fix: @lostf1sh, @Jyotirmoy)
- Added Baseline Profiles for improved performance (feat/fix: @Jyotirmoy, @google-labs-julesbot)
- Added support for custom playlist covers

### Changed

- **Material 3 Expressive UI**: Modernized Settings, Stats, Player, Bottom Sheets, and dialogs (refactor: @Jyotirmoy, @lostf1sh)
- **Library Sync**: Rebuilt initial sync flow with phase-based progress reporting and linear indicators (feat: @lostf1sh)
- **Settings Architecture**: Introduced category sub-screens and improved navigation handling (refactor/fix: @Jyotirmoy)
- **Queue & Player**: Decoupled queue updates from scroll animations, added animated queue scrolling (feat/fix: @lostf1sh, @Jyotirmoy)
- Improved widget previews and case-insensitive sorting logic (feat/fix: @lostf1sh, @google-labs-julesbot)

### Fixed

- Fixed casting stability, queue transitions, and reduced latency (fix: @Jyotirmoy)
- Fixed delayed content rendering and unwanted collapses in Player Sheet (fix/refactor: @Jyotirmoy)
- Fixed reordering issues in queue
- General crash fixes and minor UX improvements (fix: @lostf1sh, @Jyotirmoy)

## [0.4.0-beta] - 2025-12-15

### Added

- Major navigation redesign
- New file explorer for choosing source directories
- Landscape mode (thanks to "leave this blank for now")
- New Connectivity and casting functionalities
- Seamless continuity between remote devices
- Gapless transition between songs
- Crossfade
- New Custom Transitions feature (only for playlists)
- Keep playing after closed the app
- UI Optimizations
- Improved stats feature
- Redesigned Queue control with more features
- Improved different filetypes support for playing and metadata editing
- Improved permission controller
- Minor bug fixes

## [0.3.0-beta] - 2025-10-28

### What's new

- Introduced a richer listening stats hub with deeper insights into your sessions.
- Launched a floating quick player to instantly open and preview local files.
- Added a folders tab with a tree-style navigator and playlist-ready view.

### Improvements

- Refined the overall Material 3 UI for a cleaner and more cohesive experience.
- Smoothed out animations and transitions across the app for more fluid navigation.
- Enhanced the artist screen layout with richer details and polish.
- Upgraded DailyMix and YourMix generation with smarter, more diverse selections.
- Strengthened the AI assistant to deliver more relevant playback suggestions.
- Improved search relevance and presentation for faster discovery.
- Expanded support for a broader range of audio file formats.

### Fixes

- Resolved metadata quirks so song details stay accurate everywhere.
- Restored notification shortcuts so they reliably jump back into playback.

## [0.2.0-beta] - 2024-09-15

### Added

- Chromecast support for casting audio from your device (temporarily disabled).
- In-app changelog to keep you updated on the latest features.
- Improved lyrics search
- Support for .LRC files, both embedded and external.
- Offline lyrics support.
- Synchronized lyrics (synced with the song).
- New screen to view the full queue.
- Reorder and remove songs from the queue.
- Mini-player gestures (swipe down to close).
- Added more material animations.
- New settings to customize the look and feel.
- New settings to clear the cache.

### Changed

- Complete redesign of the user interface.
- Complete redesign of the player.
- Performance improvements in the library.
- Improved application startup speed.
- The AI now provides better results.

### Fixed

- Fixed various bugs in the tag editor.
- Fixed a bug where the playback notification was not clearing.
- Fixed several bugs that caused the app to crash.

## [0.1.0-beta] - 2024-08-30

### Added

- Initial beta release of Musicly Music Player.
- Local music scanning and playback (MP3, FLAC, AAC).
- Background playback using a foreground service and Media3.
- Modern UI with Jetpack Compose, Material 3, and Dynamic Color support.
- Music library organization by songs, albums, and artists.
- Home screen widget for music control.
- Real-time audio waveform visualization.
- Built-in tag editor for song metadata.
- AI-powered features using Gemini.
- Smooth in-app permission handling.
