package com.chenyq9.chessai;

public class Piece {
    public enum Type { KING, ADVISOR, ELEPHANT, HORSE, ROOK, CANNON, PAWN }
    public final Type type;
    public final boolean red; // true=红方(玩家), false=黑方(AI)

    public Piece(Type type, boolean red) {
        this.type = type;
        this.red = red;
    }

    public String name() {
        String[] redNames = {"帅","仕","相","马","车","炮","兵"};
        String[] blackNames = {"将","士","象","马","车","炮","卒"};
        String[] n = red ? redNames : blackNames;
        switch (type) {
            case KING: return n[0];
            case ADVISOR: return n[1];
            case ELEPHANT: return n[2];
            case HORSE: return n[3];
            case ROOK: return n[4];
            case CANNON: return n[5];
            case PAWN: return n[6];
        }
        return "?";
    }
}
