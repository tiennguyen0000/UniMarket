package com.example.unimarket.pages.home;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Cart;
import com.example.unimarket.data.model.CartItem;
import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.model.DiscountCode;
import com.example.unimarket.data.model.Message;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.OrderItem;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CartItemService;
import com.example.unimarket.data.service.CartService;
import com.example.unimarket.data.service.DiscountCodeService;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.OrderItemService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.pages.chat.ChatViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartBottomSheetFragment extends BottomSheetDialogFragment {
    private RecyclerView rvCartItems;
    private View layoutCartEmpty;
    private View layoutCartFooter;
    private View layoutCartCheckoutBar;
    private ProgressBar progressCart;
    private TextView tvCartTotal;
    private TextView tvCartSubtotal;
    private TextView tvCartShipping;
    private TextView tvCartDiscount;
    private TextView btnCartCheckout;
    private View btnShipStandard;
    private View btnShipExpress;
    private TextView tvShipStandardTitle;
    private TextView tvShipStandardDetail;
    private TextView tvShipStandardFee;
    private TextView tvShipExpressTitle;
    private TextView tvShipExpressDetail;
    private TextView tvShipExpressFee;
    private ImageView ivShipStandardIcon;
    private ImageView ivShipExpressIcon;
    private TextView btnApplyDiscount;
    private TextView tvDiscountStatus;
    private View layoutCheckoutHeader;
    private View layoutCheckoutFields;
    private TextView tvCheckoutFormSummary;
    private ImageView ivCheckoutFormToggle;
    private EditText etCheckoutPhone;
    private EditText etCheckoutLocation;
    private EditText etSellerNote;
    private EditText etDiscountCode;
    private CartItemAdapter adapter;

    private final CartService cartService = new CartService();
    private final CartItemService cartItemService = new CartItemService();
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final OrderItemService orderItemService = new OrderItemService();
    private final NotificationService notificationService = new NotificationService();
    private final DiscountCodeService discountCodeService = new DiscountCodeService();
    private final UserService userService = new UserService();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String SHIPPING_STANDARD = "standard";
    private static final String SHIPPING_EXPRESS = "express";
    private static final double STANDARD_SHIPPING_FEE = 10000d;
    private static final double EXPRESS_SHIPPING_FEE = 25000d;

    private String selectedShippingMethod = SHIPPING_STANDARD;
    private DiscountCode appliedDiscountCode;
    private double appliedDiscountAmount;
    private double cartSubtotal;
    private boolean checkoutFormExpanded = true;

    public static CartBottomSheetFragment newInstance() {
        return new CartBottomSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_cart, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(params);
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutCartEmpty = view.findViewById(R.id.layoutCartEmpty);
        layoutCartFooter = view.findViewById(R.id.layoutCartFooter);
        layoutCartCheckoutBar = view.findViewById(R.id.layoutCartCheckoutBar);
        progressCart = view.findViewById(R.id.progressCart);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        tvCartSubtotal = view.findViewById(R.id.tvCartSubtotal);
        tvCartShipping = view.findViewById(R.id.tvCartShipping);
        tvCartDiscount = view.findViewById(R.id.tvCartDiscount);
        btnCartCheckout = view.findViewById(R.id.btnCartCheckout);
        btnShipStandard = view.findViewById(R.id.btnShipStandard);
        btnShipExpress = view.findViewById(R.id.btnShipExpress);
        tvShipStandardTitle = view.findViewById(R.id.tvShipStandardTitle);
        tvShipStandardDetail = view.findViewById(R.id.tvShipStandardDetail);
        tvShipStandardFee = view.findViewById(R.id.tvShipStandardFee);
        tvShipExpressTitle = view.findViewById(R.id.tvShipExpressTitle);
        tvShipExpressDetail = view.findViewById(R.id.tvShipExpressDetail);
        tvShipExpressFee = view.findViewById(R.id.tvShipExpressFee);
        ivShipStandardIcon = view.findViewById(R.id.ivShipStandardIcon);
        ivShipExpressIcon = view.findViewById(R.id.ivShipExpressIcon);
        btnApplyDiscount = view.findViewById(R.id.btnApplyDiscount);
        tvDiscountStatus = view.findViewById(R.id.tvDiscountStatus);
        layoutCheckoutHeader = view.findViewById(R.id.layoutCheckoutHeader);
        layoutCheckoutFields = view.findViewById(R.id.layoutCheckoutFields);
        tvCheckoutFormSummary = view.findViewById(R.id.tvCheckoutFormSummary);
        ivCheckoutFormToggle = view.findViewById(R.id.ivCheckoutFormToggle);
        etCheckoutPhone = view.findViewById(R.id.etCheckoutPhone);
        etCheckoutLocation = view.findViewById(R.id.etCheckoutLocation);
        etSellerNote = view.findViewById(R.id.etSellerNote);
        etDiscountCode = view.findViewById(R.id.etDiscountCode);
        ImageView close = view.findViewById(R.id.ivCartClose);

        adapter = new CartItemAdapter(new CartItemAdapter.Listener() {
            @Override
            public void onQuantityChanged(CartItemAdapter.CartLine line, int quantity) {
                updateQuantity(line, quantity);
            }

            @Override
            public void onRemove(CartItemAdapter.CartLine line) {
                removeLine(line);
            }
        });
        rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCartItems.setAdapter(adapter);
        close.setOnClickListener(v -> dismiss());
        btnCartCheckout.setOnClickListener(v -> checkout());
        btnShipStandard.setOnClickListener(v -> selectShipping(SHIPPING_STANDARD));
        btnShipExpress.setOnClickListener(v -> selectShipping(SHIPPING_EXPRESS));
        btnApplyDiscount.setOnClickListener(v -> applyDiscountCode());
        layoutCheckoutHeader.setOnClickListener(v -> setCheckoutFormExpanded(!checkoutFormExpanded));
        selectShipping(SHIPPING_STANDARD);
        TextWatcher checkoutWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateCheckoutAvailability();
                updateCheckoutFormSummary();
            }
        };
        etCheckoutPhone.addTextChangedListener(checkoutWatcher);
        etCheckoutLocation.addTextChangedListener(checkoutWatcher);
        setCheckoutFormExpanded(true);

        loadBuyerProfile();
        loadCart();
    }

    private void loadBuyerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getUid())) {
            updateCheckoutAvailability();
            return;
        }
        userService.getProfileById(user.getUid(), new AsyncCrudService.ItemCallback<com.example.unimarket.data.model.User>() {
            @Override
            public void onSuccess(com.example.unimarket.data.model.User data) {
                if (!isAdded() || data == null) {
                    updateCheckoutAvailability();
                    return;
                }
                if (TextUtils.isEmpty(textOf(etCheckoutPhone)) && !TextUtils.isEmpty(data.getPhone())) {
                    etCheckoutPhone.setText(data.getPhone());
                }
                if (TextUtils.isEmpty(textOf(etCheckoutLocation)) && !TextUtils.isEmpty(data.getLocation())) {
                    etCheckoutLocation.setText(data.getLocation());
                }
                updateCheckoutAvailability();
            }

            @Override
            public void onError(String error) {
                updateCheckoutAvailability();
            }
        });
    }

    private void selectShipping(String method) {
        selectedShippingMethod = SHIPPING_EXPRESS.equals(method) ? SHIPPING_EXPRESS : SHIPPING_STANDARD;
        boolean standardSelected = SHIPPING_STANDARD.equals(selectedShippingMethod);
        bindShippingOption(
                btnShipStandard,
                ivShipStandardIcon,
                tvShipStandardTitle,
                tvShipStandardDetail,
                tvShipStandardFee,
                standardSelected
        );
        bindShippingOption(
                btnShipExpress,
                ivShipExpressIcon,
                tvShipExpressTitle,
                tvShipExpressDetail,
                tvShipExpressFee,
                !standardSelected
        );
        clearAppliedDiscount(false);
        bindTotal();
        updateCheckoutFormSummary();
    }

    private void bindShippingOption(View container, ImageView icon, TextView title,
                                    TextView detail, TextView fee, boolean selected) {
        if (container == null) {
            return;
        }
        container.setBackgroundColor(Color.TRANSPARENT);
        int primary = selected ? 0xFF21409A : 0xFF667085;
        if (icon != null) icon.setColorFilter(primary);
        if (title != null) title.setTextColor(primary);
        if (detail != null) detail.setTextColor(primary);
        if (fee != null) fee.setTextColor(primary);
    }

    private void applyDiscountCode() {
        String code = textOf(etDiscountCode);
        if (TextUtils.isEmpty(code)) {
            clearAppliedDiscount(true);
            showDiscountStatus("Nhập mã giảm trước khi áp dụng.", false);
            return;
        }
        double validationBase = cartSubtotal + shippingFee();
        btnApplyDiscount.setEnabled(false);
        discountCodeService.validateCode(code, validationBase, result -> {
            if (!isAdded()) {
                return;
            }
            btnApplyDiscount.setEnabled(true);
            if (result.isSuccess() && result.getData() != null) {
                DiscountCodeService.Validation validation = result.getData();
                appliedDiscountCode = validation.getDiscountCode();
                appliedDiscountAmount = validation.getAmount();
                showDiscountStatus("Đã áp dụng " + appliedDiscountCode.getCode() + ".", true);
            } else {
                clearAppliedDiscount(false);
                showDiscountStatus(result.getError(), false);
            }
            bindTotal();
        });
    }

    private void clearAppliedDiscount(boolean clearInput) {
        appliedDiscountCode = null;
        appliedDiscountAmount = 0d;
        if (clearInput && etDiscountCode != null) {
            etDiscountCode.setText("");
        }
        if (tvDiscountStatus != null) {
            tvDiscountStatus.setVisibility(View.GONE);
        }
    }

    private void showDiscountStatus(String message, boolean success) {
        tvDiscountStatus.setText(message);
        tvDiscountStatus.setTextColor(success ? 0xFF027A48 : 0xFFB42318);
        tvDiscountStatus.setVisibility(View.VISIBLE);
    }

    private void loadCart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmpty();
            return;
        }

        setLoading(true);
        cartService.getWithFilter("user_id", user.getUid(), new AsyncCrudService.ListCallback<Cart>() {
            @Override
            public void onSuccess(List<Cart> data) {
                if (!isAdded()) {
                    return;
                }
                if (data == null || data.isEmpty()) {
                    setLoading(false);
                    showEmpty();
                    return;
                }
                loadItems(data.get(0));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showEmpty();
            }
        });
    }

    private void loadItems(Cart cart) {
        cartItemService.getCartItemsByCartId(cart.getId(), new AsyncCrudService.ListCallback<CartItem>() {
            @Override
            public void onSuccess(List<CartItem> data) {
                if (!isAdded()) {
                    return;
                }
                List<CartItem> items = data != null ? data : new ArrayList<>();
                if (items.isEmpty()) {
                    setLoading(false);
                    showEmpty();
                    return;
                }
                loadProducts(items);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showEmpty();
            }
        });
    }

    private void loadProducts(List<CartItem> items) {
        List<CartItemAdapter.CartLine> lines = new ArrayList<>();
        final int[] pending = {items.size()};
        for (CartItem item : items) {
            productService.getById(item.getProduct_id(), new AsyncCrudService.ItemCallback<Product>() {
                @Override
                public void onSuccess(Product product) {
                    if (isPurchasable(product)) {
                        lines.add(new CartItemAdapter.CartLine(item, product));
                    }
                    finishOne();
                }

                @Override
                public void onError(String error) {
                    finishOne();
                }

                private void finishOne() {
                    pending[0]--;
                    if (pending[0] == 0 && isAdded()) {
                        setLoading(false);
                        adapter.submitList(lines);
                        bindTotal();
                        if (lines.isEmpty()) {
                            showEmpty();
                        } else {
                            showList();
                        }
                    }
                }
            });
        }
    }

    private void updateQuantity(CartItemAdapter.CartLine line, int quantity) {
        if (quantity <= 0) {
            removeLine(line);
            return;
        }
        int available = availableQuantity(line.product);
        if (quantity > available) {
            Toast.makeText(requireContext(), "Số lượng còn lại chỉ còn " + available + ".", Toast.LENGTH_SHORT).show();
            quantity = available;
        }
        clearAppliedDiscount(false);
        line.item.setQuantity(quantity);
        cartItemService.save(line.item, result -> loadCart());
    }

    private void removeLine(CartItemAdapter.CartLine line) {
        if (line == null || line.item == null || TextUtils.isEmpty(line.item.getId())) {
            return;
        }
        cartItemService.deleteById(line.item.getId(), new AsyncCrudService.BooleanCallback() {
            @Override
            public void onSuccess(boolean success) {
                loadCart();
            }

            @Override
            public void onError(String error) {
                loadCart();
            }
        });
    }

    private void checkout() {
        List<CartItemAdapter.CartLine> lines = adapter.currentItems();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || lines.isEmpty()) {
            return;
        }
        if (!isCheckoutInfoComplete()) {
            updateCheckoutAvailability();
            Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại và vị trí nhận hàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (CartItemAdapter.CartLine line : lines) {
            if (line.quantity() > availableQuantity(line.product)) {
                Toast.makeText(requireContext(), "Một sản phẩm trong giỏ không đủ số lượng.", Toast.LENGTH_SHORT).show();
                loadCart();
                return;
            }
        }

        setCheckoutLoading(true);
        final int[] pending = {lines.size()};
        final boolean[] failed = {false};
        final double shippingShare = lines.isEmpty() ? 0d : shippingFee() / lines.size();
        final double totalBeforeDiscount = cartSubtotal + shippingFee();
        for (CartItemAdapter.CartLine line : lines) {
            double discountShare = discountShareForLine(line, totalBeforeDiscount);
            saveOrder(user, line, shippingShare, discountShare, new CheckoutCallback() {
                @Override
                public void onComplete(boolean success) {
                    if (!success) {
                        failed[0] = true;
                    }
                    pending[0]--;
                    if (pending[0] == 0 && isAdded()) {
                        setCheckoutLoading(false);
                        if (!failed[0]) {
                            incrementDiscountUsageIfNeeded();
                            dismiss();
                            Navigation.findNavController(requireActivity(), R.id.controllerNavHost)
                                    .navigate(R.id.ordersFragment);
                        } else {
                            loadCart();
                        }
                    }
                }
            });
        }
    }

    private void saveOrder(FirebaseUser buyer, CartItemAdapter.CartLine line,
                           double shippingShare, double discountShare,
                           CheckoutCallback callback) {
        Product product = line.product;
        double price = product.getPrice() != null ? product.getPrice() : 0;
        double subtotal = price * line.quantity();
        Order order = new Order();
        order.setBuyer_id(buyer.getUid());
        order.setSeller_id(product.getSeller_id());
        order.setProduct_id(product.getId());
        order.setProduct_title(product.getTitle());
        order.setProduct_image_url(firstImage(product));
        order.setQuantity(line.quantity());
        order.setUnit_price(price);
        order.setSubtotal_price(subtotal);
        order.setShipping_fee(shippingShare);
        order.setSeller_amount(subtotal);
        order.setDiscount_code(appliedDiscountCode != null ? appliedDiscountCode.getCode() : null);
        order.setDiscount_amount(discountShare);
        order.setTotal_price(Math.max(0d, subtotal + shippingShare - discountShare));
        order.setBuyer_phone(textOf(etCheckoutPhone));
        order.setDelivery_location(textOf(etCheckoutLocation));
        order.setShipping_method(selectedShippingMethod);
        order.setBuyer_note(textOf(etSellerNote));
        order.setStatus("pending");
        String now = CartFlow.nowIsoUtc();
        order.setCreated_at(now);
        order.setUpdated_at(now);

        orderService.save(order, result -> {
            if (!result.isSuccess() || result.getData() == null) {
                callback.onComplete(false);
                return;
            }
            saveOrderItem(result.getData(), line, callback);
        });
    }

    private void saveOrderItem(Order order, CartItemAdapter.CartLine line, CheckoutCallback callback) {
        Product product = line.product;
        OrderItem item = new OrderItem();
        item.setId(CartFlow.stableDocId("order_item", order.getId(), product.getId()));
        item.setOrder_id(order.getId());
        item.setProduct_id(product.getId());
        item.setSeller_id(product.getSeller_id());
        item.setPrice(product.getPrice() != null ? product.getPrice() : 0);
        item.setQuantity(line.quantity());

        orderItemService.save(item, result -> {
            if (!result.isSuccess()) {
                callback.onComplete(false);
                return;
            }
            cartItemService.deleteById(line.item.getId(), new AsyncCrudService.BooleanCallback() {
                @Override
                public void onSuccess(boolean success) {
                    notifySellerNewOrder(order);
                    sendSellerNoteIfNeeded(order);
                    callback.onComplete(true);
                }

                @Override
                public void onError(String error) {
                    callback.onComplete(false);
                }
            });
        });
    }

    private void notifySellerNewOrder(Order order) {
        if (order == null || TextUtils.isEmpty(order.getSeller_id()) || TextUtils.isEmpty(order.getId())) {
            return;
        }
        FirebaseUser buyer = FirebaseAuth.getInstance().getCurrentUser();
        if (buyer != null && order.getSeller_id().equals(buyer.getUid())) {
            return;
        }

        Notification notification = new Notification();
        notification.setId(CartFlow.stableDocId("notif", order.getSeller_id(), order.getId(), "new_order"));
        notification.setUser_id(order.getSeller_id());
        notification.setTitle("Có đơn hàng mới");
        notification.setContent("Người mua vừa đặt \"" + safeTitle(order) + "\". Vào Đơn hàng để xác nhận.");
        notification.setType("order");
        notification.setTarget_id(order.getId());
        notification.setIs_read(false);
        notification.setCreated_at(CartFlow.nowIsoUtc());
        notificationService.save(notification, ignored -> {});
    }

    private boolean isPurchasable(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return false;
        }
        String status = product.getStatus() != null ? product.getStatus().toLowerCase(Locale.ROOT) : "";
        return availableQuantity(product) > 0
                && (status.isEmpty() || status.equals("active") || status.equals("available"));
    }

    private String firstImage(Product product) {
        if (product == null || product.getImage_urls() == null || product.getImage_urls().isEmpty()) {
            return null;
        }
        return product.getImage_urls().get(0);
    }

    private String safeTitle(Order order) {
        return order != null && !TextUtils.isEmpty(order.getProduct_title())
                ? order.getProduct_title()
                : "sản phẩm";
    }

    private void bindTotal() {
        double total = 0;
        for (CartItemAdapter.CartLine line : adapter.currentItems()) {
            total += line.total();
        }
        cartSubtotal = total;
        double shipping = adapter.currentItems().isEmpty() ? 0d : shippingFee();
        double discount = Math.min(appliedDiscountAmount, total + shipping);
        appliedDiscountAmount = Math.max(0d, discount);
        double payable = Math.max(0d, total + shipping - discount);

        tvCartSubtotal.setText("Tạm tính: " + money(total));
        tvCartShipping.setText("Phí ship: " + money(shipping));
        tvCartDiscount.setText("Giảm: " + money(discount));
        tvCartTotal.setText(money(payable));
        updateCheckoutAvailability();
    }

    private void showList() {
        rvCartItems.setVisibility(View.VISIBLE);
        layoutCartEmpty.setVisibility(View.GONE);
        layoutCartFooter.setVisibility(View.VISIBLE);
        layoutCartCheckoutBar.setVisibility(View.VISIBLE);
        updateCheckoutAvailability();
    }

    private void showEmpty() {
        adapter.submitList(new ArrayList<>());
        cartSubtotal = 0d;
        clearAppliedDiscount(false);
        tvCartSubtotal.setText("Tạm tính: " + money(0d));
        tvCartShipping.setText("Phí ship: " + money(0d));
        tvCartDiscount.setText("Giảm: " + money(0d));
        tvCartTotal.setText(money(0d));
        rvCartItems.setVisibility(View.GONE);
        layoutCartEmpty.setVisibility(View.VISIBLE);
        layoutCartFooter.setVisibility(View.GONE);
        layoutCartCheckoutBar.setVisibility(View.GONE);
        updateCheckoutAvailability();
    }

    private void setLoading(boolean loading) {
        progressCart.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvCartItems.setVisibility(loading ? View.GONE : rvCartItems.getVisibility());
    }

    private void setCheckoutLoading(boolean loading) {
        btnCartCheckout.setEnabled(!loading && isCheckoutInfoComplete() && !adapter.currentItems().isEmpty());
        btnCartCheckout.setAlpha(loading ? 0.65f : 1f);
        btnCartCheckout.setText(loading ? "Đang đặt..." : "Đặt hàng");
        if (!loading) {
            updateCheckoutAvailability();
        }
    }

    private int availableQuantity(Product product) {
        if (product == null) {
            return 0;
        }
        Integer quantity = product.getQuantity();
        return quantity != null ? Math.max(0, quantity) : 1;
    }

    private double shippingFee() {
        return SHIPPING_EXPRESS.equals(selectedShippingMethod) ? EXPRESS_SHIPPING_FEE : STANDARD_SHIPPING_FEE;
    }

    private double discountShareForLine(CartItemAdapter.CartLine line, double totalBeforeDiscount) {
        if (line == null || totalBeforeDiscount <= 0d || appliedDiscountAmount <= 0d) {
            return 0d;
        }
        int lineCount = Math.max(1, adapter.currentItems().size());
        double lineBase = line.total() + (shippingFee() / lineCount);
        return Math.max(0d, Math.min(lineBase, appliedDiscountAmount * lineBase / totalBeforeDiscount));
    }

    private void incrementDiscountUsageIfNeeded() {
        if (appliedDiscountCode == null) {
            return;
        }
        int used = appliedDiscountCode.getUsed_count() != null ? appliedDiscountCode.getUsed_count() : 0;
        appliedDiscountCode.setUsed_count(used + 1);
        discountCodeService.save(appliedDiscountCode, ignored -> {});
    }

    private boolean isCheckoutInfoComplete() {
        return !TextUtils.isEmpty(textOf(etCheckoutPhone))
                && !TextUtils.isEmpty(textOf(etCheckoutLocation));
    }

    private void updateCheckoutAvailability() {
        if (btnCartCheckout == null || adapter == null) {
            return;
        }
        boolean enabled = isCheckoutInfoComplete() && !adapter.currentItems().isEmpty();
        btnCartCheckout.setEnabled(enabled);
        btnCartCheckout.setAlpha(enabled ? 1f : 0.55f);
        updateCheckoutFormSummary();
    }

    private void setCheckoutFormExpanded(boolean expanded) {
        checkoutFormExpanded = expanded;
        if (layoutCheckoutFields != null) {
            layoutCheckoutFields.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        if (ivCheckoutFormToggle != null) {
            ivCheckoutFormToggle.animate()
                    .rotation(expanded ? 0f : 180f)
                    .setDuration(160L)
                    .start();
        }
        updateCheckoutFormSummary();
    }

    private void updateCheckoutFormSummary() {
        if (tvCheckoutFormSummary == null) {
            return;
        }
        String phone = textOf(etCheckoutPhone);
        String location = textOf(etCheckoutLocation);
        if (TextUtils.isEmpty(phone) && TextUtils.isEmpty(location)) {
            tvCheckoutFormSummary.setText("Điền thông tin để người bán giao đúng hẹn");
            return;
        }
        StringBuilder summary = new StringBuilder();
        if (!TextUtils.isEmpty(phone)) {
            summary.append(phone);
        }
        if (!TextUtils.isEmpty(location)) {
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append(location);
        }
        if (summary.length() > 0) {
            summary.append(" · ");
        }
        summary.append(shippingLabel());
        tvCheckoutFormSummary.setText(summary.toString());
    }

    private String textOf(EditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String shippingLabel() {
        return SHIPPING_EXPRESS.equals(selectedShippingMethod) ? "Hỏa tốc" : "Giao thường";
    }

    private String money(double amount) {
        return amount <= 0d ? "0đ" : HomeUiUtils.formatPrice(amount);
    }

    private void sendSellerNoteIfNeeded(Order order) {
        String note = textOf(etSellerNote);
        if (order == null
                || TextUtils.isEmpty(note)
                || TextUtils.isEmpty(order.getProduct_id())
                || TextUtils.isEmpty(order.getBuyer_id())
                || TextUtils.isEmpty(order.getSeller_id())) {
            return;
        }

        FirebaseUser buyer = FirebaseAuth.getInstance().getCurrentUser();
        String buyerName = buyer != null && !TextUtils.isEmpty(buyer.getDisplayName())
                ? buyer.getDisplayName()
                : buyer != null && !TextUtils.isEmpty(buyer.getEmail()) ? buyer.getEmail() : "Người mua";
        String conversationId = ChatViewModel.buildProductConversationId(
                order.getProduct_id(),
                order.getBuyer_id(),
                order.getSeller_id()
        );
        String now = CartFlow.nowIsoUtc();
        String content = "Lời nhắc cho đơn " + shortOrderId(order) + ": " + note;

        Conversation conversation = new Conversation(conversationId, now);
        conversation.setUpdated_at(now);
        conversation.setProduct_id(order.getProduct_id());
        conversation.setProduct_title(safeTitle(order));
        conversation.setProduct_image_url(order.getProduct_image_url());
        conversation.setBuyer_id(order.getBuyer_id());
        conversation.setSeller_id(order.getSeller_id());
        conversation.setBuyer_name(buyerName);
        conversation.setSeller_name("Người bán");
        conversation.setLast_message(content);
        conversation.setLast_sender_id(order.getBuyer_id());
        conversation.setLast_message_at(now);

        db.collection("conversations")
                .document(conversationId)
                .set(conversation, SetOptions.merge())
                .addOnSuccessListener(unused -> db.collection("messages")
                        .add(new Message(null, conversationId, order.getBuyer_id(), content, now)));
    }

    private String shortOrderId(Order order) {
        String id = order != null ? order.getId() : null;
        if (TextUtils.isEmpty(id)) {
            return "#UM";
        }
        String compact = id.replaceAll("[^A-Za-z0-9]", "");
        if (compact.toLowerCase(Locale.ROOT).startsWith("order")) {
            compact = compact.substring(5);
        }
        if (TextUtils.isEmpty(compact)) {
            return "#UM";
        }
        return "#UM" + compact.substring(0, Math.min(6, compact.length())).toUpperCase(Locale.ROOT);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    private interface CheckoutCallback {
        void onComplete(boolean success);
    }
}
