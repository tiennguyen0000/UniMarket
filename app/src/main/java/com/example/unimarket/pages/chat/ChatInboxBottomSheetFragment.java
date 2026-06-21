package com.example.unimarket.pages.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Conversation;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatInboxBottomSheetFragment extends BottomSheetDialogFragment {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, Conversation> buyerConversations = new LinkedHashMap<>();
    private final Map<String, Conversation> sellerConversations = new LinkedHashMap<>();

    private ListenerRegistration buyerListener;
    private ListenerRegistration sellerListener;
    private ConversationAdapter adapter;
    private TextView tvEmptyState;
    private RecyclerView rvConversations;
    private boolean buyerLoaded;
    private boolean sellerLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_chat_inbox, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.82f);
        bottomSheet.requestLayout();
        bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight((int) (screenHeight * 0.82f));
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView closeButton = view.findViewById(R.id.ivCloseInbox);
        tvEmptyState = view.findViewById(R.id.tvConversationEmptyState);
        rvConversations = view.findViewById(R.id.rvConversations);
        closeButton.setOnClickListener(v -> dismiss());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvEmptyState.setText("Vui lòng đăng nhập để xem tin nhắn.");
            tvEmptyState.setVisibility(View.VISIBLE);
            rvConversations.setVisibility(View.GONE);
            return;
        }

        adapter = new ConversationAdapter(currentUser.getUid(), this::openConversation);
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.setAdapter(adapter);
        showLoadingState();

        listenForConversations(currentUser.getUid());
    }

    private void listenForConversations(String userId) {
        buyerListener = db.collection("conversations")
                .whereEqualTo("buyer_id", userId)
                .addSnapshotListener((snapshots, error) -> {
                    buyerLoaded = true;
                    if (error != null) {
                        showError(error.getMessage());
                        renderConversations();
                        return;
                    }
                    buyerConversations.clear();
                    if (snapshots != null) {
                        snapshots.forEach(doc -> {
                            Conversation conversation = doc.toObject(Conversation.class);
                            if (conversation != null) {
                                conversation.setId(doc.getId());
                                buyerConversations.put(doc.getId(), conversation);
                            }
                        });
                    }
                    renderConversations();
                });

        sellerListener = db.collection("conversations")
                .whereEqualTo("seller_id", userId)
                .addSnapshotListener((snapshots, error) -> {
                    sellerLoaded = true;
                    if (error != null) {
                        showError(error.getMessage());
                        renderConversations();
                        return;
                    }
                    sellerConversations.clear();
                    if (snapshots != null) {
                        snapshots.forEach(doc -> {
                            Conversation conversation = doc.toObject(Conversation.class);
                            if (conversation != null) {
                                conversation.setId(doc.getId());
                                sellerConversations.put(doc.getId(), conversation);
                            }
                        });
                    }
                    renderConversations();
                });
    }

    private void renderConversations() {
        Map<String, Conversation> conversationsById = new LinkedHashMap<>();
        conversationsById.putAll(buyerConversations);
        conversationsById.putAll(sellerConversations);

        if ((!buyerLoaded || !sellerLoaded) && conversationsById.isEmpty()) {
            showLoadingState();
            return;
        }

        List<Conversation> conversations = new ArrayList<>(conversationsById.values());
        conversations.sort(Comparator.comparing(this::sortKey).reversed());
        adapter.submitList(conversations);

        boolean empty = conversations.isEmpty();
        if (empty) {
            tvEmptyState.setText("Chưa có nội dung\nHãy mở chi tiết sản phẩm và nhắn tin với người bán.");
        }
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvConversations.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLoadingState() {
        tvEmptyState.setText("Đang tải cuộc trò chuyện...");
        tvEmptyState.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
    }

    private void openConversation(Conversation conversation) {
        dismiss();
        ChatBottomSheetFragment
                .newConversation(conversation)
                .show(getParentFragmentManager(), "chat_conversation_" + conversation.getId());
    }

    private String sortKey(Conversation conversation) {
        if (conversation == null) return "";
        if (conversation.getLast_message_at() != null) return conversation.getLast_message_at();
        if (conversation.getUpdated_at() != null) return conversation.getUpdated_at();
        return conversation.getCreated_at() != null ? conversation.getCreated_at() : "";
    }

    private void showError(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message != null ? message : "Không thể tải tin nhắn.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (buyerListener != null) buyerListener.remove();
        if (sellerListener != null) sellerListener.remove();
        super.onDestroyView();
    }
}
