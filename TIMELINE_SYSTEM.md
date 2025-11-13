# Timeline Projection System

## Overview
The timeline projection system provides users with realistic completion estimates for their memorization goals based on their available daily time and actual learning velocity.

## Features Implemented

### 1. **TimelineCalculator**
- Calculates projected ayahs per day based on available daily minutes
- Accounts for 50% review overhead (as learned material grows, more time goes to reviews)
- Tracks actual learning velocity from last 14 days of activity
- Compares projected vs actual pace to determine if user is on track
- Provides completion date estimates

**Time Estimates:**
- New learning: 2 minutes per aya (slow×5 + fast×10 repetitions)
- Review: 1 minute per aya
- Review overhead: 50% of time allocated to reviews

### 2. **Predefined Goals**
- **Juz ʿAmma** (Surahs 78-114): 564 ayahs
- **Juz 29** (Surahs 67-77): 390 ayahs
- **Last 10 Surahs**: 129 ayahs
- **Full Quran**: 6,236 ayahs

### 3. **MemorizationGoal Entity**
Tracks:
- Active goal and goal type
- Total ayahs in goal
- Baseline date and ayahs learned when goal was set
- Original projected days to complete
- Completion status and date

### 4. **TimelineSettingsActivity**
Allows users to:
- Set daily available memorization time (minimum 30 minutes)
- Select memorization goal
- View live projection updates as settings change
- See:
  - Current progress (ayahs learned / total)
  - Projected completion date
  - Days to completion
  - On-track status (ahead/behind/on track)
  - Milestone messages

### 5. **Timeline Card in MemorizationActivity**
Displays:
- Current goal name
- Progress percentage
- Projected completion date and days remaining
- Status (✅ on track, ⚠️ behind schedule, 🌱 milestone messages)
- Quick access button to Timeline Settings

## Adaptive Behavior

### Automatic Timeline Adjustments

1. **Daily Minutes Change**
   - Immediately recalculates timeline when user updates daily available time
   - Adjusts time allocations: 50% new learning, 30% recent reviews, 20% old reviews
   - Updates completion date projection

2. **Actual vs Projected Velocity**
   - Tracks ayahs learned per day over last 14 days
   - Compares with projected pace
   - Shows user if they're ahead (✅), on track (✅), or behind (⚠️)
   - Calculates days ahead/behind schedule

3. **Missed Sessions**
   - When user misses days, actual velocity drops
   - Timeline automatically extends to reflect realistic completion date
   - Status updates to show days behind schedule

4. **Faster Learning**
   - If user learns faster than projected pace, timeline shortens
   - Status shows days ahead of schedule
   - Motivates continued progress

### Milestone Messages
Progress-based encouragement:
- 🌱 Beginning journey (0-9%)
- ✨ Off to a strong start! (10-24%)
- 📈 Great progress! (25-49%)
- 💪 Halfway there! (50-74%)
- 🔥 Final stretch! (75-99%)
- 🎉 Goal Complete! (100%)

## Integration with Adaptive Learning

The timeline system works alongside the adaptive chunk size system:

1. **PerformanceTracker** adjusts chunk sizes (1-5 ayahs) based on success rate
2. **TimelineCalculator** uses current chunk size to estimate learning velocity
3. As chunk sizes increase/decrease, projected completion dates adjust accordingly
4. Timeline remains realistic by factoring in both available time and actual performance

## Database Schema

### memorization_goals Table
```sql
CREATE TABLE memorization_goals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    goalType TEXT,              -- "juz_amma", "juz_29", "last_10", "full_quran", "custom"
    customStartSurah INTEGER,   -- for custom goals
    customEndSurah INTEGER,     -- for custom goals
    goalName TEXT,
    totalAyahs INTEGER,
    isActive INTEGER,           -- boolean: 1 = active, 0 = inactive
    createdAt INTEGER,
    completedAt INTEGER,        -- null if not completed
    baselineDate INTEGER,       -- when projection was set
    baselineAyahsLearned INTEGER,
    projectedDaysToComplete INTEGER
)
```

## Usage Flow

1. User opens **MemorizationActivity**
2. Timeline card displays current goal progress
3. User clicks "Settings" button on timeline card
4. **TimelineSettingsActivity** opens
5. User adjusts daily minutes or selects different goal
6. Projection updates live
7. User saves settings
8. Goal is stored/updated in database
9. Timeline card in MemorizationActivity refreshes on resume

## Future Enhancements

Potential additions (not yet implemented):
- Custom goal ranges (specific surah ranges)
- Multiple concurrent goals
- Weekly/monthly velocity charts
- Push notifications for milestones
- Streak tracking tied to timeline
- Historical goal tracking and analytics
- Goal sharing/comparison features

## Technical Notes

- Uses SharedPreferences for quick access to current settings
- Database stores goal entities for history tracking
- Background threads for all calculations to avoid UI blocking
- Live updates when settings change
- Refreshes on activity resume to reflect latest learning activity
