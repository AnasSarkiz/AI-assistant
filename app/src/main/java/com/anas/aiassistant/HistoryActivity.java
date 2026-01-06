package com.anas.aiassistant;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversations;
    private ChatDatabaseHelper databaseHelper;
    private UserSession userSession;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        databaseHelper = new ChatDatabaseHelper(this);
        userSession = new UserSession(this);

        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        currentUserId = userSession.getUserId();

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        loadConversations();
    }

    private void loadConversations() {
        conversations = databaseHelper.getConversationsForUser(currentUserId);
        conversationAdapter = new ConversationAdapter(this, conversations, conversationId -> {
            databaseHelper.deleteConversation(conversationId);
            loadConversations(); // Refresh the list
        });
        recyclerViewHistory.setAdapter(conversationAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations(); // Refresh list when returning
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}