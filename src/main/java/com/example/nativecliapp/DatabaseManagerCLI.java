package com.example.nativecliapp;

import com.example.nativecliapp.entities.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ShellComponent
public class DatabaseManagerCLI {

    private final ProductRepository productRepository;
    private final Logger logger = LoggerFactory.getLogger(DatabaseManagerCLI.class);

    public DatabaseManagerCLI(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🗄️  DATABASE MANAGEMENT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"db-status", "status"}, value = "📊 Display database connection status and statistics")
    public String databaseStatus() {
        try {
            long totalProducts = productRepository.count();
            return formatResponse("DATABASE STATUS",
                    "🟢 Connection: ACTIVE\n" +
                            "📦 Total Products: " + totalProducts + "\n" +
                            "🔗 Repository: " + productRepository.getClass().getSimpleName() + "\n" +
                            "⏰ Last Check: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Database connection failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"db-info", "info"}, value = "ℹ️  Show database schema and table information")
    public String databaseInfo() {
        return formatResponse("DATABASE INFORMATION",
                "📋 Schema: Products Management\n" +
                        "🏷️  Table: products\n" +
                        "📝 Columns: id (BIGINT), name (VARCHAR), price (DECIMAL)\n" +
                        "🔑 Primary Key: id\n" +
                        "🤖 Auto-increment: Enabled"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 📦 PRODUCT MANAGEMENT COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"create-product", "add", "insert"}, value = "➕ Create a new product record")
    public String createProduct(
            @ShellOption(value = {"-n", "--name"}, help = "Product name") String name,
            @ShellOption(value = {"-p", "--price"}, help = "Product price", defaultValue = "0.0") double price) {

        try {
            if (name.trim().isEmpty()) {
                return formatError("Invalid Input", "Product name cannot be empty");
            }

            Product product = Product.builder()
                    .name(name.trim())
                    .price(price)
                    .build();

            Product savedProduct = productRepository.save(product);

            return formatResponse("PRODUCT CREATED",
                    "✅ Successfully created product\n" +
                            "🆔 ID: " + savedProduct.getId() + "\n" +
                            "🏷️  Name: " + savedProduct.getName() + "\n" +
                            "💰 Price: $" + String.format("%.2f", savedProduct.getPrice()) + "\n" +
                            "📅 Created: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Creation Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"find-product", "get", "select"}, value = "🔍 Find product by ID")
    public String findProduct(
            @ShellOption(value = {"-i", "--id"}, help = "Product ID") Long id) {

        try {
            Optional<Product> product = productRepository.findById(id);

            if (product.isPresent()) {
                Product p = product.get();
                return formatResponse("PRODUCT FOUND",
                        "🆔 ID: " + p.getId() + "\n" +
                                "🏷️  Name: " + p.getName() + "\n" +
                                "💰 Price: $" + String.format("%.2f", p.getPrice())
                );
            } else {
                return formatWarning("Product Not Found", "No product exists with ID: " + id);
            }
        } catch (Exception e) {
            return formatError("Query Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"list-products", "list", "all"}, value = "📋 List all products with pagination")
    public String listProducts(
            @ShellOption(value = {"-l", "--limit"}, help = "Maximum number of records", defaultValue = "10") int limit,
            @ShellOption(value = {"-o", "--offset"}, help = "Starting record offset", defaultValue = "0") int offset) {

        try {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            Page<Product> products = productRepository.findAll(pageable);

            if (products.isEmpty()) {
                return formatWarning("No Products", "No products found in the database");
            }

            StringBuilder result = new StringBuilder();
            result.append("📊 PRODUCT LISTING\n");
            result.append("═".repeat(60)).append("\n");
            result.append(String.format("📄 Page %d of %d | Total: %d products\n\n",
                    products.getNumber() + 1, products.getTotalPages(), products.getTotalElements()));

            products.getContent().forEach(product -> {
                result.append(String.format("🆔 %-5d | 🏷️  %-20s | 💰 $%-10.2f\n",
                        product.getId(),
                        truncateString(product.getName(), 20),
                        product.getPrice()));
            });

            result.append("\n").append("═".repeat(60));
            return result.toString();
        } catch (Exception e) {
            return formatError("List Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"update-product", "update", "modify"}, value = "✏️  Update an existing product")
    public String updateProduct(
            @ShellOption(value = {"-i", "--id"}, help = "Product ID") Long id,
            @ShellOption(value = {"-n", "--name"}, help = "New product name", defaultValue = ShellOption.NULL) String name,
            @ShellOption(value = {"-p", "--price"}, help = "New product price", defaultValue = ShellOption.NULL) String priceStr) {

        try {
            Optional<Product> existingProduct = productRepository.findById(id);

            if (!existingProduct.isPresent()) {
                return formatWarning("Product Not Found", "No product exists with ID: " + id);
            }

            Product product = existingProduct.get();
            boolean updated = false;

            if (name != null && !name.trim().isEmpty()) {
                product.setName(name.trim());
                updated = true;
            }

            if (priceStr != null) {
                try {
                    double price = Double.parseDouble(priceStr);
                    product.setPrice(price);
                    updated = true;
                } catch (NumberFormatException e) {
                    return formatError("Invalid Price", "Price must be a valid number");
                }
            }

            if (!updated) {
                return formatWarning("No Changes", "No valid updates provided");
            }

            Product savedProduct = productRepository.save(product);

            return formatResponse("PRODUCT UPDATED",
                    "✅ Successfully updated product\n" +
                            "🆔 ID: " + savedProduct.getId() + "\n" +
                            "🏷️  Name: " + savedProduct.getName() + "\n" +
                            "💰 Price: $" + String.format("%.2f", savedProduct.getPrice()) + "\n" +
                            "📅 Updated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Update Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"delete-product", "delete", "remove"}, value = "🗑️  Delete a product by ID")
    public String deleteProduct(
            @ShellOption(value = {"-i", "--id"}, help = "Product ID") Long id,
            @ShellOption(value = {"-f", "--force"}, help = "Force deletion without confirmation", defaultValue = "false") boolean force) {

        try {
            Optional<Product> product = productRepository.findById(id);

            if (!product.isPresent()) {
                return formatWarning("Product Not Found", "No product exists with ID: " + id);
            }

            Product p = product.get();
            productRepository.deleteById(id);

            return formatResponse("PRODUCT DELETED",
                    "🗑️  Successfully deleted product\n" +
                            "🆔 ID: " + p.getId() + "\n" +
                            "🏷️  Name: " + p.getName() + "\n" +
                            "📅 Deleted: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Deletion Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"search-products", "search", "find"}, value = "🔎 Search products by name pattern")
    public String searchProducts(
            @ShellOption(value = {"-q", "--query"}, help = "Search query") String query,
            @ShellOption(value = {"-l", "--limit"}, help = "Maximum results", defaultValue = "10") int limit) {

        try {
            // Assuming you have a custom query method in your repository
            // List<Product> products = productRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, limit));

            // For now, using basic findAll and filtering
            List<Product> allProducts = productRepository.findAll();
            List<Product> filteredProducts = allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()))
                    .limit(limit)
                    .collect(Collectors.toList());

            if (filteredProducts.isEmpty()) {
                return formatWarning("No Results", "No products found matching: " + query);
            }

            StringBuilder result = new StringBuilder();
            result.append("🔍 SEARCH RESULTS\n");
            result.append("═".repeat(60)).append("\n");
            result.append(String.format("🔎 Query: '%s' | Found: %d products\n\n", query, filteredProducts.size()));

            filteredProducts.forEach(product -> {
                result.append(String.format("🆔 %-5d | 🏷️  %-20s | 💰 $%-10.2f\n",
                        product.getId(),
                        truncateString(product.getName(), 20),
                        product.getPrice()));
            });

            result.append("\n").append("═".repeat(60));
            return result.toString();
        } catch (Exception e) {
            return formatError("Search Failed", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🧹 UTILITY COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════════════════

    @ShellMethod(key = {"clear-db", "truncate"}, value = "🧹 Clear all products from database")
    public String clearDatabase(
            @ShellOption(value = {"-f", "--force"}, help = "Force clearing without confirmation", defaultValue = "false") boolean force) {

        if (!force) {
            return formatWarning("Confirmation Required",
                    "This will delete ALL products from the database.\n" +
                            "Use --force flag to confirm: clear-db --force");
        }

        try {
            long count = productRepository.count();
            productRepository.deleteAll();

            return formatResponse("DATABASE CLEARED",
                    "🧹 Successfully cleared database\n" +
                            "📊 Deleted Records: " + count + "\n" +
                            "📅 Cleared: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        } catch (Exception e) {
            return formatError("Clear Failed", e.getMessage());
        }
    }

    @ShellMethod(key = {"help-db", "db-help"}, value = "❓ Show database management help")
    public String showHelp() {
        return "🗄️  DATABASE MANAGEMENT CLI\n" +
                "═".repeat(60) + "\n" +
                "📊 Status Commands:\n" +
                "  status, db-status     - Show database status\n" +
                "  info, db-info         - Show database information\n\n" +
                "📦 Product Commands:\n" +
                "  add, create-product   - Create new product\n" +
                "  get, find-product     - Find product by ID\n" +
                "  list, list-products   - List all products\n" +
                "  update, update-product - Update existing product\n" +
                "  delete, delete-product - Delete product by ID\n" +
                "  search, search-products - Search products by name\n\n" +
                "🧹 Utility Commands:\n" +
                "  clear-db, truncate    - Clear all products\n" +
                "  help-db, db-help      - Show this help\n\n" +
                "💡 Use 'help <command>' for detailed command help\n" +
                "═".repeat(60);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // 🎨 FORMATTING UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════════

    private String formatResponse(String title, String content) {
        return String.format("✅ %s\n%s\n%s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                content);
    }

    private String formatError(String title, String message) {
        return String.format("❌ %s\n%s\n🔥 %s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                message);
    }

    private String formatWarning(String title, String message) {
        return String.format("⚠️  %s\n%s\n💡 %s",
                title,
                "─".repeat(Math.max(title.length(), 40)),
                message);
    }

    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
