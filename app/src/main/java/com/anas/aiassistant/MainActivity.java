package com.anas.aiassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;

import com.anas.aiassistant.api.ApiClient;
import com.anas.aiassistant.api.GeminiRequest;
import com.anas.aiassistant.api.GeminiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "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";
    private static final String MODEL = "xiaomi/mimo-v2-flash:free";

    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant. Maintain conversation context and respond naturally. " +
            "Remember previous messages in the conversation and provide relevant, contextual responses. " +
            "Keep responses in plain text format, not markdown.";

    private static final int MAX_CONTEXT_LENGTH = 2000; // Very conservative limit to prevent token overuse

    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private Button buttonSend;
    private TextView textEmptyState;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private Toolbar toolbar;
    private ChatDatabaseHelper databaseHelper;
    private UserSession userSession;
    private long currentUserId;
    private long currentConversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ensure overflow menu is visible
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // Initialize database helper and user session
        databaseHelper = new ChatDatabaseHelper(this);
        userSession = new UserSession(this);

        // Check if user is logged in
        if (!userSession.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        currentUserId = userSession.getUserId();

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        textEmptyState = findViewById(R.id.textEmptyState);

        // Initialize or load current conversation
        initializeCurrentConversation();

        chatAdapter = new ChatAdapter(chatMessages);

        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);

        updateEmptyState();

        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToAI(message);
            }
        });
    }

    private void sendMessageToAI(String userMessage) {
        ChatMessage userChatMessage = new ChatMessage(userMessage, true);
        chatMessages.add(userChatMessage);

        // Save to database with conversation ID
        long messageId = databaseHelper.insertMessage(currentConversationId, userChatMessage);
        android.util.Log.d("Database", "Saved user message with ID: " + messageId + " to conversation " + currentConversationId);

        // Title will be generated after AI response for better context

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerViewChat.scrollToPosition(chatMessages.size() - 1);

        editTextMessage.setText("");
        updateEmptyState();

        // Build conversation history (keep ONLY last 2 exchanges to minimize token usage)
        StringBuilder conversationContext = new StringBuilder();
        conversationContext.append(SYSTEM_PROMPT).append("\n\n");

        // Keep only the MOST recent messages to drastically reduce token usage
        int messagesToInclude = Math.min(chatMessages.size(), 4); // Last 2 exchanges (4 messages max)
        int startIndex = Math.max(0, chatMessages.size() - messagesToInclude);

        // Add recent conversation history (limit context to prevent token overuse)
        for (int i = startIndex; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            if (msg.isUser()) {
                conversationContext.append("User: ").append(msg.getMessage()).append("\n");
            } else {
                conversationContext.append("Assistant: ").append(msg.getMessage()).append("\n");
            }
        }

        // Enforce maximum context length (2000 chars = ~400 tokens)
        if (conversationContext.length() > 2000) {
            conversationContext = new StringBuilder(
                SYSTEM_PROMPT + "\n\n[Previous context truncated due to length]\n\n" +
                "User: " + userMessage + "\n"
            );
        }

        // Build messages list
        List<GeminiRequest.Message> messages = new ArrayList<>();
        messages.add(new GeminiRequest.Message("system", SYSTEM_PROMPT));

        // Add conversation history
        for (int i = startIndex; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            String role = msg.isUser() ? "user" : "assistant";
            messages.add(new GeminiRequest.Message(role, msg.getMessage()));
        }

        // Debug: Log context size for monitoring token usage
        int contextMessageCount = chatMessages.size() - startIndex;
        android.util.Log.d("AIAssistant", "Sending " + contextMessageCount + " messages in context");

        GeminiRequest request = new GeminiRequest(MODEL, messages);

        ApiClient.getGeminiService()
                .generateChat("Bearer " + API_KEY, request)
                .enqueue(new Callback<GeminiResponse>() {

                     @Override
                     public void onResponse(Call<GeminiResponse> call,
                                            Response<GeminiResponse> response) {
                         if (response.isSuccessful() && response.body() != null) {
                             String aiText = response.body().getFirstText();
                             runOnUiThread(() -> {
                                 addBotMessage(aiText);

                                 // Generate title after first AI response for better context
                                 if (chatMessages.size() == 2) { // User message + AI response
                                     String generatedTitle = generateTitleFromConversation(userMessage, aiText);
                                     databaseHelper.updateConversationTitle(currentConversationId, generatedTitle);
                                 }
                             });
                          } else {
                              String errorMessage = getErrorMessage(response.code());
                              runOnUiThread(() -> addBotMessage(errorMessage));
                          }
                      }

                    @Override
                    public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        runOnUiThread(() -> addBotMessage("Network error: " + t.getMessage()));
                    }
                });
    }

    private void addBotMessage(String text) {
        ChatMessage botChatMessage = new ChatMessage(text, false);
        chatMessages.add(botChatMessage);

        // Save to database with conversation ID
        long botMessageId = databaseHelper.insertMessage(currentConversationId, botChatMessage);
        android.util.Log.d("Database", "Saved bot message with ID: " + botMessageId + " to conversation " + currentConversationId);

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
        updateEmptyState();
    }

    private String getErrorMessage(int code) {
        switch (code) {
            case 400:
                return "Bad request. Please check your input.";
            case 401:
                return "Authentication failed. Please check your API key.";
            case 403:
                return "Access forbidden. You may not have permission.";
            case 404:
                return "Resource not found.";
            case 429:
                return "Too many requests. Please try again later.";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "Error " + code + ": Something went wrong. Please try again.";
        }
    }

    private void updateEmptyState() {
        if (textEmptyState != null) {
            textEmptyState.setVisibility(chatMessages.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_new_chat) {
            createNewConversation();
            return true;
        } else if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (id == R.id.action_clear_history) {
            clearChatHistory();
            return true;
        } else if (id == R.id.action_logout) {
            userSession.logoutUser();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChatHistory() {
        // Clear messages for current conversation from database
        databaseHelper.deleteConversation(currentConversationId);

        // Create a new conversation to replace the cleared one
        createNewConversation();
    }

    private void initializeCurrentConversation() {
        // Get conversations for current user
        List<Conversation> conversations = databaseHelper.getConversationsForUser(currentUserId);

        if (conversations.isEmpty()) {
            // Create first conversation for this user
            currentConversationId = databaseHelper.createConversation(currentUserId, "New Chat");
            android.util.Log.d("Database", "Created new conversation with ID: " + currentConversationId + " for user " + currentUserId);
        } else {
            // Load the most recent conversation
            currentConversationId = conversations.get(0).getId();
            android.util.Log.d("Database", "Loaded existing conversation with ID: " + currentConversationId + " (" + conversations.size() + " total conversations for user " + currentUserId + ")");
        }

        // Load messages for current conversation
        chatMessages = databaseHelper.getMessagesForConversation(currentConversationId);
        android.util.Log.d("Database", "Loaded " + chatMessages.size() + " messages for conversation " + currentConversationId);
    }

    private void createNewConversation() {
        // Generate a title for the new conversation
        String title = generateConversationTitle();

        // Create new conversation for current user
        currentConversationId = databaseHelper.createConversation(currentUserId, title);

        // Clear current messages and load empty conversation
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private String generateConversationTitle() {
        // For now, use a simple title. Later we can make it smarter
        return "New Chat " + (databaseHelper.getConversationsForUser(currentUserId).size() + 1);
    }

    private String generateTitleFromMessage(String message) {
        // Clean and truncate the message for use as title
        String cleanMessage = message.trim();

        // Remove common greetings and make it title-case
        cleanMessage = cleanMessage.replaceAll("(?i)^(hi|hello|hey|good morning|good afternoon|good evening)\\s*", "");

        // Limit length and add ellipsis if needed
        if (cleanMessage.length() > 30) {
            cleanMessage = cleanMessage.substring(0, 27) + "...";
        }

        // Capitalize first letter
        if (!cleanMessage.isEmpty()) {
            cleanMessage = cleanMessage.substring(0, 1).toUpperCase() +
                           cleanMessage.substring(1).toLowerCase();
        }

        // Fallback if message becomes empty
        if (cleanMessage.trim().isEmpty()) {
            return "New Conversation";
        }

        return cleanMessage;
    }

    private String generateTitleFromConversation(String userMessage, String aiResponse) {
        // Try to extract a meaningful title from the conversation
        String combined = userMessage + " " + aiResponse;

        // Look for question patterns
        if (userMessage.toLowerCase().startsWith("what") ||
            userMessage.toLowerCase().startsWith("how") ||
            userMessage.toLowerCase().startsWith("why") ||
            userMessage.toLowerCase().startsWith("when") ||
            userMessage.toLowerCase().startsWith("where")) {
            // For questions, use the question itself as title
            return generateTitleFromMessage(userMessage);
        }

        // For other conversations, try to find key topics
        // This is a simple implementation - could be enhanced with AI-generated titles
        String[] words = combined.split("\\s+");
        if (words.length > 3) {
            // Take first few meaningful words
            StringBuilder title = new StringBuilder();
            int wordCount = 0;
            for (String word : words) {
                if (word.length() > 3 && wordCount < 4) { // Skip short words
                    if (title.length() > 0) title.append(" ");
                    title.append(word);
                    wordCount++;
                }
            }
            if (title.length() > 0) {
                return generateTitleFromMessage(title.toString());
            }
        }

        // Fallback to user message
        return generateTitleFromMessage(userMessage);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
