# Insights & Analytics System

## Overview
The insights system provides comprehensive analysis of memorization patterns, consistency, and retention to help users optimize their learning approach.

## Features Implemented

### 1. **Core Metrics Tracking**

#### Average Time Per Aya
- Calculates time spent per aya across all sessions
- Identifies fast learners (<1.5 min/aya) vs methodical learners (>3 min/aya)
- Provides personalized pace feedback

#### Success Rates
- **Session Success Rate**: Success across individual sessions
- **Overall Success Rate**: Cumulative success from all units
- Tracks success/fail counts per unit for retention analysis

#### Retention by Review Level
- Analyzes retention rates at each spaced repetition level (1-6)
- Identifies weak review levels requiring more attention
- Provides visual progress bars for each level

### 2. **Pattern Detection**

#### Time-Based Patterns
- **Session Duration Analysis**: Groups attempts by 5-minute buckets
- **Accuracy vs Duration**: Detects if accuracy drops after certain session lengths
- **Optimal Session Length**: Identifies ideal session duration (typically <20 min)
- **Example Insight**: "Your accuracy drops after 25 minutes — consider shorter sessions"

#### Surah Type Performance
- **Makkan vs Madinan**: Compares success rates by revelation type
- Uses SurahMetadata utility for accurate classification
- **Example Insight**: "You recall Makkan surahs better (85% vs 70%)"
- Provides targeted recommendations for weaker surah types

### 3. **Chunk Size Evolution**
- Tracks min, max, and average chunk sizes used
- Shows progression from 1-5 ayahs over time
- Integrates with adaptive chunk size system
- Visualizes user's comfort zone for memorization units

### 4. **Consistency Tracking**

#### Consistency Score (0-100)
Calculated from three components:
- **Attempts per day** (14 points max): Daily engagement
- **Study time** (42 points max): Total minutes invested (max 210 min/week)
- **Success rate** (43 points max): Quality of memorization

Levels:
- 80-100: Excellent consistency 🔥
- 60-79: Good consistency 👍
- 40-59: Building momentum 📊
- 0-39: Starting journey 🌱

### 5. **Weekly Analytics**
- Sessions completed
- Successful attempts
- Total study time (minutes)
- Consistency score
- Average time per aya

### 6. **Personalized Insights**

The system generates context-aware insights:

- ⚡ **Fast learner!** Averaging 1.2 min per aya
- 🐢 **Taking your time** at 3.5 min per aya - Quality over speed!
- ✅ **Good pace** at 2.0 min per aya
- 📉 Your accuracy **drops after 20 minutes**
- 🕌 You **recall Makkan surahs better** (85% vs 70%)
- 📏 Chunk sizes: **1-4 ayahs** (avg 2.5)
- 🔄 Retention **dips at review level 3** (65%)
- 🔥 **Excellent consistency!** Score: 85/100
- 🎯 **Strong performance** at 82% success rate

### 7. **Actionable Recommendations**

Based on detected patterns:

- "Try keeping sessions **under 20 minutes** for better retention"
- "Allocate **more review time** for Madinan surahs"
- "Focus on reviewing material at **level 3** more frequently"
- "Consider **reducing chunk sizes** or increasing repetitions"
- "Try to study **more consistently** throughout the week"
- "Consider **smaller chunk sizes** if memorization feels overwhelming"

## Database Schema

### insight_summaries Table
```sql
CREATE TABLE insight_summaries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    summaryType TEXT,           -- "weekly", "monthly", "milestone"
    periodStart INTEGER,         -- timestamp
    periodEnd INTEGER,           -- timestamp
    summaryText TEXT,            -- human-readable summary
    metricsSnapshot TEXT,        -- JSON with key metrics
    topInsight1 TEXT,
    topInsight2 TEXT,
    topInsight3 TEXT,
    topRecommendation TEXT,
    createdAt INTEGER,
    isRead INTEGER              -- boolean: 0 = unread, 1 = read
)
```

## Architecture

