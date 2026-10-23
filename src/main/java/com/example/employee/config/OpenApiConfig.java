package com.example.employee.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Employee Management API")
                .description("REST API to create, view, update, delete, search and filter employees.")
                .version("1.0.0")
                .contact(new Contact().name("Employee Management Team")));
    }
}
