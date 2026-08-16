package com.notificationengine.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fails fast, before any bean (including DataSource) is created, if a property listed in
 * app.required-secrets resolved to blank or missing. Runs as an EnvironmentPostProcessor
 * rather than a @PostConstruct bean specifically because Spring gives no ordering guarantee
 * between a plain @Component and auto-configured beans like DataSource — a @PostConstruct
 * check can silently run AFTER Hikari already tried (and failed) to connect. This class is
 * discovered via META-INF/spring.factories, independent of component scanning, so it applies
 * automatically to every module that depends on notification-common — no @ComponentScan
 * change needed for this specific check.
 */
public class RequiredSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String requiredSecretsCsv = environment.getProperty("app.required-secrets");
        if (requiredSecretsCsv == null || requiredSecretsCsv.isBlank()) {
            return;
        }

        List<String> blankOrMissing = new ArrayList<>();
        for (String key : requiredSecretsCsv.split(",")) {
            String trimmedKey = key.trim();
            if (trimmedKey.isEmpty()) continue;

            String value = environment.getProperty(trimmedKey);
            if (value == null || value.isBlank()) {
                blankOrMissing.add(trimmedKey);
            }
        }

        if (!blankOrMissing.isEmpty()) {
            throw new IllegalStateException(
                    "Required secret(s) resolved to blank or missing at startup — check that Vault "
                            + "secrets are actually populated (not just present as empty keys): " + blankOrMissing);
        }
    }

    @Override
    public int getOrder() {
        // Must run AFTER Spring Boot's own ConfigDataEnvironmentPostProcessor (which loads
        // application.properties AND resolves spring.config.import=vault://) — otherwise
        // Vault-sourced properties wouldn't be in the Environment yet when we check them.
        // LOWEST_PRECEDENCE places us last among EnvironmentPostProcessors, which is still
        // safely before any bean or ApplicationContext gets created.
        return Ordered.LOWEST_PRECEDENCE;
    }
}