package com.example.tleilax.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.EntityType;
import com.example.tleilax.simulation.PlantType;
import com.example.tleilax.simulation.TreeLifeStage;
import com.example.tleilax.simulation.TreeVariant;
import com.example.tleilax.simulation.WorldSnapshot;

public class SimulationCanvasView extends View {

    private static final int STARTUP_VISIBLE_CELLS = 14;
    private static final int MIN_VISIBLE_CELLS = 64;
    private static final int MAX_VISIBLE_CELLS = 12;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final TextureLibrary textureLibrary;
    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;

    @Nullable
    private WorldSnapshot worldSnapshot;
    @Nullable
    private EntityType selectedEntityType;
    @Nullable
    private OnTileTapListener onTileTapListener;

    private float scaleFactor = 1f;
    private float offsetX;
    private float offsetY;
    private boolean dragging;
    private boolean viewportInitialized;
    private boolean gridVisible = true;

    public SimulationCanvasView(Context context) {
        this(context, null);
    }

    public SimulationCanvasView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimulationCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textureLibrary = new TextureLibrary(context);
        gridPaint.setColor(0x553C3836);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(getResources().getDisplayMetrics().density);
        selectionPaint.setColor(0xFFD79921);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(2.5f * getResources().getDisplayMetrics().density);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        setClickable(true);
    }

    public void setWorldSnapshot(@NonNull WorldSnapshot worldSnapshot) {
        this.worldSnapshot = worldSnapshot;
        initializeViewportIfNeeded();
        clampOffsets();
        invalidate();
    }

    public void setSelectedEntityType(@Nullable EntityType selectedEntityType) {
        this.selectedEntityType = selectedEntityType;
        invalidate();
    }

    public void setOnTileTapListener(@Nullable OnTileTapListener onTileTapListener) {
        this.onTileTapListener = onTileTapListener;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF1A1918);
        if (worldSnapshot == null || worldSnapshot.width() <= 0 || worldSnapshot.height() <= 0) {
            return;
        }

        float baseCellWidth = getWidth() / (float) worldSnapshot.width();
        float baseCellHeight = getHeight() / (float) worldSnapshot.height();
        float cellWidth = baseCellWidth * scaleFactor;
        float cellHeight = baseCellHeight * scaleFactor;

        int visibleStartX = Math.max(0, (int) Math.floor((-offsetX) / cellWidth));
        int visibleStartY = Math.max(0, (int) Math.floor((-offsetY) / cellHeight));
        int visibleEndX = Math.min(worldSnapshot.width() - 1, (int) Math.ceil((getWidth() - offsetX) / cellWidth));
        int visibleEndY = Math.min(worldSnapshot.height() - 1, (int) Math.ceil((getHeight() - offsetY) / cellHeight));

        for (int y = visibleStartY; y <= visibleEndY; y++) {
            for (int x = visibleStartX; x <= visibleEndX; x++) {
                float left = offsetX + x * cellWidth;
                float top = offsetY + y * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;
                drawRect.set(left, top, right, bottom);
                drawBitmap(canvas, textureLibrary.getTerrainTexture(worldSnapshot.defaultTerrainType()), drawRect);
            }
        }

        for (WorldSnapshot.CellSnapshot cell : worldSnapshot.cells()) {
            float left = offsetX + cell.x() * cellWidth;
            float top = offsetY + cell.y() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            if (right < 0 || bottom < 0 || left > getWidth() || top > getHeight()) {
                continue;
            }

            drawRect.set(left, top, right, bottom);
            drawBitmap(canvas, textureLibrary.getTerrainTexture(cell.terrainType()), drawRect);

            if (cell.grassAmount() > 0) {
                drawBitmap(canvas, textureLibrary.getGrassTexture(), drawRect);
            }
        }

        for (WorldSnapshot.CellSnapshot cell : worldSnapshot.cells()) {
            float left = offsetX + cell.x() * cellWidth;
            float top = offsetY + cell.y() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            if (right < 0 || bottom < 0 || left > getWidth() || top > getHeight()) {
                continue;
            }

            drawRect.set(left, top, right, bottom);

            if (cell.animal() != null && !shouldDrawAnimalAfterPlants(cell)) {
                drawBitmap(canvas, textureLibrary.getAnimalTexture(cell.animal().type()), inset(drawRect, 0.18f));
            }
        }

        for (WorldSnapshot.CellSnapshot cell : worldSnapshot.cells()) {
            float left = offsetX + cell.x() * cellWidth;
            float top = offsetY + cell.y() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            if (right < 0 || bottom < 0 || left > getWidth() || top > getHeight()) {
                continue;
            }

            drawRect.set(left, top, right, bottom);

            if (cell.plant() != null) {
                if (cell.plant().plantType() == PlantType.BERRY_BUSH) {
                    drawBitmap(canvas, textureLibrary.getBushTexture(cell.plant().dead()), inset(drawRect, 0.18f));
                } else {
                    TreeVariant variant = resolveTreeRenderVariant(cell);
                    drawBitmap(canvas, textureLibrary.getTreeTexture(variant, cell.plant().dead()),
                            treeBounds(drawRect, variant));
                }
            }
        }

        for (WorldSnapshot.CellSnapshot cell : worldSnapshot.cells()) {
            float left = offsetX + cell.x() * cellWidth;
            float top = offsetY + cell.y() * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            if (right < 0 || bottom < 0 || left > getWidth() || top > getHeight()) {
                continue;
            }

            drawRect.set(left, top, right, bottom);

            if (cell.animal() != null && shouldDrawAnimalAfterPlants(cell)) {
                drawBitmap(canvas, textureLibrary.getAnimalTexture(cell.animal().type()), inset(drawRect, 0.18f));
            }
        }

        if (gridVisible) {
            for (int y = visibleStartY; y <= visibleEndY; y++) {
                for (int x = visibleStartX; x <= visibleEndX; x++) {
                    float left = offsetX + x * cellWidth;
                    float top = offsetY + y * cellHeight;
                    float right = left + cellWidth;
                    float bottom = top + cellHeight;
                    drawRect.set(left, top, right, bottom);
                    canvas.drawRect(drawRect, gridPaint);
                }
            }
        }

        if (selectedEntityType != null) {
            selectionPaint.setColor(selectedEntityType.getRenderColor());
            canvas.drawRect(0, 0, getWidth(), getHeight(), selectionPaint);
        }
    }

    private boolean shouldDrawAnimalAfterPlants(@NonNull WorldSnapshot.CellSnapshot cell) {
        return cell.animal() != null
                && cell.plant() != null
                && cell.plant().plantType() == PlantType.TREE;
    }

    @NonNull
    private TreeVariant resolveTreeRenderVariant(@NonNull WorldSnapshot.CellSnapshot cell) {
        if (cell.plant() == null || cell.plant().plantType() != PlantType.TREE) {
            return TreeVariant.MEDIUM;
        }
        TreeLifeStage stage = cell.plant().treeLifeStage();
        if (stage == null) {
            return cell.plant().treeVariant() != null ? cell.plant().treeVariant() : TreeVariant.MEDIUM;
        }
        return switch (stage) {
            case SAPLING -> TreeVariant.LOW;
            case MATURE -> TreeVariant.MEDIUM;
            case OLD, DEAD -> TreeVariant.TALL;
        };
    }

    @NonNull
    private RectF treeBounds(@NonNull RectF tileBounds, @NonNull TreeVariant variant) {
        float widthFactor = switch (variant) {
            case LOW -> 1.00f;
            case MEDIUM -> 1.45f;
            case TALL -> 1.70f;
        };
        float heightFactor = switch (variant) {
            case LOW -> 1.25f;
            case MEDIUM -> 2.00f;
            case TALL -> 2.75f;
        };
        float width = tileBounds.width() * widthFactor;
        float height = tileBounds.height() * heightFactor;
        float centerX = tileBounds.centerX();
        float bottom = tileBounds.bottom + tileBounds.height() * 0.08f;
        return new RectF(centerX - width / 2f, bottom - height, centerX + width / 2f, bottom);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_UP && !dragging && worldSnapshot != null) {
            int gridX = screenToGridX(event.getX());
            int gridY = screenToGridY(event.getY());
            if (gridX >= 0 && gridY >= 0 && onTileTapListener != null) {
                onTileTapListener.onTileTapped(gridX, gridY);
            }
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            dragging = false;
        }
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    private void drawBitmap(@NonNull Canvas canvas, @NonNull Bitmap bitmap, @NonNull RectF rect) {
        canvas.drawBitmap(bitmap, null, rect, null);
    }

    @NonNull
    private RectF inset(@NonNull RectF source, float ratio) {
        float insetX = source.width() * ratio;
        float insetY = source.height() * ratio;
        return new RectF(source.left + insetX, source.top + insetY, source.right - insetX, source.bottom - insetY);
    }

    private int screenToGridX(float x) {
        float cellWidth = (getWidth() / (float) worldSnapshot.width()) * scaleFactor;
        int gridX = (int) ((x - offsetX) / cellWidth);
        return gridX >= 0 && gridX < worldSnapshot.width() ? gridX : -1;
    }

    private int screenToGridY(float y) {
        float cellHeight = (getHeight() / (float) worldSnapshot.height()) * scaleFactor;
        int gridY = (int) ((y - offsetY) / cellHeight);
        return gridY >= 0 && gridY < worldSnapshot.height() ? gridY : -1;
    }

    private void clampOffsets() {
        if (worldSnapshot == null) {
            return;
        }
        float worldWidth = (getWidth() / (float) worldSnapshot.width()) * scaleFactor * worldSnapshot.width();
        float worldHeight = (getHeight() / (float) worldSnapshot.height()) * scaleFactor * worldSnapshot.height();
        float minOffsetX = Math.min(0f, getWidth() - worldWidth);
        float minOffsetY = Math.min(0f, getHeight() - worldHeight);
        offsetX = clamp(offsetX, minOffsetX, 0f);
        offsetY = clamp(offsetY, minOffsetY, 0f);
    }

    private void initializeViewportIfNeeded() {
        if (viewportInitialized || worldSnapshot == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float startupScaleX = worldSnapshot.width() / (float) STARTUP_VISIBLE_CELLS;
        float startupScaleY = worldSnapshot.height() / (float) STARTUP_VISIBLE_CELLS;
        scaleFactor = Math.max(startupScaleX, startupScaleY);
        float cellWidth = (getWidth() / (float) worldSnapshot.width()) * scaleFactor;
        float cellHeight = (getHeight() / (float) worldSnapshot.height()) * scaleFactor;
        float centerX = worldSnapshot.width() / 2f;
        float centerY = worldSnapshot.height() / 2f;
        offsetX = getWidth() / 2f - centerX * cellWidth;
        offsetY = getHeight() / 2f - centerY * cellHeight;
        viewportInitialized = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        initializeViewportIfNeeded();
        clampOffsets();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float previousScale = scaleFactor;
            float minScale = worldSnapshot != null ? worldSnapshot.width() / (float) MIN_VISIBLE_CELLS : 1f;
            float maxScale = worldSnapshot != null ? worldSnapshot.width() / (float) MAX_VISIBLE_CELLS : 4f;
            scaleFactor = clamp(scaleFactor * detector.getScaleFactor(), minScale, maxScale);
            if (worldSnapshot != null && previousScale != scaleFactor) {
                float focusRatioX = detector.getFocusX() - offsetX;
                float focusRatioY = detector.getFocusY() - offsetY;
                float scaleChange = scaleFactor / previousScale;
                offsetX = detector.getFocusX() - focusRatioX * scaleChange;
                offsetY = detector.getFocusY() - focusRatioY * scaleChange;
                clampOffsets();
                invalidate();
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            dragging = true;
            offsetX -= distanceX;
            offsetY -= distanceY;
            clampOffsets();
            invalidate();
            return true;
        }
    }

    public interface OnTileTapListener {
        void onTileTapped(int gridX, int gridY);
    }
}