### InsightsEngine
Core analytics engine that:
1. Queries last 30 days of attempts and units
2. Calculates all metrics
3. Analyzes patterns across multiple dimensions
4. Generates human-readable insights and recommendations

**Key Methods:**
- `generateInsights()`: Main entry point
- `calculateCoreMetrics()`: Time per aya, success rates
- `analyzeRetentionRates()`: Retention by review level
- `analyzeChunkSizeEvolution()`: Chunk size trends
- `analyzeSurahTypePerformance()`: Makkan vs Madinan
- `detectTimePatterns()`: Session duration patterns
- `calculateWeeklyStats()`: Last 7 days metrics
- `generateInsightMessages()`: Human-readable text

### WeeklySummaryGenerator
Automated summary generation:
- Checks if weekly summary needed
- Generates summary from InsightsEngine
- Stores in database for history tracking
- Can be called on app start via `autoGenerateIfNeeded()`

### SurahMetadata
Classification utility:
- 86 Makkan surahs
- 28 Madinan surahs
- Methods: `isMakkan()`, `isMadinan()`, `getRevelationType()`

## Integration Points

### PerformanceDashboardActivity
Displays comprehensive insights including:
- Performance overview (existing)
- **NEW**: Personalized Insights section with:
  - Key metrics
  - Retention by level (visual bars)
  - Surah type performance
  - Pattern insights
  - Actionable recommendations

### Enhanced DAOs

#### MemorizationAttemptDao
New queries:
- `getAttemptsSince(timestamp)`: Attempts after date
- `getAttemptsBetween(start, end)`: Date range
- `getAverageSuccessfulDuration()`: Avg time for successful sessions
- `getAttemptCountSince(timestamp)`: Count after date

#### MemorizationUnitDao
New queries:
- `getUnitsByReviewLevel(level)`: Units at specific review level
- `getLearnedUnitsForSurah(surah)`: Learned units in surah
- `getRecentlyLearnedUnits(limit)`: Recent learned units
- `getAverageSuccessRate()`: Overall success rate
- `getTotalAyahsLearned()`: Total ayahs memorized

## Usage Flow

1. User opens **PerformanceDashboardActivity**
2. InsightsEngine analyzes last 30 days of data
3. Patterns detected across multiple dimensions
4. Insights and recommendations generated
5. Displayed in dedicated insights section
6. Weekly summaries auto-generated and stored

## Example Output

```
═══ Personalized Insights ═══

📊 KEY METRICS:
  • Avg time per aya: 2.1 min
  • Overall success: 78%
  • Chunk size range: 2-4 ayahs
  • Consistency score: 72/100

🔄 RETENTION BY REVIEW LEVEL:
  Level 1: ██████████ 95%
  Level 2: ████████░░ 82%
  Level 3: ██████░░░░ 65%
  Level 4: ████████░░ 80%

🕌 SURAH TYPE PERFORMANCE:
  Makkan: 85% (23 sessions)
  Madinan: 70% (12 sessions)

💡 INSIGHTS:
  ✅ Good pace at 2.1 min per aya
  ⏱️ Average session: 18 minutes
  🕌 You recall Makkan surahs better (85% vs 70%)
  📏 Chunk sizes: 2-4 ayahs (avg 2.8)
  🔄 Retention dips at review level 3 (65%)
  👍 Good consistency. Score: 72/100

✨ RECOMMENDATIONS:
  • Allocate more review time for Madinan surahs
  • Focus on reviewing material at level 3 more frequently
```

## Future Enhancements

Potential additions:
- Time-of-day performance analysis
- Day-of-week patterns
- Long-term trend charts (weekly/monthly graphs)
- Comparison with community averages
- Achievement badges for milestones
- Export insights as PDF/share
- Push notifications for weekly summaries
- Predictive analytics (projected completion times)
- A/B testing recommendations

## Technical Notes

- All calculations run on background threads
- Minimum data requirements: ~5-10 sessions for pattern detection
- Graceful degradation with limited data
- Efficient queries with indexed timestamps
- Room database version 6 with InsightSummary entity
- Monospace font for consistent formatting
- Progress bars use Unicode block characters (█░)
