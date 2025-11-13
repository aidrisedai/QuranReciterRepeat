# Outfit Font Setup Instructions

The new design uses the Outfit font family for a modern, friendly appearance similar to language learning apps.

## How to Add Outfit Font

1. **Download Outfit Font from Google Fonts:**
   - Visit: https://fonts.google.com/specimen/Outfit
   - Click "Download family" button
   - Extract the downloaded ZIP file

2. **Add Font Files to Project:**
   - From the extracted folder, copy these files to `app/src/main/res/font/`:
     - `Outfit-Regular.ttf` → rename to `outfit_regular.ttf`
     - `Outfit-Medium.ttf` → rename to `outfit_medium.ttf`
     - `Outfit-SemiBold.ttf` → rename to `outfit_semibold.ttf`
     - `Outfit-Bold.ttf` → rename to `outfit_bold.ttf`
   
   Note: Android requires lowercase font file names with underscores.

3. **The font family XML is already configured** at `app/src/main/res/font/outfit.xml`

## Alternative: Use System Fonts

If you prefer to use system fonts temporarily or permanently:
- The current design uses `sans-serif-medium` which works well
- You can update `fontFamily` attributes in the layout files to use other system fonts like:
  - `sans-serif`
  - `sans-serif-light`
  - `sans-serif-medium`
  - `sans-serif-condensed`

## After Adding Fonts

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. Update layout files to use the Outfit font:
   - Replace `android:fontFamily="sans-serif-medium"` 
   - With `android:fontFamily="@font/outfit"`
