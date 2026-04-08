package com.example.unimarket.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.unimarket.data.model.Cart;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Conversation;
import com.example.unimarket.data.model.Message;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.ProductImage;
import com.example.unimarket.data.model.Report;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.StudentVerification;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.model.UserBehavior;
import com.example.unimarket.data.model.Wishlist;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class CrudServicesTest {

    @Test
    public void allServicesShouldSupportCrud() throws Exception {
        List<Case> cases = Arrays.asList(
                new Case(new UserService(), User.class),
                new Case(new CategoryService(), Category.class),
                new Case(new ProductService(), Product.class),
                new Case(new CartService(), Cart.class),
                new Case(new ConversationService(), Conversation.class),
                new Case(new MessageService(), Message.class),
                new Case(new NotificationService(), Notification.class),
                new Case(new OrderService(), Order.class),
                new Case(new ProductImageService(), ProductImage.class),
                new Case(new ReportService(), Report.class),
                new Case(new ReviewService(), Review.class),
                new Case(new StudentVerificationService(), StudentVerification.class),
                new Case(new UserBehaviorService(), UserBehavior.class),
                new Case(new WishlistService(), Wishlist.class)
        );

        for (Case c : cases) {
            Object item = c.modelClass.getDeclaredConstructor().newInstance();

            boolean created = (boolean) call(c.service, "create", new Class[]{c.modelClass}, item);
            assertTrue("create() failed for " + c.service.getClass().getSimpleName(), created);

            Method getId = c.modelClass.getMethod("getId");
            Long id = (Long) getId.invoke(item);
            assertNotNull("ID should be generated for " + c.service.getClass().getSimpleName(), id);

            Object found = call(c.service, "getById", new Class[]{Long.class}, id);
            assertNotNull("getById() returned null for " + c.service.getClass().getSimpleName(), found);

            Object updatedItem = c.modelClass.getDeclaredConstructor().newInstance();
            Method setId = c.modelClass.getMethod("setId", Long.class);
            setId.invoke(updatedItem, id);

            boolean updated = (boolean) call(c.service, "update", new Class[]{c.modelClass}, updatedItem);
            assertTrue("update() failed for " + c.service.getClass().getSimpleName(), updated);

            List<?> all = (List<?>) call(c.service, "getAll", new Class[]{});
            assertTrue("getAll() should contain at least one row for " + c.service.getClass().getSimpleName(), all.size() >= 1);

            boolean deleted = (boolean) call(c.service, "delete", new Class[]{Long.class}, id);
            assertTrue("delete() failed for " + c.service.getClass().getSimpleName(), deleted);

            Object afterDelete = call(c.service, "getById", new Class[]{Long.class}, id);
            assertEquals("Row should not exist after delete for " + c.service.getClass().getSimpleName(), null, afterDelete);
        }
    }

    private Object call(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method m = target.getClass().getMethod(methodName, parameterTypes);
        return m.invoke(target, args);
    }

    private static class Case {
        private final Object service;
        private final Class<?> modelClass;

        private Case(Object service, Class<?> modelClass) {
            this.service = service;
            this.modelClass = modelClass;
        }
    }
}
