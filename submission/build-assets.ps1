$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# Store asset refresh for Clockwise 1.0.10.
#
# Inputs:
#   app/build/screenshots/*.png          - Roborazzi renders (run :app:testDebugUnitTest first)
#   core/ui/res/drawable-nodpi/widget_preview.png - the app's generated widget preview
#   app/build/outputs/apk/release/app-release.apk - the signed release build
#
# Outputs (all written next to this file):
#   clockwise-screenshot-{clocks,map,themes}.jpg  640x1280 (Aptoide's 2:1 limit)
#   clockwise-screenshot-widget.jpg               640x1280, from the app's widget preview
#   Clockwise-v1.0.10.apk                         signed release copy

$root = Split-Path -Parent $PSScriptRoot
$out = $PSScriptRoot

function Save-JpegHighQuality {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path,
        [int]$Quality = 95
    )
    $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
        Where-Object { $_.MimeType -eq 'image/jpeg' }
    $params = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter(
        [System.Drawing.Imaging.Encoder]::Quality, [long]$Quality)
    $Bitmap.Save($Path, $codec, $params)
    $params.Dispose()
}

# 1) Screen screenshots: pad the 720x1600 Roborazzi renders onto a 640x1280
#    canvas (exactly 2:1) using the render's own edge colour.
$shots = @(
    @{ Src = 'clocks_screen_360dp.png';  Dst = 'clockwise-screenshot-clocks.jpg' },
    @{ Src = 'map_screen_360dp.png';     Dst = 'clockwise-screenshot-map.jpg' },
    @{ Src = 'settings_sheet_360dp.png'; Dst = 'clockwise-screenshot-themes.jpg' }
)
foreach ($shot in $shots) {
    $src = Join-Path $root ('app\build\screenshots\' + $shot.Src)
    $img = New-Object System.Drawing.Bitmap($src)
    $edge = $img.GetPixel(1, 1)
    $canvas = New-Object System.Drawing.Bitmap(640, 1280)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.Clear($edge)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $drawW = [int]($img.Width * 1280.0 / $img.Height)
    $g.DrawImage($img,
        (New-Object System.Drawing.Rectangle([int]((640 - $drawW) / 2), 0, $drawW, 1280)),
        (New-Object System.Drawing.Rectangle(0, 0, $img.Width, $img.Height)),
        [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $img.Dispose()
    Save-JpegHighQuality -Bitmap $canvas -Path (Join-Path $out $shot.Dst)
    $canvas.Dispose()
}

# 2) Widget screenshot: the app's own generated widget preview (500x500)
#    centred on its card background colour.
$widget = New-Object System.Drawing.Bitmap(
    (Join-Path $root 'core\ui\src\main\res\drawable-nodpi\widget_preview.png'))
$bg = $widget.GetPixel(250, 250)
$canvas = New-Object System.Drawing.Bitmap(640, 1280)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear($bg)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$drawW = 640 - 2 * 24
$g.DrawImage($widget,
    (New-Object System.Drawing.Rectangle(24, [int]((1280 - $drawW) / 2), $drawW, $drawW)),
    (New-Object System.Drawing.Rectangle(0, 0, $widget.Width, $widget.Height)),
    [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose(); $widget.Dispose()
Save-JpegHighQuality -Bitmap $canvas -Path (Join-Path $out 'clockwise-screenshot-widget.jpg')
$canvas.Dispose()

# 3) Signed release APK copy with the store-facing file name
Copy-Item -Path (Join-Path $root 'app\build\outputs\apk\release\app-release.apk') `
    -Destination (Join-Path $out 'Clockwise-v1.0.10.apk') -Force

Get-ChildItem $out -File | Select-Object Name, Length | Format-Table -AutoSize
