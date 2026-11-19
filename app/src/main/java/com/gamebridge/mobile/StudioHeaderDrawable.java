package com.gamebridge.mobile;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Draws the workspace header without a bitmap decode or a live blur effect. */
final class StudioHeaderDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final Path clip = new Path();
    private final Path top = polygon(8, 31, 48, 9, 89, 31, 48, 54);
    private final Path left = polygon(8, 31, 48, 54, 48, 100, 8, 77);
    private final Path right = polygon(48, 54, 89, 31, 89, 77, 48, 100);
    private Shader background;
    private Shader light;
    private float radius;
    private int opacity = 255;
    private ColorFilter colorFilter;

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        panel.set(bounds);
        if (bounds.isEmpty()) return;
        radius = Math.min(bounds.height(), bounds.width()) * 0.15f;
        clip.reset();
        clip.addRoundRect(panel, radius, radius, Path.Direction.CW);
        background = new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                0xFF142032, 0xFF102524, Shader.TileMode.CLAMP);
        light = new RadialGradient(bounds.right, bounds.top + bounds.height() * 0.4f,
                bounds.width() * 0.63f, 0x70469683, 0x00469683, Shader.TileMode.CLAMP);
    }

    @Override public void draw(Canvas canvas) {
        if (panel.isEmpty()) return;
        canvas.save();
        canvas.clipPath(clip);
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(opacity);
        paint.setColorFilter(colorFilter);
        paint.setShader(background);
        canvas.drawRect(panel, paint);
        paint.setShader(light);
        canvas.drawRect(panel, paint);
        paint.setShader(null);

        float scale = Math.min(panel.width() * 0.29f, panel.height() * 0.72f) / 105f;
        canvas.save();
        canvas.translate(panel.right - 115f * scale, panel.top + 17f * scale);
        canvas.scale(scale, scale);
        tint(0x285CE5CC);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.9f);
        canvas.drawCircle(49, 54, 53, paint);
        canvas.drawCircle(49, 54, 66, paint);
        paint.setStyle(Paint.Style.FILL);
        drawFace(canvas, left, 0x4929A890);
        drawFace(canvas, right, 0x6C2A806E);
        drawFace(canvas, top, 0x8264CFB1);
        tint(0xA2BFFFE6);
        paint.setStrokeWidth(1.2f);
        canvas.drawLine(30, 19, 70, 42, paint);
        canvas.drawCircle(100, 13, 2.3f, paint);
        canvas.drawCircle(5, 93, 1.5f, paint);
        canvas.restore();

        tint(0x4C9CCBCB);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, radius / 24f));
        canvas.drawRoundRect(panel, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private void drawFace(Canvas canvas, Path face, int color) {
        tint(color);
        canvas.drawPath(face, paint);
        tint(0x89B7FCE8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        canvas.drawPath(face, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void tint(int color) {
        paint.setColor(color);
        paint.setAlpha((color >>> 24) * opacity / 255);
    }

    private static Path polygon(float x1, float y1, float x2, float y2,
                                float x3, float y3, float x4, float y4) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        path.close();
        return path;
    }

    @Override public void setAlpha(int alpha) { opacity = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { colorFilter = filter; invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
