# Reciter Selection Order Fix

## Issue
Users reported that reciter selection was no longer respecting their chosen order and was instead being sorted alphabetically during playback.

## Root Cause
In `MainActivity.java`, the `showTabbedReciterPicker()` method was:
1. Sorting reciters alphabetically for display (correct)
2. But then saving selections by iterating through the sorted array (wrong)
3. This resulted in saving reciters in alphabetical order, not selection order

## Solution
Implemented Option 1A: **Numbered selection with preserved order**

### How It Works
1. **Load existing order**: Preserves user's current reciter order from preferences
2. **Display alphabetically**: Shows reciters sorted A-Z for easy finding
3. **Track selection order**: Maintains a separate list (`selectionOrder`) that tracks the order users select
4. **Show numbers dynamically**: Selected reciters show their playback position: `[1] Abdul Basit`, `[2] Mishary Rashid`
5. **Real-time updates**: Numbers update immediately as users check/uncheck reciters
6. **Save order**: Saves the `selectionOrder` list (not alphabetical iteration)

### User Experience
```
Before selecting:
☐ Abdul Basit
☐ Ahmed Al-Ajmi
☐ Mishary Rashid
☐ Sudais

After selecting Mishary, then Abdul Basit, then Sudais:
☑ [2] Abdul Basit      ← Selected second, plays second
☐ Ahmed Al-Ajmi
☑ [1] Mishary Rashid   ← Selected first, plays first
☑ [3] Sudais           ← Selected third, plays third

After unchecking Abdul Basit:
☐ Abdul Basit
☐ Ahmed Al-Ajmi
☑ [1] Mishary Rashid   ← Renumbered to #1
☑ [2] Sudais           ← Renumbered to #2
```

## Code Changes
**File:** `app/src/main/java/com/repeatquran/MainActivity.java`  
**Method:** `showTabbedReciterPicker()` (lines 763-821)

### Key Implementation Details
- Uses `selectionOrder` list to track user's intended order
- When checked: adds reciter ID to end of `selectionOrder`
- When unchecked: removes reciter ID from `selectionOrder`
- Display items updated on every click with current position numbers
- `notifyDataSetChanged()` refreshes the ListView to show updated numbers
- Final order saved as CSV to `reciters.order` preference

## Testing
✅ Build successful  
⚠️ Manual testing recommended:
1. Open reciter picker
2. Select reciters in specific order (e.g., 3rd, 1st, 4th alphabetically)
3. Verify numbers appear correctly
4. Start playback
5. Verify reciters play in selected order (not alphabetical)

## Benefits
- ✅ Preserves user's intended playback order
- ✅ Clear visual feedback with numbers
- ✅ Existing selections maintain their order
- ✅ New selections added in order of selection
- ✅ Real-time number updates on each click
- ✅ Minimal code (compact implementation)
- ✅ Human-readable code structure
