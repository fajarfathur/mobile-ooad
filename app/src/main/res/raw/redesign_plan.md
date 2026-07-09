# SIMO — Redesign Total v5

## Masalah yang Akan Diperbaiki
1. Warna: ganti dari navy tua → **Indigo/Violet modern** (Linear, Notion style)
2. Icon: semua ganti ke Material Symbols Rounded paths yang bersih
3. Dashboard: bukan card kotak biasa → fluid card dengan gradient teks + chart interaktif
4. Chart trend: custom View yang bisa di-tap, show detail bottomsheet, animasi smooth
5. Profile: photo picker dengan overlay edit button
6. Forms: semua di-update dengan style baru yang konsisten
7. Animasi: shimmer loading, gradient shader animasi di hero

## Palette Baru (Modern/Kekinian)
- Background:   #F5F7FF (indigo 50 — sangat cerah)
- Primary:      #4F46E5 (Indigo 600)
- Primary Dark: #3730A3 (Indigo 800)
- Gradient:     #4F46E5 → #7C3AED (Indigo → Violet)
- Green:        #10B981 (Emerald)
- Amber:        #F59E0B
- Red:          #EF4444
- Blue:         #3B82F6
- Text:         #111827 / #6B7280

## Komponen yang Diubah
- colors.xml — palette total baru
- themes.xml — font, corner, style konsisten  
- ic_*.xml — semua icon Material Symbols baru
- fragment_dashboard.xml — hero fluid, stats chips, interactive chart
- QuarterlyChartView.kt — custom View canvas chart
- ChartDetailBottomSheet.kt — detail saat bar di-tap
- fragment_lainnya.xml — profile dengan edit photo
- LainnyaFragment.kt — image picker intent
- activity_login.xml — login redesign indigo
