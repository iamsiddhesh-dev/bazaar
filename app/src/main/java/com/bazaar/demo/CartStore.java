package com.bazaar.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart state.
 *
 * NOTE (seeded defect B1): an entry's quantity is only ever assigned by
 * syncWithServer(), which is not wired up in this build. add() inserts the
 * entry and then increments the quantity of the previously inserted entry,
 * which unboxes a null Integer and throws NullPointerException on the second
 * add of the same product.
 */
public final class CartStore {

    public static final class Entry {
        public Product product;
        public Integer qty;
    }

    public static final List<Entry> ITEMS = new ArrayList<>();

    public static void reset() {
        ITEMS.clear();
    }

    public static int count() {
        return ITEMS.size();
    }

    public static void add(Product p) {
        Entry existing = findById(p.id);

        Entry e = new Entry();
        e.product = p;
        // qty is intentionally left null here; syncWithServer() was meant to set it.
        ITEMS.add(e);

        if (existing != null) {
            existing.qty = existing.qty + 1; // B1: NullPointerException on 2nd add
        }
    }

    private static Entry findById(String id) {
        for (Entry e : ITEMS) {
            if (e.product.id.equals(id)) return e;
        }
        return null;
    }
}
