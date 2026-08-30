package com.chenyq9.chessai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    // 在这里填入你的 DeepSeek API Key
    private static final String API_KEY = "sk-你的DeepSeek密钥";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    private ChessBoardView boardView;
    private TextView tvStatus;
    private TextView tvChat;
    private EditText etInput;
    private ScrollView scrollChat;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private StringBuilder chatLog = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boardView = findViewById(R.id.chess_board);
        tvStatus = findViewById(R.id.tv_status);
        tvChat = findViewById(R.id.tv_chat);
        etInput = findViewById(R.id.et_input);
        scrollChat = findViewById(R.id.scroll_chat);
        Button btnRestart = findViewById(R.id.btn_restart);
        Button btnUndo = findViewById(R.id.btn_undo);
        Button btnSend = findViewById(R.id.btn_send);

        boardView.setOnMoveListener((fr, fc, tr, tc, moved) -> {
            if (moved) {
                updateStatus();
                if (boardView.getEngine().isGameOver()) {
                    tvStatus.setText("红方胜！");
                    return;
                }
                tvStatus.setText("AI思考中...");
                executor.execute(this::aiMove);
            } else {
                Toast.makeText(this, "不合法的走法", Toast.LENGTH_SHORT).show();
            }
        });

        btnRestart.setOnClickListener(v -> {
            boardView.getEngine().reset();
            boardView.clearSelection();
            boardView.invalidate();
            updateStatus();
        });

        btnUndo.setOnClickListener(v -> {
            ChessEngine e = boardView.getEngine();
            if (e.undo() && e.undo()) {
                boardView.clearSelection();
                boardView.invalidate();
                updateStatus();
            } else {
                Toast.makeText(this, "无法悔棋", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(v -> sendChat());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            sendChat();
            return true;
        });

        updateStatus();
    }

    private void updateStatus() {
        ChessEngine e = boardView.getEngine();
        if (e.isGameOver()) {
            tvStatus.setText(e.isRedTurn() ? "黑方胜！" : "红方胜！");
        } else {
            tvStatus.setText(e.isRedTurn() ? "红方走棋" : "黑方(AI)走棋");
        }
    }

    private void aiMove() {
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        ChessEngine e = boardView.getEngine();
        if (e.isGameOver() || e.isRedTurn()) {
            handler.post(this::updateStatus);
            return;
        }

        String boardText = buildBoardText(e);
        String sys = "你是中国象棋高手。棋盘是10行9列，行0在顶部（黑方底线），行9在底部（红方底线），列0在最左。"
                + "当前轮到黑方走棋。请分析局面，返回黑方最佳走法。"
                + "只返回一行，格式严格为：fr,fc,tr,tc （四个整数，用英文逗号分隔，不要任何其他文字）。"
                + "例如：2,1,2,3 表示从(2,1)走到(2,3)。";
        String user = "当前棋盘（用 . 表示空位）：\n" + boardText
                + "\n请给出黑方最佳走法（fr,fc,tr,tc）：";

        String reply = callDeepSeek(sys, user);
        int[] mv = parseMove(reply);
        boolean moved = false;
        if (mv != null) {
            final int fr = mv[0], fc = mv[1], tr = mv[2], tc = mv[3];
            if (e.legalMove(fr, fc, tr, tc, false)) {
                final boolean[] ok = {false};
                handler.post(() -> {
                    e.move(fr, fc, tr, tc);
                    boardView.invalidate();
                    updateStatus();
                });
                moved = true;
            }
        }

        if (!moved) {
            // 大模型返回非法，回退到本地算法
            ChessEngine.Move local = e.bestMove(false);
            if (local != null) {
                final int fr = local.fr, fc = local.fc, tr = local.tr, tc = local.tc;
                handler.post(() -> {
                    e.move(fr, fc, tr, tc);
                    boardView.invalidate();
                    updateStatus();
                });
            }
        }
    }

    private String buildBoardText(ChessEngine e) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece p = e.getPiece(r, c);
                if (p == null) {
                    sb.append(".");
                } else {
                    sb.append(p.red ? "R" : "B");
                    switch (p.type) {
                        case KING: sb.append("K"); break;
                        case ADVISOR: sb.append("A"); break;
                        case ELEPHANT: sb.append("E"); break;
                        case HORSE: sb.append("H"); break;
                        case ROOK: sb.append("R"); break;
                        case CANNON: sb.append("C"); break;
                        case PAWN: sb.append("P"); break;
                    }
                }
                sb.append(c == 8 ? "\n" : " ");
            }
        }
        return sb.toString();
    }

    private int[] parseMove(String reply) {
        if (reply == null) return null;
        Pattern p = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)");
        Matcher m = p.matcher(reply);
        if (m.find()) {
            try {
                int fr = Integer.parseInt(m.group(1));
                int fc = Integer.parseInt(m.group(2));
                int tr = Integer.parseInt(m.group(3));
                int tc = Integer.parseInt(m.group(4));
                if (fr >= 0 && fr <= 9 && fc >= 0 && fc <= 8 && tr >= 0 && tr <= 9 && tc >= 0 && tc <= 8) {
                    return new int[]{fr, fc, tr, tc};
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String callDeepSeek(String systemPrompt, String userMsg) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            JSONObject body = new JSONObject();
            body.put("model", "deepseek-chat");
            JSONArray messages = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.put(sys);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", userMsg);
            messages.put(user);
            body.put("messages", messages);
            body.put("max_tokens", 500);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            BufferedReader br;
            if (code >= 200 && code < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();

            if (code >= 200 && code < 300) {
                JSONObject resp = new JSONObject(sb.toString());
                return resp.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void sendChat() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        appendChat("你: " + text + "\n");
        executor.execute(() -> {
            String reply = callDeepSeek(
                    "你是一个中国象棋助手，可以和用户聊天，也可以讨论棋局。回答简洁友好。",
                    text
            );
            if (reply == null) reply = "AI调用失败，请检查网络或API Key";
            appendChat("AI: " + reply.trim() + "\n\n");
        });
    }

    private void appendChat(String s) {
        handler.post(() -> {
            chatLog.append(s);
            tvChat.setText(chatLog.toString());
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        });
    }
}
