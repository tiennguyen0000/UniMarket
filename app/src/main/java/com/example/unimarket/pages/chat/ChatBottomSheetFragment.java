package com.example.unimarket.pages.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.model.Product;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_PRODUCT_TITLE = "product_title";
    private static final String ARG_PRODUCT_IMAGE_URL = "product_image_url";
    private static final String ARG_BUYER_ID = "buyer_id";
    private static final String ARG_SELLER_ID = "seller_id";
    private static final String ARG_BUYER_NAME = "buyer_name";
    private static final String ARG_SELLER_NAME = "seller_name";

    private ChatViewModel viewModel;
    private ChatMessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView tvEmptyState;
    private TextView tvChatTitle;
    private TextView tvChatSubtitle;

    public static ChatBottomSheetFragment newConversation(Conversation conversation) {
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();
        putConversationArgs(args, conversation);
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatBottomSheetFragment newProductChat(Product product, String imageUrl,
                                                         String buyerId, String buyerName,
                                                         String sellerName) {
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();

        String productId = product != null ? product.getId() : null;
        String sellerId = product != null ? product.getSeller_id() : null;
        String conversationId = ChatViewModel.buildProductConversationId(productId, buyerId, sellerId);
        args.putString(ARG_CONVERSATION_ID, conversationId);
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_PRODUCT_TITLE, product != null ? product.getTitle() : "Sản phẩm UniMarket");
        args.putString(ARG_PRODUCT_IMAGE_URL, imageUrl);
        args.putString(ARG_BUYER_ID, buyerId);
        args.putString(ARG_SELLER_ID, sellerId);
        args.putString(ARG_BUYER_NAME, buyerName);
        args.putString(ARG_SELLER_NAME, sellerName);

        fragment.setArguments(args);
        return fragment;
    }

    private static void putConversationArgs(Bundle args, Conversation conversation) {
        if (conversation == null) return;
        args.putString(ARG_CONVERSATION_ID, conversation.getId());
        args.putString(ARG_PRODUCT_ID, conversation.getProduct_id());
        args.putString(ARG_PRODUCT_TITLE, conversation.getProduct_title());
        args.putString(ARG_PRODUCT_IMAGE_URL, conversation.getProduct_image_url());
        args.putString(ARG_BUYER_ID, conversation.getBuyer_id());
        args.putString(ARG_SELLER_ID, conversation.getSeller_id());
        args.putString(ARG_BUYER_NAME, conversation.getBuyer_name());
        args.putString(ARG_SELLER_NAME, conversation.getSeller_name());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_chat, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.88f);
        bottomSheet.requestLayout();
        bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight((int) (screenHeight * 0.88f));
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = user != null ? user.getUid() : "guest_user";
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        adapter = new ChatMessageAdapter(currentUserId);

        rvMessages = view.findViewById(R.id.rvChatMessages);
        etMessage = view.findViewById(R.id.etChatMessage);
        tvEmptyState = view.findViewById(R.id.tvChatEmptyState);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);
        tvChatSubtitle = view.findViewById(R.id.tvChatSubtitle);
        ImageView closeButton = view.findViewById(R.id.ivCloseChat);
        View sendButton = view.findViewById(R.id.btnSendChat);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        closeButton.setOnClickListener(v -> dismiss());
        sendButton.setOnClickListener(v -> sendCurrentMessage(currentUserId));
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendCurrentMessage(currentUserId);
            return true;
        });

        observeViewModel();
        Conversation conversation = buildConversationFromArgs(currentUserId, user);
        bindHeader(conversation, currentUserId);
        viewModel.startListening(conversation);
    }

    private Conversation buildConversationFromArgs(String currentUserId, FirebaseUser user) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        Conversation conversation = new Conversation();
        conversation.setId(args.getString(ARG_CONVERSATION_ID));
        conversation.setProduct_id(args.getString(ARG_PRODUCT_ID));
        conversation.setProduct_title(args.getString(ARG_PRODUCT_TITLE));
        conversation.setProduct_image_url(args.getString(ARG_PRODUCT_IMAGE_URL));
        conversation.setBuyer_id(args.getString(ARG_BUYER_ID));
        conversation.setSeller_id(args.getString(ARG_SELLER_ID));
        conversation.setBuyer_name(args.getString(ARG_BUYER_NAME));
        conversation.setSeller_name(args.getString(ARG_SELLER_NAME));

        if (TextUtils.isEmpty(conversation.getBuyer_id())) {
            conversation.setBuyer_id(currentUserId);
        }
        if (TextUtils.isEmpty(conversation.getBuyer_name()) && user != null) {
            conversation.setBuyer_name(!TextUtils.isEmpty(user.getDisplayName())
                    ? user.getDisplayName() : user.getEmail());
        }
        return conversation;
    }

    private void bindHeader(Conversation conversation, String currentUserId) {
        String peerName = "Người bán";
        if (conversation != null) {
            boolean currentUserIsSeller = currentUserId != null && currentUserId.equals(conversation.getSeller_id());
            peerName = currentUserIsSeller
                    ? safeText(conversation.getBuyer_name(), "Người mua")
                    : safeText(conversation.getSeller_name(), "Người bán");
        }
        tvChatTitle.setText(peerName);
        tvChatSubtitle.setText("Trao đổi về: " + safeText(
                conversation != null ? conversation.getProduct_title() : null,
                "Sản phẩm UniMarket"
        ));
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            boolean empty = messages == null || messages.isEmpty();
            tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) {
                rvMessages.post(() -> rvMessages.scrollToPosition(adapter.getItemCount() - 1));
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error) && isAdded()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCurrentMessage(String currentUserId) {
        String content = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (content.isEmpty()) return;

        viewModel.sendMessage(currentUserId, content);
        etMessage.setText("");
        hideKeyboard();
    }

    private void hideKeyboard() {
        if (!isAdded() || getView() == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private String safeText(String value, String fallback) {
        return !TextUtils.isEmpty(value) ? value : fallback;
    }
}
