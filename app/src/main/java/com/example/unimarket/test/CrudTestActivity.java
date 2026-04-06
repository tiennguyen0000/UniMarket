package com.example.unimarket.test;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.R;
import com.example.unimarket.data.service.CartService;
import com.example.unimarket.data.service.CategoryService;
import com.example.unimarket.data.service.ConversationService;
import com.example.unimarket.data.service.MessageService;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductImageService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.ReportService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.StudentVerificationService;
import com.example.unimarket.data.service.UserBehaviorService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.WishlistService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrudTestActivity extends AppCompatActivity {

    private Spinner spinnerTable;
    private EditText edtId;
    private TextView tvResult;
    private List<ServiceBinding> bindings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crud_test);

        spinnerTable = findViewById(R.id.spinnerTable);
        edtId = findViewById(R.id.edtId);
        tvResult = findViewById(R.id.tvResult);

        Button btnCreate = findViewById(R.id.btnCreate);
        Button btnRead = findViewById(R.id.btnRead);
        Button btnUpdate = findViewById(R.id.btnUpdate);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnList = findViewById(R.id.btnList);

        setupBindings();
        setupSpinner();

        btnCreate.setOnClickListener(v -> onCreateItem());
        btnRead.setOnClickListener(v -> onReadItem());
        btnUpdate.setOnClickListener(v -> onUpdateItem());
        btnDelete.setOnClickListener(v -> onDeleteItem());
        btnList.setOnClickListener(v -> onListItems());
    }

    private void setupBindings() {
        bindings = Arrays.asList(
                new ServiceBinding("users", new UserService(), com.example.unimarket.data.model.User.class),
                new ServiceBinding("categories", new CategoryService(), com.example.unimarket.data.model.Category.class),
                new ServiceBinding("products", new ProductService(), com.example.unimarket.data.model.Product.class),
                new ServiceBinding("carts", new CartService(), com.example.unimarket.data.model.Cart.class),
                new ServiceBinding("conversations", new ConversationService(), com.example.unimarket.data.model.Conversation.class),
                new ServiceBinding("messages", new MessageService(), com.example.unimarket.data.model.Message.class),
                new ServiceBinding("notifications", new NotificationService(), com.example.unimarket.data.model.Notification.class),
                new ServiceBinding("orders", new OrderService(), com.example.unimarket.data.model.Order.class),
                new ServiceBinding("product_images", new ProductImageService(), com.example.unimarket.data.model.ProductImage.class),
                new ServiceBinding("reports", new ReportService(), com.example.unimarket.data.model.Report.class),
                new ServiceBinding("reviews", new ReviewService(), com.example.unimarket.data.model.Review.class),
                new ServiceBinding("student_verifications", new StudentVerificationService(), com.example.unimarket.data.model.StudentVerification.class),
                new ServiceBinding("user_behavior", new UserBehaviorService(), com.example.unimarket.data.model.UserBehavior.class),
                new ServiceBinding("wishlist", new WishlistService(), com.example.unimarket.data.model.Wishlist.class)
        );
    }

    private void setupSpinner() {
        List<String> names = new ArrayList<>();
        for (ServiceBinding b : bindings) {
            names.add(b.tableName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTable.setAdapter(adapter);
    }

    private ServiceBinding currentBinding() {
        int pos = spinnerTable.getSelectedItemPosition();
        if (pos < 0 || pos >= bindings.size()) {
            return null;
        }
        return bindings.get(pos);
    }

    private void onCreateItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        try {
            Object model = binding.modelClass.getDeclaredConstructor().newInstance();
            Long inputId = parseId(false);
            if (inputId != null) {
                invokeSetter(model, "setId", Long.class, inputId);
            }
            fillSampleData(model, binding.tableName);

            boolean ok = (boolean) invokeMethod(binding.service, "create", new Class[]{binding.modelClass}, model);
            appendResult("CREATE " + binding.tableName + " -> " + ok + " | " + describeObject(model));
        } catch (Exception e) {
            showError(e);
        }
    }

    private void onReadItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        Long id = parseId(true);
        if (id == null) {
            return;
        }

        try {
            Object item = invokeMethod(binding.service, "getById", new Class[]{Long.class}, id);
            appendResult("READ " + binding.tableName + " id=" + id + " -> " + describeObject(item));
        } catch (Exception e) {
            showError(e);
        }
    }

    private void onUpdateItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        Long id = parseId(true);
        if (id == null) {
            return;
        }

        try {
            Object model = binding.modelClass.getDeclaredConstructor().newInstance();
            invokeSetter(model, "setId", Long.class, id);
            fillSampleData(model, binding.tableName + "_updated");

            boolean ok = (boolean) invokeMethod(binding.service, "update", new Class[]{binding.modelClass}, model);
            appendResult("UPDATE " + binding.tableName + " id=" + id + " -> " + ok + " | " + describeObject(model));
        } catch (Exception e) {
            showError(e);
        }
    }

    private void onDeleteItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        Long id = parseId(true);
        if (id == null) {
            return;
        }

        try {
            boolean ok = (boolean) invokeMethod(binding.service, "delete", new Class[]{Long.class}, id);
            appendResult("DELETE " + binding.tableName + " id=" + id + " -> " + ok);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void onListItems() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        try {
            Object result = invokeMethod(binding.service, "getAll", new Class[]{});
            if (result instanceof List) {
                List<?> list = (List<?>) result;
                appendResult("LIST " + binding.tableName + " size=" + list.size() + "\n" + describeList(list));
            } else {
                appendResult("LIST " + binding.tableName + " -> unexpected result");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void fillSampleData(Object model, String token) {
        Method[] methods = model.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (!name.startsWith("set") || method.getParameterCount() != 1 || "setId".equals(name)) {
                continue;
            }

            Class<?> type = method.getParameterTypes()[0];
            try {
                if (type == String.class) {
                    method.invoke(model, token + "_" + name.substring(3).toLowerCase());
                } else if (type == Long.class || type == long.class) {
                    method.invoke(model, 1L);
                } else if (type == Integer.class || type == int.class) {
                    method.invoke(model, 1);
                } else if (type == Double.class || type == double.class) {
                    method.invoke(model, 99.0);
                } else if (type == Boolean.class || type == boolean.class) {
                    method.invoke(model, true);
                }
            } catch (Exception ignored) {
                // Ignore unsupported setter types for quick test screen.
            }
        }
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private void invokeSetter(Object target, String setterName, Class<?> parameterType, Object value) throws Exception {
        Method method = target.getClass().getMethod(setterName, parameterType);
        method.invoke(target, value);
    }

    private Long parseId(boolean required) {
        String text = edtId.getText() != null ? edtId.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            if (required) {
                Toast.makeText(this, "Nhập ID trước", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ID phải là số", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void appendResult(String message) {
        String current = tvResult.getText() != null ? tvResult.getText().toString() : "";
        String next = TextUtils.isEmpty(current) ? message : current + "\n\n" + message;
        tvResult.setText(next);
    }

    private String describeList(List<?> list) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Object item : list) {
            sb.append(index++).append(". ").append(describeObject(item)).append("\n");
        }
        return sb.toString().trim();
    }

    private String describeObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(obj.getClass().getSimpleName()).append(" {");

        Method[] methods = obj.getClass().getMethods();
        boolean first = true;
        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            String name = method.getName();
            if ("getClass".equals(name)) {
                continue;
            }

            boolean isGetter = name.startsWith("get") || name.startsWith("is");
            if (!isGetter) {
                continue;
            }

            try {
                Object value = method.invoke(obj);
                String fieldName;
                if (name.startsWith("get")) {
                    fieldName = name.substring(3);
                } else {
                    fieldName = name.substring(2);
                }

                if (!first) {
                    sb.append(", ");
                }
                sb.append(fieldName).append("=").append(String.valueOf(value));
                first = false;
            } catch (Exception ignored) {
                // Ignore getter failures and continue rendering.
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private void showError(Exception e) {
        appendResult("ERROR: " + e.getMessage());
    }

    private static class ServiceBinding {
        private final String tableName;
        private final Object service;
        private final Class<?> modelClass;

        ServiceBinding(String tableName, Object service, Class<?> modelClass) {
            this.tableName = tableName;
            this.service = service;
            this.modelClass = modelClass;
        }
    }
}
