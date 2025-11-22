# Bengali Fonts for Porakhela

This directory should contain Bengali fonts for proper text rendering in the Porakhela app.

## Required Fonts

### Noto Sans Bengali
Download from Google Fonts: https://fonts.google.com/noto/specimen/Noto+Sans+Bengali

Required files:
- `noto_sans_bengali_regular.ttf` (Weight: 400)
- `noto_sans_bengali_medium.ttf` (Weight: 500) 
- `noto_sans_bengali_semibold.ttf` (Weight: 600)
- `noto_sans_bengali_bold.ttf` (Weight: 700)

### Kalpurush (Optional)
Download from: http://www.omicronlab.com/kalpurush-download.html

Required files:
- `kalpurush_regular.ttf` (Weight: 400)
- `kalpurush_bold.ttf` (Weight: 700)

## Installation

1. Download the font files
2. Rename them according to the naming convention above
3. Place them in this `/res/font/` directory
4. The font families are already configured in `noto_sans_bengali.xml` and `kalpurush.xml`

## Usage

```kotlin
// In Compose
Text(
    text = "বাংলা টেক্সট",
    fontFamily = BengaliFonts.NotoSansBengali
)
```

## Accessibility

These fonts are specifically chosen for:
- Clear readability for children
- Proper Bengali character rendering
- Good contrast and spacing
- Support for all Bengali Unicode characters