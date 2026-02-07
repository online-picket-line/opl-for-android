#!/bin/bash
# Generate Android app icons from opl.svg
# Uses ImageMagick to convert SVG to PNG at all required mipmap densities

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SVG_PATH="$SCRIPT_DIR/opl.svg"
RES_DIR="$SCRIPT_DIR/app/src/main/res"

if [ ! -f "$SVG_PATH" ]; then
    echo "Error: opl.svg not found at $SVG_PATH"
    exit 1
fi

echo "Generating Android app icons from opl.svg..."

# Android adaptive icon sizes per density bucket
# mdpi: 48x48 (launcher), 108x108 (adaptive layers)
# hdpi: 72x72 (launcher), 162x162 (adaptive layers)
# xhdpi: 96x96 (launcher), 216x216 (adaptive layers)
# xxhdpi: 144x144 (launcher), 324x324 (adaptive layers)
# xxxhdpi: 192x192 (launcher), 432x432 (adaptive layers)

declare -A LAUNCHER_SIZES=(
    ["mipmap-mdpi"]=48
    ["mipmap-hdpi"]=72
    ["mipmap-xhdpi"]=96
    ["mipmap-xxhdpi"]=144
    ["mipmap-xxxhdpi"]=192
)

declare -A ADAPTIVE_SIZES=(
    ["mipmap-mdpi"]=108
    ["mipmap-hdpi"]=162
    ["mipmap-xhdpi"]=216
    ["mipmap-xxhdpi"]=324
    ["mipmap-xxxhdpi"]=432
)

# Generate standard launcher icons (square with rounded corners handled by OS)
for density in "${!LAUNCHER_SIZES[@]}"; do
    size=${LAUNCHER_SIZES[$density]}
    dir="$RES_DIR/$density"
    mkdir -p "$dir"

    # Render centered on square canvas
    convert -background "#f0f4f8" -density 300 "$SVG_PATH" \
        -resize "$((size * 80 / 100))x${size}" \
        -gravity center \
        -extent "${size}x${size}" \
        -flatten \
        "$dir/ic_launcher.png"

    # Round version (same content, OS applies mask)
    cp "$dir/ic_launcher.png" "$dir/ic_launcher_round.png"

    echo "  Generated $density: ${size}x${size}"
done

# Generate adaptive icon foreground layers (108dp base, with 18dp safe zone padding)
for density in "${!ADAPTIVE_SIZES[@]}"; do
    size=${ADAPTIVE_SIZES[$density]}
    dir="$RES_DIR/$density"
    mkdir -p "$dir"

    # The opal centered with padding for the safe zone (inner 66dp of 108dp)
    # Scale opal to fit in ~60% of the canvas (safe zone)
    opal_size=$((size * 60 / 100))
    convert -background none -density 300 "$SVG_PATH" \
        -resize "${opal_size}x${opal_size}" \
        -gravity center \
        -extent "${size}x${size}" \
        "$dir/ic_launcher_foreground.png"

    echo "  Generated $density adaptive foreground: ${size}x${size}"
done

# Generate adaptive icon background (solid color matching opal background)
for density in "${!ADAPTIVE_SIZES[@]}"; do
    size=${ADAPTIVE_SIZES[$density]}
    dir="$RES_DIR/$density"

    convert -size "${size}x${size}" "xc:#f0f4f8" "$dir/ic_launcher_background.png"

    echo "  Generated $density adaptive background: ${size}x${size}"
done

# Update adaptive icon XML to use PNG bitmaps instead of vector drawables
cat > "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
EOF

cat > "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
EOF

# Also generate a notification icon (white silhouette, 24dp base)
declare -A NOTIF_SIZES=(
    ["drawable-mdpi"]=24
    ["drawable-hdpi"]=36
    ["drawable-xhdpi"]=48
    ["drawable-xxhdpi"]=72
    ["drawable-xxxhdpi"]=96
)

for density in "${!NOTIF_SIZES[@]}"; do
    size=${NOTIF_SIZES[$density]}
    dir="$RES_DIR/$density"
    mkdir -p "$dir"

    # White silhouette of the opal for notification
    convert -background none -density 300 "$SVG_PATH" \
        -resize "${size}x${size}" \
        -gravity center \
        -extent "${size}x${size}" \
        -fill white -colorize 100 \
        "$dir/ic_notification.png"

    echo "  Generated $density notification icon: ${size}x${size}"
done

echo ""
echo "Done! Android app icons generated from opl.svg"
echo "  - Launcher icons in mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/"
echo "  - Adaptive icons (foreground + background) for API 26+"
echo "  - Notification icons in drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/"
