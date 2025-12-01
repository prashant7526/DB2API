package com.db2api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme(value = "db2api")
public class DB2APIApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(DB2APIApplication.class, args);
	}

}
