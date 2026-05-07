package com.example.unimarket.pages.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final String currentUserId;
    private final OnConversationClickListener listener;
    private final List<Conversation> items = new ArrayList<>();

    public ConversationAdapter(String currentUserId, OnConversationClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submitList(List<Conversation> conversations) {
        items.clear();
        if (conversations != null) {
            items.addAll(conversations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProduct;
        private final TextView tvPeerName;
        private final TextView tvProductTitle;
        private final TextView tvLastMessage;
        private final TextView tvRoleBadge;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivConversationProduct);
            tvPeerName = itemView.findViewById(R.id.tvConversationPeerName);
            tvProductTitle = itemView.findViewById(R.id.tvConversationProductTitle);
            tvLastMessage = itemView.findViewById(R.id.tvConversationLastMessage);
            tvRoleBadge = itemView.findViewById(R.id.tvConversationRoleBadge);
        }

        void bind(Conversation conversation) {
            boolean isSeller = conversation != null
                    && !TextUtils.isEmpty(currentUserId)
                    && currentUserId.equals(conversation.getSeller_id());

            String peerName = isSeller
                    ? safeText(conversation != null ? conversation.getBuyer_name() : null, "Người mua")
                    : safeText(conversation != null ? conversation.getSeller_name() : null, "Người bán");
            tvPeerName.setText(peerName);
            tvProductTitle.setText(safeText(conversation != null ? conversation.getProduct_title() : null,
                    "Sản phẩm UniMarket"));
            tvLastMessage.setText(safeText(conversation != null ? conversation.getLast_message() : null,
                    "Chưa có tin nhắn. Bắt đầu trao đổi ngay."));
            tvRoleBadge.setText(isSeller ? "Bạn là người bán" : "Bạn là người mua");

            String imageUrl = conversation != null ? conversation.getProduct_image_url() : null;
            Glide.with(itemView.getContext())
                    .load(!TextUtils.isEmpty(imageUrl) ? imageUrl : R.drawable.ic_uni_logo)
                    .centerCrop()
                    .placeholder(R.drawable.bg_light_grey_rounded)
                    .error(R.drawable.ic_uni_logo)
                    .into(ivProduct);

            itemView.setOnClickListener(v -> {
                if (listener != null && conversation != null) {
                    listener.onConversationClick(conversation);
                }
            });
        }

        private String safeText(String value, String fallback) {
            return !TextUtils.isEmpty(value) ? value : fallback;
        }
    }
}
