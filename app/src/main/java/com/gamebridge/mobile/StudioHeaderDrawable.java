package com.gamebridge.mobile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Clips and shades the Studio artwork so the header copy stays readable across widths. */
final class StudioHeaderDrawable extends Drawable {
    private static final int BACKGROUND = 0xFF060B11;

    private final Bitmap artwork;
    private final float radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF panel = new RectF();
    private final RectF artworkBounds = new RectF();
    private final Path clip = new Path();
    private Shader shade;
    private Shader border;
    private int alpha = 255;

    StudioHeaderDrawable(Bitmap artwork, float radius) {
        this.artwork = artwork;
        this.radius = radius;
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        panel.set(bounds);
        if (bounds.isEmpty()) return;
        clip.reset();
        clip.addRoundRect(panel, radius, radius, Path.Direction.CW);
        if (artwork != null) {
            float scale = Math.min(panel.height() / artwork.getHeight(), panel.width() / artwork.getWidth());
            float width = artwork.getWidth() * scale;
            float height = artwork.getHeight() * scale;
            float top = panel.centerY() - height / 2f;
            artworkBounds.set(panel.right - width, top, panel.right, top + height);
            shade = new LinearGradient(artworkBounds.left, 0f, artworkBounds.right, 0f,
                    new int[]{BACKGROUND, 0xD9060B11, 0x69060B11, 0x00060B11},
                    new float[]{0f, 0.27f, 0.48f, 0.66f}, Shader.TileMode.CLAMP);
        }
        border = new LinearGradient(panel.left, panel.top, panel.right, panel.bottom,
                0xEE3D5A5D, 0x66476368, Shader.TileMode.CLAMP);
    }

    @Override public void draw(Canvas canvas) {
        if (panel.isEmpty()) return;
        canvas.save();
        canvas.clipPath(clip);
        paint.setColor(BACKGROUND);
        paint.setAlpha(alpha);
        canvas.drawRect(panel, paint);

        if (artwork != null) {
            canvas.drawBitmap(artwork, null, artworkBounds, paint);
            paint.setShader(shade);
            canvas.drawRect(artworkBounds, paint);
        }

        paint.setShader(border);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, radius * 0.10f));
        canvas.drawRoundRect(panel, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
