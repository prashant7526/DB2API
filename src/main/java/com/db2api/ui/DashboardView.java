package com.db2api.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Dashboard view providing a welcome message and overview of the application.
 */
@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    /**
     * Constructs the DashboardView.
     */
    public DashboardView() {
        initUI();
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        H2 title = new H2("Welcome to DB2API");
        Paragraph description = new Paragraph("Manage your database connections and APIs here.");
        add(title, description);
    }
}
