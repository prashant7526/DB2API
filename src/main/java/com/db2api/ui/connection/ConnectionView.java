package com.db2api.ui.connection;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.connection.DbConnection;
import com.db2api.service.connection.ConnectionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * View for managing database connections.
 * Allows users to register, test, and delete JDBC connections to various
 * databases.
 */
@PageTitle("Connections")
@Route(value = "connections", layout = MainLayout.class)
public class ConnectionView extends VerticalLayout {

    private final ConnectionService connectionService;

    private DbConnection currentConnection;

    /**
     * Constructs the ConnectionView.
     * 
     * @param connectionService the service for managing database connections
     */
    public ConnectionView(ConnectionService connectionService) {
        this.connectionService = connectionService;
        initUI();
    }

    /**
     * Checks if the currently authenticated user has the ADMIN role.
     * 
     * @return true if the user is an admin, false otherwise
     */
    private boolean isAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Refreshes the list of database connections.
     * 
     * @param grid the grid to update
     */
    private void updateList(Grid<DbConnection> grid) {
        grid.setItems(connectionService.getAllConnections());
    }

    /**
     * Opens the editor for the specified database connection.
     * 
     * @param connection the connection to edit
     * @param binder     the binder for form mapping
     */
    private void editConnection(DbConnection connection, Binder<DbConnection> binder) {
        if (connection == null) {
            editConnection(connectionService.createNewConnection(), binder);
        } else {
            currentConnection = connection;
            binder.setBean(connection);
        }
    }

    /**
     * Saves the current database connection.
     * 
     * @param binder the binder containing form values
     * @param grid   the grid to refresh after saving
     */
    private void save(Binder<DbConnection> binder, Grid<DbConnection> grid) {
        if (binder.validate().isOk()) {
            connectionService.saveConnection(currentConnection);
            updateList(grid);
            Notification.show("Connection saved");
        }
    }

    /**
     * Deletes the current database connection.
     * 
     * @param grid   the grid to refresh after deletion
     * @param binder the binder to reset
     */
    private void delete(Grid<DbConnection> grid, Binder<DbConnection> binder) {
        if (currentConnection != null) {
            connectionService.deleteConnection(currentConnection);
            updateList(grid);
            editConnection(null, binder);
            Notification.show("Connection deleted");
        }
    }

    /**
     * Tests the connectivity of the currently edited connection.
     * 
     * @param binder the binder used to validate form data before testing
     */
    private void testConnection(Binder<DbConnection> binder) {
        if (binder.validate().isOk()) {
            boolean valid = connectionService.testConnection(currentConnection);
            if (valid) {
                Notification.show("Connection Successful", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Connection Failed", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
            }
        }
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        // Initialize Components
        Grid<DbConnection> grid = new Grid<>(DbConnection.class, false);
        Binder<DbConnection> binder = new Binder<>(DbConnection.class);

        TextField name = new TextField("Name");
        TextField url = new TextField("JDBC URL");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        TextField driverClass = new TextField("Driver Class");

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        Button test = new Button("Test Connection");
        Button create = new Button("Add Connection");

        setSizeFull();

        // Grid Configuration
        grid.addColumn(DbConnection::getName).setHeader("Name");
        grid.addColumn(DbConnection::getUrl).setHeader("URL");
        grid.asSingleSelect().addValueChangeListener(event -> editConnection(event.getValue(), binder));

        // Binder Configuration
        binder.bind(name, DbConnection::getName, DbConnection::setName);
        binder.bind(url, DbConnection::getUrl, DbConnection::setUrl);
        binder.bind(username, DbConnection::getUsername, DbConnection::setUsername);
        binder.bind(password, DbConnection::getPassword, DbConnection::setPassword);
        binder.bind(driverClass, DbConnection::getDriverClass, DbConnection::setDriverClass);

        // Action Listeners
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> save(binder, grid));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> delete(grid, binder));
        cancel.addClickListener(event -> editConnection(null, binder));
        test.addClickListener(event -> testConnection(binder));
        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editConnection(connectionService.createNewConnection(), binder);
        });

        // Layout Assembly
        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel, test);
        HorizontalLayout toolbar = new HorizontalLayout(create);
        toolbar.addClassName("toolbar");

        VerticalLayout editorLayout = new VerticalLayout(
                toolbar,
                new FormLayout(name, url, username, password, driverClass),
                buttons);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(grid, editorLayout);
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("Database Connections"), splitLayout);

        // Initial State
        updateList(grid);

        // Role-Based Access Control
        if (!isAdmin()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
        }
    }
}
