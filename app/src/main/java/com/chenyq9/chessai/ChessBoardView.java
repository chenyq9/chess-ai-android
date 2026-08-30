package com.chenyq9.chessai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ChessBoardView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ChessEngine engine;
    private OnMoveListener listener;
    private int selectedRow = -1, selectedCol = -1;

    public interface OnMoveListener {
        void onPlayerMove(int fromRow, int fromCol, int toRow, int toCol, boolean moved);
    }

    public ChessBoardView(Context c, AttributeSet a) {
        super(c, a);
        engine = new ChessEngine();
    }

    public void setOnMoveListener(OnMoveListener l) { this.listener = l; }
    public ChessEngine getEngine() { return engine; }
    public void setEngine(ChessEngine e) { engine = e; invalidate(); }
    public void clearSelection() { selectedRow = selectedCol = -1; invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        float left = (w - size) / 2f, top = (h - size) / 2f;
        float cell = size / 10f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(213, 176, 120));
        canvas.drawRoundRect(left - 4, top - 4, left + size + 4, top + size + 4, 12, 12, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.rgb(80, 50, 20));

        for (int i = 0; i < 10; i++) {
            canvas.drawLine(left + cell, top + (i + 0.5f) * cell, left + 8 * cell, top + (i + 0.5f) * cell, paint);
        }
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 8) {
                canvas.drawLine(left + (i + 0.5f) * cell, top + cell, left + (i + 0.5f) * cell, top + 9 * cell, paint);
            } else {
                canvas.drawLine(left + (i + 0.5f) * cell, top + cell, left + (i + 0.5f) * cell, top + 4 * cell, paint);
                canvas.drawLine(left + (i + 0.5f) * cell, top + 6 * cell, left + (i + 0.5f) * cell, top + 9 * cell, paint);
            }
        }
        canvas.drawLine(left + 3.5f * cell, top + cell, left + 5.5f * cell, top + 3 * cell, paint);
        canvas.drawLine(left + 5.5f * cell, top + cell, left + 3.5f * cell, top + 3 * cell, paint);
        canvas.drawLine(left + 3.5f * cell, top + 7 * cell, left + 5.5f * cell, top + 9 * cell, paint);
        canvas.drawLine(left + 5.5f * cell, top + 7 * cell, left + 3.5f * cell, top + 9 * cell, paint);

        if (selectedRow >= 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(90, 255, 200, 0));
            canvas.drawCircle(left + (selectedCol + 0.5f) * cell, top + (selectedRow + 0.5f) * cell, cell * 0.45f, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece p = engine.getPiece(r, c);
                if (p == null) continue;
                float cx = left + (c + 0.5f) * cell;
                float cy = top + (r + 0.5f) * cell;
                paint.setColor(p.red ? Color.rgb(220, 80, 60) : Color.rgb(40, 40, 40));
                canvas.drawCircle(cx, cy, cell * 0.42f, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(cell * 0.55f);
                paint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = paint.getFontMetrics();
                float by = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(p.name(), cx, by, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return true;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        float cell = size / 10f;
        int col = (int) ((e.getX() - left) / cell);
        int row = (int) ((e.getY() - top) / cell);
        if (row < 0 || row > 9 || col < 0 || col > 8) return true;

        if (!engine.isRedTurn()) return true; // 黑方回合由AI处理，玩家不可动
        if (selectedRow < 0) {
            Piece p = engine.getPiece(row, col);
            if (p != null && p.red) { selectedRow = row; selectedCol = col; }
        } else {
            boolean moved = false;
            if (engine.legalMove(selectedRow, selectedCol, row, col, true)) {
                engine.move(selectedRow, selectedCol, row, col);
                moved = true;
            }
            if (listener != null) listener.onPlayerMove(selectedRow, selectedCol, row, col, moved);
            selectedRow = selectedCol = -1;
        }
        invalidate();
        return true;
    }
}
