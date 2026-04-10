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
import com.example.unimarket.network.SupabaseApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrudTestActivity extends AppCompatActivity {

    private Spinner spinnerTable;
    private EditText edtId;
    private TextView tvResult;
    private List<ServiceBinding> bindings;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
                new ServiceBinding("users", com.example.unimarket.data.model.User.class),
                new ServiceBinding("categories", com.example.unimarket.data.model.Category.class),
                new ServiceBinding("products", com.example.unimarket.data.model.Product.class),
                new ServiceBinding("carts", com.example.unimarket.data.model.Cart.class),
                new ServiceBinding("conversations", com.example.unimarket.data.model.Conversation.class),
                new ServiceBinding("messages", com.example.unimarket.data.model.Message.class),
                new ServiceBinding("notifications", com.example.unimarket.data.model.Notification.class),
                new ServiceBinding("orders", com.example.unimarket.data.model.Order.class),
                new ServiceBinding("product_images", com.example.unimarket.data.model.ProductImage.class),
                new ServiceBinding("reports", com.example.unimarket.data.model.Report.class),
                new ServiceBinding("reviews", com.example.unimarket.data.model.Review.class),
                new ServiceBinding("student_verifications", com.example.unimarket.data.model.StudentVerification.class),
                new ServiceBinding("user_behavior", com.example.unimarket.data.model.UserBehavior.class),
                new ServiceBinding("wishlist", com.example.unimarket.data.model.Wishlist.class)
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

            String inputId = parseId(false);
            if (inputId != null) {
                setIdIfExists(model, inputId);
            }

            fillSampleData(model, binding.tableName);

            String jsonBody = buildJsonBody(model, inputId != null);
            appendResult("CREATE " + binding.tableName + " -> sending...\n" + prettyJson(jsonBody));

            SupabaseApi.create(binding.tableName, jsonBody, new SupabaseApi.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() ->
                            appendResult("CREATE " + binding.tableName + " -> SUCCESS\n" + prettyJson(responseBody))
                    );
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            appendResult("CREATE " + binding.tableName + " -> ERROR\n" + error)
                    );
                }
            });

        } catch (Exception e) {
            showError(e);
        }
    }

    private void onReadItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        String id = parseId(true);
        if (id == null) {
            return;
        }

        appendResult("READ " + binding.tableName + " id=" + id + " -> sending...");

        SupabaseApi.getById(binding.tableName, id, new SupabaseApi.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                runOnUiThread(() ->
                        appendResult("READ " + binding.tableName + " id=" + id + " -> SUCCESS\n" + prettyJson(responseBody))
                );
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        appendResult("READ " + binding.tableName + " id=" + id + " -> ERROR\n" + error)
                );
            }
        });
    }

    private void onUpdateItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        String id = parseId(true);
        if (id == null) {
            return;
        }

        try {
            Object model = binding.modelClass.getDeclaredConstructor().newInstance();
            fillSampleData(model, binding.tableName + "_updated");

            String jsonBody = buildJsonBody(model, false);
            appendResult("UPDATE " + binding.tableName + " id=" + id + " -> sending...\n" + prettyJson(jsonBody));

            SupabaseApi.update(binding.tableName, id, jsonBody, new SupabaseApi.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() ->
                            appendResult("UPDATE " + binding.tableName + " id=" + id + " -> SUCCESS\n" + prettyJson(responseBody))
                    );
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            appendResult("UPDATE " + binding.tableName + " id=" + id + " -> ERROR\n" + error)
                    );
                }
            });

        } catch (Exception e) {
            showError(e);
        }
    }

    private void onDeleteItem() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        String id = parseId(true);
        if (id == null) {
            return;
        }

        appendResult("DELETE " + binding.tableName + " id=" + id + " -> sending...");

        SupabaseApi.delete(binding.tableName, id, new SupabaseApi.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                runOnUiThread(() ->
                        appendResult("DELETE " + binding.tableName + " id=" + id + " -> SUCCESS\n" + prettyJson(responseBody))
                );
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        appendResult("DELETE " + binding.tableName + " id=" + id + " -> ERROR\n" + error)
                );
            }
        });
    }

    private void onListItems() {
        ServiceBinding binding = currentBinding();
        if (binding == null) {
            return;
        }

        appendResult("LIST " + binding.tableName + " -> sending...");

        SupabaseApi.getAll(binding.tableName, new SupabaseApi.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                runOnUiThread(() ->
                        appendResult("LIST " + binding.tableName + " -> SUCCESS\n" + prettyJson(responseBody))
                );
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        appendResult("LIST " + binding.tableName + " -> ERROR\n" + error)
                );
            }
        });
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
                } else if (type == Float.class || type == float.class) {
                    method.invoke(model, 99.0f);
                } else if (type == Boolean.class || type == boolean.class) {
                    method.invoke(model, true);
                }
            } catch (Exception ignored) {
                // Bỏ qua setter không hỗ trợ trong màn test nhanh.
            }
        }
    }

    private void setIdIfExists(Object target, String value) {
        try {
            Method method = target.getClass().getMethod("setId", String.class);
            method.invoke(target, value);
            return;
        } catch (Exception ignored) {
        }

        // Legacy fallback if String version doesn't exist
        try {
            Method method = target.getClass().getMethod("setId", Long.class);
            method.invoke(target, value);
            return;
        } catch (Exception ignored) {
        }

        try {
            Method method = target.getClass().getMethod("setId", long.class);
            method.invoke(target, value);
        } catch (Exception ignored) {
            // Không có setId cũng không sao.
        }
    }

    private String buildJsonBody(Object model, boolean includeId) {
        JsonElement element = gson.toJsonTree(model);
        if (element != null && element.isJsonObject() && !includeId) {
            element.getAsJsonObject().remove("id");
        }
        return gson.toJson(element);
    }

    private String parseId(boolean required) {
        String text = edtId.getText() != null ? edtId.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            if (required) {
                Toast.makeText(this, "Nhập ID trước", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
        return text;  // UUID is a string, no need to parse as Long
    }

    private void appendResult(String message) {
        String current = tvResult.getText() != null ? tvResult.getText().toString() : "";
        String next = TextUtils.isEmpty(current) ? message : current + "\n\n" + message;
        tvResult.setText(next);
    }

    private String prettyJson(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "(empty response)";
        }

        try {
            JsonElement jsonElement = JsonParser.parseString(raw);
            return gson.toJson(jsonElement);
        } catch (Exception e) {
            return raw;
        }
    }

    private void showError(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        appendResult("ERROR: " + (cause.getMessage() != null ? cause.getMessage() : cause.toString()));
    }

    private static class ServiceBinding {
        private final String tableName;
        private final Class<?> modelClass;

        ServiceBinding(String tableName, Class<?> modelClass) {
            this.tableName = tableName;
            this.modelClass = modelClass;
        }
    }
}