package com.example.unimarket.pages.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {
    private final List<Message> items = new ArrayList<>();
    private final String currentUserId;

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Message> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        private final View botContainer;
        private final View meContainer;
        private final TextView tvBotMessage;
        private final TextView tvMeMessage;

        ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            botContainer = itemView.findViewById(R.id.botMessageContainer);
            meContainer = itemView.findViewById(R.id.meMessageContainer);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            tvMeMessage = itemView.findViewById(R.id.tvMeMessage);
        }

        void bind(Message message) {
            boolean isMine = message != null
                    && !TextUtils.isEmpty(currentUserId)
                    && currentUserId.equals(message.getSender_id());
            String content = message != null && !TextUtils.isEmpty(message.getContent())
                    ? message.getContent()
                    : "";

            botContainer.setVisibility(isMine ? View.GONE : View.VISIBLE);
            meContainer.setVisibility(isMine ? View.VISIBLE : View.GONE);
            if (isMine) {
                tvMeMessage.setText(content);
            } else {
                tvBotMessage.setText(content);
            }
        }
    }
}
