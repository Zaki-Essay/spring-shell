package com.example.nativecliapp;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HintsRegistrar implements RuntimeHintsRegistrar {

    //this configuration class is graalVM to get the file with desiared extension to be consedered
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("*.pdf");
        hints.resources().registerPattern("*.st");
        hints.resources().registerPattern("*.txt");
    }
}
