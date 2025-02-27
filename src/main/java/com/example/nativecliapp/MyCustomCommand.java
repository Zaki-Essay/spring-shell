package com.example.nativecliapp;

import com.example.nativecliapp.entities.Product;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Optional;

@ShellComponent
public class MyCustomCommand {
    private ProductRepository productRepository;

    public MyCustomCommand(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @ShellMethod(key = "hello")
    public String hello(@ShellOption(defaultValue = "me") String name,
                        @ShellOption(defaultValue = "1") int counter){
        StringBuilder stringBuilder= new StringBuilder();
        for (int i = 0; i < counter; i++) {
            stringBuilder.append("Hello "+name+" iteration "+i+" \n");
        }
        return stringBuilder.toString();
    }
    @ShellMethod(key = "test")
    public String test(){
        return "Test Shell";
    }
    @ShellMethod(key = "add")
    public String add(@ShellOption(defaultValue = "unknown") String name,
                      @ShellOption(defaultValue = "0") double price){
        Product product = productRepository.save(Product.builder()
                .name(name).price(price)
                .build());
        return product.toString();
    }
    @ShellMethod(key="findById")
    public String find(@ShellOption(defaultValue = "1") Long id){
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) return product.get().toString();
        else return "Product not found => "+id;
    }
}
