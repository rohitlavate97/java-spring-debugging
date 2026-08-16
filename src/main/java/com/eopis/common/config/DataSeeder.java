package com.eopis.common.config;

import com.eopis.customer.entity.Address;
import com.eopis.customer.entity.Customer;
import com.eopis.customer.repository.CustomerRepository;
import com.eopis.inventory.entity.Inventory;
import com.eopis.inventory.entity.Warehouse;
import com.eopis.inventory.repository.InventoryRepository;
import com.eopis.inventory.repository.WarehouseRepository;
import com.eopis.product.entity.Category;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.CategoryRepository;
import com.eopis.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

    public DataSeeder(CustomerRepository customerRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      WarehouseRepository warehouseRepository,
                      InventoryRepository inventoryRepository) {
        this.customerRepository = customerRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Database already seeded with enterprise data. Skipping seeding.");
            return;
        }

        log.info("Starting enterprise data seeding (Phase 4)...");

        // 1. Categories
        Category electronics = new Category("Electronics", "CAT-ELEC", "Enterprise electronics and computing");
        Category accessories = new Category("Accessories", "CAT-ACC", "Peripherals and accessories");
        categoryRepository.save(electronics);
        categoryRepository.save(accessories);

        // 2. Products
        Product laptop = new Product("PROD-381", "Enterprise Developer Laptop 16-inch", "High performance workstation", new BigDecimal("1899.99"), "USD");
        laptop.setCategory(electronics);
        Product monitor = new Product("PROD-452", "4K Ultra-Wide Monitor 34-inch", "Ergonomic 4K display", new BigDecimal("649.50"), "USD");
        monitor.setCategory(electronics);
        Product keyboard = new Product("PROD-109", "Mechanical Low-Profile Keyboard", "RGB mechanical keyboard", new BigDecimal("129.99"), "USD");
        keyboard.setCategory(accessories);
        Product mouse = new Product("PROD-102", "Ergonomic Wireless Mouse", "Precision wireless mouse", new BigDecimal("49.99"), "USD");
        mouse.setCategory(accessories);

        productRepository.save(laptop);
        productRepository.save(monitor);
        productRepository.save(keyboard);
        productRepository.save(mouse);

        // 3. Warehouses
        Warehouse whEast = new Warehouse("WH-17", "Central Distribution Facility #17", "742 Evergreen Terrace, Springfield, IL");
        Warehouse whWest = new Warehouse("WH-04", "West Coast Fulfillment Hub #04", "100 Industrial Parkway, Seattle, WA");
        warehouseRepository.save(whEast);
        warehouseRepository.save(whWest);

        // 4. Inventory
        inventoryRepository.save(new Inventory(whEast, laptop, 150, 0));
        inventoryRepository.save(new Inventory(whEast, monitor, 300, 0));
        inventoryRepository.save(new Inventory(whEast, keyboard, 500, 0));
        inventoryRepository.save(new Inventory(whEast, mouse, 1000, 0));

        inventoryRepository.save(new Inventory(whWest, laptop, 75, 0));
        inventoryRepository.save(new Inventory(whWest, monitor, 120, 0));
        inventoryRepository.save(new Inventory(whWest, keyboard, 250, 0));
        inventoryRepository.save(new Inventory(whWest, mouse, 400, 0));

        // 5. Customers
        Customer c1 = new Customer("CUST-18291", "Eleanor", "Vance", "+1-555-0192", "VIP");
        c1.setUserId(UUID.randomUUID());
        Address addr1 = new Address("452 Elm Street", "Boston", "MA", "02108", "USA");
        addr1.setDefaultShipping(true);
        addr1.setDefaultBilling(true);
        c1.addAddress(addr1);

        Customer c2 = new Customer("CUST-49201", "Marcus", "Chen", "+1-555-0144", "ENTERPRISE");
        c2.setUserId(UUID.randomUUID());
        Address addr2 = new Address("88 Technology Drive", "San Jose", "CA", "95110", "USA");
        addr2.setDefaultShipping(true);
        addr2.setDefaultBilling(true);
        c2.addAddress(addr2);

        Customer c3 = new Customer("CUST-10382", "Sarah", "Connor", "+1-555-0177", "REGULAR");
        c3.setUserId(UUID.randomUUID());
        Address addr3 = new Address("12 Cyber Way", "Austin", "TX", "78701", "USA");
        addr3.setDefaultShipping(true);
        addr3.setDefaultBilling(true);
        c3.addAddress(addr3);

        customerRepository.save(c1);
        customerRepository.save(c2);
        customerRepository.save(c3);

        log.info("Enterprise data seeding completed successfully.");
    }
}
