package com.example.unimarket.data.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FirestoreIdsTest {
    @Test
    public void stableDocId_shouldSanitizeUnsafeCharacters() {
        assertEquals("wishlist_user_1_product_2", FirestoreIds.stableDocId("wishlist", "user/1", "product#2"));
    }
}
