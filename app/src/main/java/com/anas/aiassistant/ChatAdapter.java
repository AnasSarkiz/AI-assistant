package com.anas.aiassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.noties.markwon.Markwon;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_BOT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserViewHolder) holder).bind(chatMessage);
        } else {
            ((BotViewHolder) holder).bind(chatMessage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    // ViewHolder for user messages
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;

        UserViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.textViewUserMessage);
            timestampText = view.findViewById(R.id.textViewTimestamp);
        }

        void bind(ChatMessage chatMessage) {
            messageText.setText(chatMessage.getMessage());
            timestampText.setText(chatMessage.getFormattedTime());
        }
    }

    // ViewHolder for bot messages
    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;

        BotViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.textViewBotMessage);
            timestampText = view.findViewById(R.id.textViewTimestamp);
        }

        void bind(ChatMessage chatMessage) {
            Markwon markwon = Markwon.create(itemView.getContext());
            markwon.setMarkdown(messageText, chatMessage.getMessage());
            timestampText.setText(chatMessage.getFormattedTime());
        }
    }
}