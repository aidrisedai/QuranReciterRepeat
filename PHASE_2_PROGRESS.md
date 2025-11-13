# Phase 2: Goal Management UI - IN PROGRESS 🚧

## Progress Status: 50% Complete

### ✅ Completed Tasks

#### 1. Goal Input Screen ✅
**File**: `activity_goal_input.xml`

Created beautiful, user-friendly goal input screen with:
- Text input for goal description
- Voice input button (uses Android Speech Recognition)
- Strictness level selector (Lenient/Moderate/Strict)
- Example goals card with common patterns
- Live goal preview card (shows parsed goal)
- Cancel/Create action buttons

**Design Features**:
- Material Design 3 components
- Consistent color scheme with app
- Clear visual hierarchy
- Helpful examples and hints

#### 2. Goal Parser ✅
**File**: `GoalParser.java`

Comprehensive natural language parser that understands:

**Named Sections**:
- "Juz Amma" → Surahs 78-114
- "Last 10 surahs" → Surahs 105-114  
- "Juz 29" → Surahs 67-77

**Surah Names**:
- "Al-Mulk" → Surah 67
- "Al-Kahf" → Surah 18
- "Yasin" → Surah 36
- And 5 more common names

**Verse Ranges**:
- "Surah 2 verses 1-10" → Al-Baqarah 2:1-10
- "2:1-10" → Same as above
- "Surah 67" → Complete Surah Al-Mulk

**Recurring Goals**:
- "5 verses per day" → Daily goal with 5 verses/day
- "2 surahs per week" → Weekly goal (converted to ~0.28 verses/day)
- "10 verses per month" → Monthly goal (converted to ~0.33 verses/day)

**Features**:
- Automatic verse count calculation
- Estimated completion timeline
- Clear error messages
- Goal type classification

#### 3. Goal Input Activity ✅
**File**: `GoalInputActivity.java`

Complete functional activity with:
- Real-time goal parsing as user types
- Voice input integration
- Live preview of parsed goal
- Strictness level selection
- Goal creation with all metadata
- Error handling and validation

**Flow**:
1. User enters goal (text or voice)
2. Parser validates and extracts data
3. Preview shows what was understood
4. User selects strictness level
5. Goal is saved to database
6. Returns to caller with goal ID

### 🚧 Remaining Tasks

#### 3. AI-Assisted Goal Planning (Next)
**Status**: Not Started

Will include:
- Conversational dialog for goal planning
- "I want to memorize Juz Amma" → AI suggests timeline
- Consider user's history (how fast they usually learn)
- Adaptive suggestions based on progress
- Timeline recommendations

**Approach**: Could use simple dialog with predefined options or integrate OpenAI API for conversational planning

#### 4. Goal List and Management (Next)
**Status**: Not Started

Needs:
- Activity to view all goals
- Active/Paused/Completed tabs
- Edit goal screen
- Pause/resume functionality
- Delete goal option
- Progress visualization

## Files Created

### New Files:
1. `GoalParser.java` - Natural language goal parser
2. `activity_goal_input.xml` - Goal input UI layout
3. `GoalInputActivity.java` - Goal creation activity

### Modified Files:
1. `AndroidManifest.xml` - Added GoalInputActivity

## How to Test

### Manual Testing:
```bash
# From any activity, launch goal input:
Intent intent = new Intent(this, GoalInputActivity.class);
startActivityForResult(intent, REQUEST_CODE_GOAL);
```

### Test Cases:
1. **Text Input**:
   - Type "Surah Al-Mulk"
   - Should parse to: Surah 67, 30 verses
   
2. **Voice Input**:
   - Tap voice button
   - Say "5 verses per day"
   - Should parse to: Daily goal, 5 verses/day
   
3. **Complex Goals**:
   - Type "Juz Amma"
   - Should parse to: Surahs 78-114, 564 verses
   
4. **Strictness Selection**:
   - Select different levels
   - Verify they're saved correctly

## Integration Points

### How Other Activities Can Use This:

```java
// Launch goal creation
Intent intent = new Intent(this, GoalInputActivity.class);
startActivityForResult(intent, REQUEST_CODE_GOAL);

// Handle result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_GOAL && resultCode == RESULT_OK) {
        long goalId = data.getLongExtra("goal_id", -1);
        // Use goal ID to start memorization session
    }
}
```

### Next Integration:
Once we complete Phase 3 (Memorization Session), the flow will be:
1. User opens HomeActivity
2. Taps "Memorization Session" card
3. If no active goal → Launch GoalInputActivity
4. If active goal exists → Launch MemorizationSessionActivity with goal

## Database Schema Usage

Goals are saved with:
```java
MemorizationGoalEntity:
- goalText: "Memorize Surah Al-Mulk"
- goalType: "one-time" or "daily"
- targetSurahStart: 67
- targetAyahStart: 1
- targetSurahEnd: 67
- targetAyahEnd: 30
- versesPerDay: null (or 5 for daily goals)
- strictnessLevel: "moderate"
- totalVerses: 30
- currentProgress: 0
- isActive: true
```

## Build Status

✅ **BUILD SUCCESSFUL**

All code compiles without errors. Ready for Phase 2 completion tasks.

## What's Next

### Immediate Next Steps:
1. Create goal list/management UI
2. Add edit goal functionality
3. Integrate goal input into HomeActivity workflow

### Then Move to Phase 3:
1. Create Memorization Session Activity
2. Integrate OpenAI Realtime API with strictness modes
3. Implement error blocking logic
4. Add progress tracking

## Key Design Decisions

1. **Single Active Goal**: Only one goal can be active at a time for simplicity
2. **Strictness at Goal Level**: Each goal has its own strictness preference
3. **Voice + Text**: Support both input methods for accessibility
4. **Real-time Parsing**: Instant feedback as user types
5. **Natural Language**: Flexible parsing of common goal patterns

---

**Phase 2 Status**: 50% Complete (2/4 tasks done)
**Build Status**: ✅ Successful
**Next Priority**: Goal Management UI
