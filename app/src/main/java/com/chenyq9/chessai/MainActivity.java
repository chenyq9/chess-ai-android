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
                // AI 回合
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
            // 悔两步（玩家+AI），回到玩家回合
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
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        ChessEngine e = boardView.getEngine();
        if (e.isGameOver() || e.isRedTurn()) {
            handler.post(this::updateStatus);
            return;
        }
        ChessEngine.Move m = e.bestMove(false);
        if (m != null) {
            final int fr = m.fr, fc = m.fc, tr = m.tr, tc = m.tc;
            handler.post(() -> {
                e.move(fr, fc, tr, tc);
                boardView.invalidate();
                updateStatus();
            });
        }
    }

    private void sendChat() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        appendChat("你: " + text + "\n");
        executor.execute(() -> chatWithAI(text));
    }

    private void chatWithAI(String userMsg) {
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
            sys.put("content", "你是一个中国象棋助手，可以和用户聊天，也可以讨论棋局。回答简洁友好。");
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

            String reply;
            if (code >= 200 && code < 300) {
                JSONObject resp = new JSONObject(sb.toString());
                reply = resp.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");
            } else {
                reply = "AI调用失败(" + code + "): " + sb;
            }
            appendChat("AI: " + reply.trim() + "\n\n");
        } catch (Exception e) {
            appendChat("AI: 网络错误 - " + e.getMessage() + "\n\n");
        }
    }

    private void appendChat(String s) {
        handler.post(() -> {
            chatLog.append(s);
            tvChat.setText(chatLog.toString());
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        });
    }
}
