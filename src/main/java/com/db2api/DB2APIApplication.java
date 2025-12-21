package com.db2api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 * This class implements AppShellConfigurator to configure global application
 * settings for Vaadin.
 */
@SpringBootApplication
@Theme(value = "db2api")
public class DB2APIApplication implements AppShellConfigurator {

	/**
	 * Main method to start the Spring Boot application.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(DB2APIApplication.class, args);
	}

}
