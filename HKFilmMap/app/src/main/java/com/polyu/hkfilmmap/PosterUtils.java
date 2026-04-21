package com.polyu.hkfilmmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PosterUtils {
    private static final Map<String, BitmapDescriptor> MARKER_CACHE = new HashMap<>();

    private PosterUtils() {
    }

    public static int getPosterRes(Context context, String posterAsset) {
        if (posterAsset == null || posterAsset.trim().isEmpty()) {
            return R.drawable.poster_placeholder;
        }
        int dynamic = context.getResources().getIdentifier(posterAsset, "drawable", context.getPackageName());
        if (dynamic != 0) {
            return dynamic;
        }
        switch (posterAsset) {
            case "poster_tomorrow":
                return R.drawable.poster_tomorrow;
            case "poster_cestlavie":
                return R.drawable.poster_cestlavie;
            case "poster_chungking":
                return R.drawable.poster_chungking;
            case "poster_comrades":
                return R.drawable.poster_comrades;
            case "poster_strange":
                return R.drawable.poster_strange;
            case "poster_dragon":
                return R.drawable.poster_dragon;
            case "poster_ghost":
                return R.drawable.poster_ghost;
            case "poster_infernal":
                return R.drawable.poster_infernal;
            case "poster_police":
                return R.drawable.poster_police;
            case "poster_rouge":
                return R.drawable.poster_rouge;
            case "poster_rush":
                return R.drawable.poster_rush;
            case "poster_darkknight":
                return R.drawable.poster_darkknight;
            case "poster_golden":
                return R.drawable.poster_golden;
            case "poster_suzie":
                return R.drawable.poster_suzie;
            case "poster_transformers":
                return R.drawable.poster_transformers;
            default:
                return R.drawable.poster_placeholder;
        }
    }

    public static BitmapDescriptor getPosterMarkerIcon(Context context, String posterAsset) {
        String key = "single:" + normalizeAssetKey(posterAsset);
        if (MARKER_CACHE.containsKey(key)) {
            return MARKER_CACHE.get(key);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(createSinglePosterBitmap(context, posterAsset));
        MARKER_CACHE.put(key, descriptor);
        return descriptor;
    }

    public static BitmapDescriptor getPosterStackMarkerIcon(Context context, List<String> posterAssets, int totalCount) {
        if (posterAssets == null || posterAssets.isEmpty()) {
            return getPosterMarkerIcon(context, null);
        }
        StringBuilder keyBuilder = new StringBuilder("stack:");
        int displayCount = Math.min(3, posterAssets.size());
        for (int i = 0; i < displayCount; i++) {
            keyBuilder.append(normalizeAssetKey(posterAssets.get(i))).append('|');
        }
        keyBuilder.append(totalCount);
        String key = keyBuilder.toString();
        if (MARKER_CACHE.containsKey(key)) {
            return MARKER_CACHE.get(key);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(createPosterStackBitmap(context, posterAssets, totalCount));
        MARKER_CACHE.put(key, descriptor);
        return descriptor;
    }

    private static Bitmap createSinglePosterBitmap(Context context, String posterAsset) {
        Bitmap source = BitmapFactory.decodeResource(context.getResources(), getPosterRes(context, posterAsset));
        if (source == null) {
            source = Bitmap.createBitmap(dpToPx(context, 42), dpToPx(context, 58), Bitmap.Config.ARGB_8888);
        }

        int posterWidth = dpToPx(context, 42);
        int posterHeight = dpToPx(context, 58);
        int frame = dpToPx(context, 3);
        int pointerHeight = dpToPx(context, 10);
        int width = posterWidth + frame * 2;
        int height = posterHeight + frame * 2 + pointerHeight;
        float corner = dpToPx(context, 8);

        Bitmap scaled = Bitmap.createScaledBitmap(source, posterWidth, posterHeight, true);
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        drawCardShadow(canvas, 2f, 2f, width - 2f, posterHeight + frame * 2 + 2f, corner);
        drawPosterCard(canvas, scaled, 0f, 0f, posterWidth, posterHeight, frame, corner);
        drawPointer(canvas, width / 2f, posterHeight + frame * 2 - 1f, dpToPx(context, 6), height);
        return output;
    }

    private static Bitmap createPosterStackBitmap(Context context, List<String> posterAssets, int totalCount) {
        int displayCount = Math.min(3, posterAssets.size());
        int posterWidth = dpToPx(context, 32);
        int posterHeight = dpToPx(context, 46);
        int frame = dpToPx(context, 3);
        int overlap = dpToPx(context, 14);
        int lift = dpToPx(context, 4);
        int pointerHeight = dpToPx(context, 10);
        int width = posterWidth + frame * 2 + overlap * (displayCount - 1);
        int height = posterHeight + frame * 2 + lift * (displayCount - 1) + pointerHeight;
        float corner = dpToPx(context, 7);

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        for (int index = displayCount - 1; index >= 0; index--) {
            String asset = posterAssets.get(index);
            Bitmap source = BitmapFactory.decodeResource(context.getResources(), getPosterRes(context, asset));
            if (source == null) {
                continue;
            }
            Bitmap scaled = Bitmap.createScaledBitmap(source, posterWidth, posterHeight, true);
            float left = overlap * index;
            float top = lift * (displayCount - 1 - index);
            drawCardShadow(canvas, left + 2f, top + 2f, left + posterWidth + frame * 2 - 2f, top + posterHeight + frame * 2 + 2f, corner);
            drawPosterCard(canvas, scaled, left, top, posterWidth, posterHeight, frame, corner);
        }

        float pointerCenterX = width / 2f;
        float pointerTopY = posterHeight + frame * 2 + lift * (displayCount - 1) - 1f;
        drawPointer(canvas, pointerCenterX, pointerTopY, dpToPx(context, 6), height);
        drawCountBadge(context, canvas, width - dpToPx(context, 10), dpToPx(context, 12), totalCount);
        return output;
    }

    private static void drawPosterCard(Canvas canvas, Bitmap poster, float left, float top, int posterWidth, int posterHeight, int frame, float corner) {
        Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setColor(Color.WHITE);
        RectF frameRect = new RectF(left, top, left + posterWidth + frame * 2, top + posterHeight + frame * 2);
        canvas.drawRoundRect(frameRect, corner, corner, framePaint);

        RectF imageRect = new RectF(left + frame, top + frame, left + frame + posterWidth, top + frame + posterHeight);
        Path clipPath = new Path();
        clipPath.addRoundRect(imageRect, corner - frame, corner - frame, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        canvas.drawBitmap(poster, left + frame, top + frame, null);
        canvas.restore();

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(0x22FFFFFF);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1f);
        canvas.drawRoundRect(imageRect, corner - frame, corner - frame, strokePaint);
    }

    private static void drawCardShadow(Canvas canvas, float left, float top, float right, float bottom, float corner) {
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x33000000);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), corner, corner, shadowPaint);
    }

    private static void drawPointer(Canvas canvas, float centerX, float topY, float halfWidth, float bottomY) {
        Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointerPaint.setColor(Color.WHITE);
        Path pointer = new Path();
        pointer.moveTo(centerX - halfWidth, topY);
        pointer.lineTo(centerX, bottomY);
        pointer.lineTo(centerX + halfWidth, topY);
        pointer.close();
        canvas.drawPath(pointer, pointerPaint);
    }

    private static void drawCountBadge(Context context, Canvas canvas, float centerX, float centerY, int totalCount) {
        if (totalCount <= 1) {
            return;
        }
        float radius = dpToPx(context, 10);
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(0xFF172026);
        canvas.drawCircle(centerX, centerY, radius, badgePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dpToPx(context, 9));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(String.valueOf(totalCount), centerX, baseline, textPaint);
    }

    private static String normalizeAssetKey(String posterAsset) {
        return posterAsset == null || posterAsset.trim().isEmpty() ? "poster_placeholder" : posterAsset.trim();
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        ));
    }
}