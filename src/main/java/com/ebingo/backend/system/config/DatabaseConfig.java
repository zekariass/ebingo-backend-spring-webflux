package com.ebingo.backend.system.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing(auditorAwareRef = "auditorAware") // reactive auditing for R2DBC
public class DatabaseConfig {
}
