package com.chenyq9.chessai;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ChessEngine {
    private Piece[][] board = new Piece[10][9];
    private boolean redTurn = true;
    private boolean gameOver = false;
    private Stack<int[][]> history = new Stack<>();

    public ChessEngine() {
        reset();
    }

    public void reset() {
        redTurn = true; gameOver = false; history.clear();
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++) board[r][c] = null;
        board[9][0] = new Piece(Piece.Type.ROOK, true); board[9][8] = new Piece(Piece.Type.ROOK, true);
        board[9][1] = new Piece(Piece.Type.HORSE, true); board[9][7] = new Piece(Piece.Type.HORSE, true);
        board[9][2] = new Piece(Piece.Type.ELEPHANT, true); board[9][6] = new Piece(Piece.Type.ELEPHANT, true);
        board[9][3] = new Piece(Piece.Type.ADVISOR, true); board[9][5] = new Piece(Piece.Type.ADVISOR, true);
        board[9][4] = new Piece(Piece.Type.KING, true);
        board[7][1] = new Piece(Piece.Type.CANNON, true); board[7][7] = new Piece(Piece.Type.CANNON, true);
        for (int c = 0; c < 9; c += 2) board[6][c] = new Piece(Piece.Type.PAWN, true);

        board[0][0] = new Piece(Piece.Type.ROOK, false); board[0][8] = new Piece(Piece.Type.ROOK, false);
        board[0][1] = new Piece(Piece.Type.HORSE, false); board[0][7] = new Piece(Piece.Type.HORSE, false);
        board[0][2] = new Piece(Piece.Type.ELEPHANT, false); board[0][6] = new Piece(Piece.Type.ELEPHANT, false);
        board[0][3] = new Piece(Piece.Type.ADVISOR, false); board[0][5] = new Piece(Piece.Type.ADVISOR, false);
        board[0][4] = new Piece(Piece.Type.KING, false);
        board[2][1] = new Piece(Piece.Type.CANNON, false); board[2][7] = new Piece(Piece.Type.CANNON, false);
        for (int c = 0; c < 9; c += 2) board[3][c] = new Piece(Piece.Type.PAWN, false);
    }

    public Piece getPiece(int r, int c) { return board[r][c]; }
    public boolean isRedTurn() { return redTurn; }
    public boolean isGameOver() { return gameOver; }

    public boolean legalMove(int fr, int fc, int tr, int tc, boolean red) {
        if (gameOver || fr < 0 || fr > 9 || fc < 0 || fc > 8 || tr < 0 || tr > 9 || tc < 0 || tc > 8) return false;
        Piece p = board[fr][fc];
        if (p == null || p.red != red) return false;
        Piece target = board[tr][tc];
        if (target != null && target.red == red) return false;
        if (fr == tr && fc == tc) return false;

        int dr = tr - fr, dc = tc - fc;
        switch (p.type) {
            case ROOK:
                if (dr != 0 && dc != 0) return false;
                return pathClear(fr, fc, tr, tc) && (target == null || target.red != red);
            case CANNON:
                if (dr != 0 && dc != 0) return false;
                int cnt = countBetween(fr, fc, tr, tc);
                return (target == null && cnt == 0) || (target != null && cnt == 1);
            case HORSE:
                if (!(Math.abs(dr) == 2 && Math.abs(dc) == 1) && !(Math.abs(dr) == 1 && Math.abs(dc) == 2)) return false;
                int br = fr + (Math.abs(dr) == 2 ? dr / 2 : 0);
                int bc = fc + (Math.abs(dc) == 2 ? dc / 2 : 0);
                return board[br][bc] == null;
            case ELEPHANT:
                if (Math.abs(dr) != 2 || Math.abs(dc) != 2) return false;
                if (red && tr < 5) return false;
                if (!red && tr > 4) return false;
                return board[fr + dr / 2][fc + dc / 2] == null;
            case ADVISOR:
                if (Math.abs(dr) != 1 || Math.abs(dc) != 1) return false;
                if (red && (tr < 7 || tc < 3 || tc > 5)) return false;
                if (!red && (tr > 2 || tc < 3 || tc > 5)) return false;
                return true;
            case KING:
                if (Math.abs(dr) + Math.abs(dc) != 1) return false;
                if (red && (tr < 7 || tc < 3 || tc > 5)) return false;
                if (!red && (tr > 2 || tc < 3 || tc > 5)) return false;
                return true;
            case PAWN:
                if (red) {
                    if (dr > 0) return false;
                    if (fr >= 5) {
                        return dr == -1 && dc == 0;
                    } else {
                        return (dr == -1 && dc == 0) || (dr == 0 && Math.abs(dc) == 1);
                    }
                } else {
                    if (dr < 0) return false;
                    if (fr <= 4) {
                        return dr == 1 && dc == 0;
                    } else {
                        return (dr == 1 && dc == 0) || (dr == 0 && Math.abs(dc) == 1);
                    }
                }
        }
        return false;
    }

    private boolean pathClear(int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board[r][c] != null) return false;
            r += dr; c += dc;
        }
        return true;
    }

    private int countBetween(int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int cnt = 0, r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board[r][c] != null) cnt++;
            r += dr; c += dc;
        }
        return cnt;
    }

    public boolean move(int fr, int fc, int tr, int tc) {
        if (!legalMove(fr, fc, tr, tc, redTurn)) return false;
        int[][] backup = snapshot();
        history.push(backup);
        Piece captured = board[tr][tc];
        board[tr][tc] = board[fr][fc];
        board[fr][fc] = null;
        if (captured != null && captured.type == Piece.Type.KING) gameOver = true;
        redTurn = !redTurn;
        return true;
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        int[][] snap = history.pop();
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++) {
                int v = snap[r][c];
                if (v == 0) board[r][c] = null;
                else board[r][c] = new Piece(Piece.Type.values()[(v - 1) % 7], v <= 7);
            }
        redTurn = !redTurn;
        gameOver = false;
        return true;
    }

    private int[][] snapshot() {
        int[][] s = new int[10][9];
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++) {
                Piece p = board[r][c];
                if (p == null) s[r][c] = 0;
                else s[r][c] = p.type.ordinal() + 1 + (p.red ? 0 : 7);
            }
        return s;
    }

    public static class Move { public int fr, fc, tr, tc; public Move(int a,int b,int c,int d){fr=a;fc=b;tr=c;tc=d;} }

    public Move bestMove(boolean red) {
        List<Move> moves = allMoves(red);
        if (moves.isEmpty()) return null;
        int bestScore = Integer.MIN_VALUE;
        Move best = moves.get(0);
        for (Move m : moves) {
            int score = scoreMove(m, red);
            if (score > bestScore) { bestScore = score; best = m; }
        }
        return best;
    }

    private List<Move> allMoves(boolean red) {
        List<Move> list = new ArrayList<>();
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++)
                if (board[r][c] != null && board[r][c].red == red)
                    for (int tr = 0; tr < 10; tr++)
                        for (int tc = 0; tc < 9; tc++)
                            if (legalMove(r, c, tr, tc, red))
                                list.add(new Move(r, c, tr, tc));
        return list;
    }

    private int scoreMove(Move m, boolean red) {
        Piece captured = board[m.tr][m.tc];
        int s = 0;
        if (captured != null) {
            switch (captured.type) {
                case KING: s += 10000; break;
                case ROOK: s += 900; break;
                case CANNON: s += 450; break;
                case HORSE: s += 400; break;
                case ELEPHANT: s += 200; break;
                case ADVISOR: s += 200; break;
                case PAWN: s += 100; break;
            }
        }
        // 简单位置权重
        int r = red ? (9 - m.tr) : m.tr;
        s += r;
        return s;
    }
}
