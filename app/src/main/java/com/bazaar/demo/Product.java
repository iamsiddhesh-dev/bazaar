package com.bazaar.demo;

import java.util.ArrayList;
import java.util.List;

public final class Product {

    public final String id;
    public final String name;
    public final String price;
    public final String desc;

    public Product(String id, String name, String price, String desc) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.desc = desc;
    }

    public static List<Product> catalog() {
        List<Product> l = new ArrayList<>();
        l.add(new Product("p1",
                "Handcrafted Mahogany Wardrobe with Brass Inlay (Limited Edition)",
                "Rs 48,900",
                "Solid mahogany, hand finished, brass inlay detail. Ships in 6 weeks."));
        l.add(new Product("p2", "Teak Dining Table", "Rs 32,500",
                "Six seater, reclaimed teak, natural oil finish."));
        l.add(new Product("p3", "Walnut Bookshelf", "Rs 18,200",
                "Five shelves, wall anchored, matte lacquer."));
        l.add(new Product("p4", "Oak Study Desk", "Rs 21,750",
                "Cable channel, two drawers, oak veneer."));
        l.add(new Product("p5", "Cane Lounge Chair", "Rs 12,400",
                "Hand woven cane, solid frame, cushion included."));
        l.add(new Product("p6", "Marble Side Table", "Rs 9,800",
                "Italian marble top, powder coated steel legs."));
        return l;
    }

    public static Product byId(String id) {
        for (Product p : catalog()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
