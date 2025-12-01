package com.db2api.ui;

import com.db2api.persistent.DbConnection;
import com.db2api.service.ConnectionService;
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

@PageTitle("Connections")
@Route(value = "connections", layout = MainLayout.class)
public class ConnectionView extends VerticalLayout {

    private final ConnectionService connectionService;
    private final Grid<DbConnection> grid = new Grid<>(DbConnection.class, false);
    private final Binder<DbConnection> binder = new Binder<>(DbConnection.class);

    private final TextField name = new TextField("Name");
    private final TextField url = new TextField("JDBC URL");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final TextField driverClass = new TextField("Driver Class");

    private DbConnection currentConnection;

    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button cancel = new Button("Cancel");
    private final Button test = new Button("Test Connection");
    private final Button create = new Button("Add Connection");

    public ConnectionView(ConnectionService connectionService) {
        this.connectionService = connectionService;
        setSizeFull();

        configureGrid();
        configureButtons();

        SplitLayout splitLayout = new SplitLayout(grid, createEditorLayout());
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("Database Connections"), splitLayout);

        updateList();
        applyRbac();
    }

    private void configureGrid() {
        grid.addColumn(DbConnection::getName).setHeader("Name");
        grid.addColumn(DbConnection::getUrl).setHeader("URL");
        grid.asSingleSelect().addValueChangeListener(event -> editConnection(event.getValue()));
    }

    private void configureButtons() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> save());

        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> delete());

        cancel.addClickListener(event -> editConnection(null));

        test.addClickListener(event -> testConnection());

        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editConnection(connectionService.createNewConnection());
        });
    }

    private void applyRbac() {
        if (!isAdmin()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
        }
    }

    private VerticalLayout createEditorLayout() {
        binder.bindInstanceFields(this);

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel, test);

        VerticalLayout editorLayout = new VerticalLayout(
                getToolbar(),
                new FormLayout(name, url, username, password, driverClass),
                buttons);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);
        return editorLayout;
    }

    private HorizontalLayout getToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout(create);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private boolean isAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void updateList() {
        grid.setItems(connectionService.getAllConnections());
    }

    private void editConnection(DbConnection connection) {
        if (connection == null) {
            editConnection(connectionService.createNewConnection());
        } else {
            currentConnection = connection;
            binder.setBean(connection);
        }
    }

    private void save() {
        if (binder.validate().isOk()) {
            connectionService.saveConnection(currentConnection);
            updateList();
            Notification.show("Connection saved");
        }
    }

    private void delete() {
        if (currentConnection != null) {
            connectionService.deleteConnection(currentConnection);
            updateList();
            editConnection(null);
            Notification.show("Connection deleted");
        }
    }

    private void testConnection() {
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
}
