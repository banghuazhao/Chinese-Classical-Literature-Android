package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;

/**
 * One continuous plank behind a horizontal shelf of covers.
 * Drawn on the RecyclerView so it cannot be hidden by CardView elevation.
 */
public final class ShelfBoardDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint = new Paint();

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int width = parent.getWidth();
        int height = parent.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Context context = parent.getContext();
        Resources res = context.getResources();
        int coverHeight = res.getDimensionPixelSize(R.dimen.cover_height);
        int overlap = res.getDimensionPixelSize(R.dimen.shelf_overlap);
        int lip = res.getDimensionPixelSize(R.dimen.shelf_lip_height);
        int groove = res.getDimensionPixelSize(R.dimen.shelf_groove_height);
        int face = res.getDimensionPixelSize(R.dimen.shelf_face_height);
        int under = res.getDimensionPixelSize(R.dimen.shelf_under_height);

        int left = 0;
        int right = width;
        int y = parent.getPaddingTop() + coverHeight - overlap;

        paint.setShader(null);
        paint.setColor(MyColor.getShelfLipColor(context));
        canvas.drawRect(left, y, right, y + lip, paint);
        y += lip;

        paint.setColor(MyColor.getShelfGrooveColor(context));
        canvas.drawRect(left, y, right, y + groove, paint);
        y += groove;

        LinearGradient gradient = new LinearGradient(
                0, y, 0, y + face,
                MyColor.getShelfFaceHighlightColor(context),
                MyColor.getShelfFaceShadowColor(context),
                Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRect(left, y, right, y + face, paint);
        paint.setShader(null);
        y += face;

        paint.setColor(MyColor.getShelfUnderColor(context));
        canvas.drawRect(left, y, right, y + under, paint);
    }
}
