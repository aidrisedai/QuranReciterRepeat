# 🏗️ Engineering Artifact — Repeat Quran (v1)

## High-Level Architecture (with Diagram)

### Architecture Diagram
```mermaid
flowchart TD
    subgraph UI[UI Layer - Jetpack Compose]
        A1[Home: Dropdowns (Surah, Verse, Reciter, Repeat)]
        A2[Playback Screen: Play/Pause, Repeat, Change Reciter]
        A3[History Tab + Streak Banner]
        A4[Downloads Tab (Persistent Files)]
    end

    subgraph VM[ViewModels - MVVM + Hilt]
        B1[SessionViewModel]
        B2[HistoryViewModel]
        B3[PlaybackViewModel]
    end

    subgraph Data[Data Layer - Room DB + Repos]
        C1[Room DB: History + Streaks]
        C2[RecitationRepository (API)]
        C3[SessionRepository (Local)]
        C4[AnalyticsRepository (Local Logs)]
    end

    subgraph Playback[Playback Layer]
        D1[ExoPlayer Instance]
        D2[Playback Service + MediaSession]
    end

    subgraph Storage[Storage Layer]
        E1[Internal Files Directory]
        E2[Persistent Verse Files (Saved on First Play)]
    end

    subgraph Net[Networking]
        F1[Direct HTTPS Calls to Public Qur’an API]
    end

    %% Flows
    UI --> VM
    VM --> Data
    Data --> Playback
    Playback --> Storage
    VM --> Playback
    Net --> C2
    Playback --> UI
    Storage --> DownloadsTab[(Downloads Tab View)]
Layers & Flows
UI Layer (Jetpack Compose, MVVM)

Dropdowns (Surah, Verse, Reciter, Repeat)

Playback controls (play/pause, next/prev, repeat, change reciter)

History tab + Streak banner

Downloads tab (shows persistent files)

ViewModels (Hilt DI)

Manage session state (surah, reciter, repeat)

Expose LiveData/Flow to UI

Orchestrate persistence and playback

Data Layer

Room Database: history, streaks (computed from session logs), presets flagged in history

Repositories:

RecitationRepository (fetch from Qur’an API)

SessionRepository (local history)

AnalyticsRepository (local logs only)

Playback Layer

ExoPlayer (single instance)

Verse-level audio files queued sequentially

Foreground Service + MediaSession for background playback & Bluetooth car controls

Hybrid error handling: auto-retry once, then actionable message

Storage Layer

Persistent files in app’s internal storage (files/ dir)

Files saved on first play (progressive caching)

No auto-eviction — storage grows until user clears app or you add management later

Networking

Direct HTTPS calls to public Qur’an audio API

No proxy/backend in v1

Repository abstraction so backend swap possible later

Decisions + Alternatives
Audio Source: Existing Qur’an API (Alt: custom CDN, hybrid).

Playback Engine: ExoPlayer sequential queue (Alt: MediaPlayer, parallel sync).

Storage: Room DB for history/streaks (Alt: SharedPreferences, hybrid).

Caching: Persistent files on first play (Alt: ExoPlayer cache, hybrid with manual downloads).

Streaks: Computed from raw logs (Alt: cached count, hybrid).

Analytics: Local logs only (Alt: Firebase, hybrid).

Error Handling: Hybrid (auto-retry once, then message) (Alt: plain only, resilient retry).

Architecture: MVVM + Hilt DI (Alt: MVP, Clean Arch).

UI: Jetpack Compose (Alt: XML, hybrid).

Granularity: Verse-level files (Alt: surah-level + seek, hybrid).

Background Playback: Foreground Service + MediaSession (Alt: none, hybrid).

Downloads: Persistent files, invisible cache for v1 (Alt: manual downloads, hybrid).

Onboarding: Single static screen (Alt: interactive, hybrid).

Testing: Hybrid QA (unit tests + structured manual) (Alt: manual only, automated heavy).

Release: APK sideload (Alt: Play internal track, hybrid).

Security: Direct HTTPS calls (Alt: proxy API, hybrid).

Feasibility Score
4.5 / 5

✅ Strong alignment with PRD + Design.

✅ Lean for v1, no backend required.

✅ Technically feasible with ExoPlayer + Room + Compose.

⚠️ Risks: storage growth (persistent files), dropdown-heavy UX fatigue.

Blind Spots
Storage bloat on low-end devices (no eviction in v1).

Dependency on external Qur’an API (uptime + licensing).

Verse-level file granularity = many network calls → must test performance.

Local-only analytics → blind to cohort-wide retention/engagement.

Icon-only bottom nav may confuse users (from UX review).

Growth Reflection
This v1 architecture is lean but future-ready:

Keeps everything local (no accounts, no backend).

Lets you test adoption, memorization flows, and caching reliability quickly.

You’ve deliberately chosen simplicity over flexibility (e.g., no manual downloads, no backend).

That’s good for launch velocity. But as usage scales, you’ll need to revisit:

Storage management (clear cache, per-surah delete).

API control (proxy/CDN for reliability + licensing).

Analytics (Firebase or Amplitude for retention insights).

UX fatigue (move toward voice input or smart defaults).