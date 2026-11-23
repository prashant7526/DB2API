package com.db2api.ui.connection;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.connection.DbConnection;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.service.connection.ConnectionService;
import com.db2api.service.connection.DatabaseType;
import com.db2api.service.organization.OrganizationService;
import com.db2api.ui.organization.ClientCredentialUsersPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;

/**
 * View for managing database connections.
 * Allows users to register, test, and delete JDBC connections to various
 * databases.
 */
@PageTitle("Users & Connections")
@Route(value = "users-connections", layout = MainLayout.class)
@RouteAlias(value = "connections", layout = MainLayout.class)
@PermitAll
public class ConnectionView extends VerticalLayout {

    private final ConnectionService connectionService;
    private final OrganizationService organizationService;

    private DbConnection currentConnection;
    private ComboBox<DatabaseType> databaseType;
    private ComboBox<Organization> organizationSelect;
    private ComboBox<OrganizationClient> organizationClientSelect;
    private TextField connectionName;
    private TextField host;
    private IntegerField port;
    private TextField databaseName;
    private TextField username;
    private PasswordField password;
    private boolean loadingForm;
    private boolean connectionAddressChanged;

    /**
     * Constructs the ConnectionView.
     * 
     * @param connectionService the service for managing database connections
     */
    public ConnectionView(ConnectionService connectionService, OrganizationService organizationService) {
        this.connectionService = connectionService;
        this.organizationService = organizationService;
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

    private void updateOrganizationClients(Organization organization) {
        organizationClientSelect.clear();
        organizationClientSelect.setEnabled(organization != null);
        organizationClientSelect.setItems(organization == null
                ? java.util.List.of()
                : organizationService.getOrganizationClients(organization));
    }

    /**
     * Opens the editor for the specified database connection.
     * 
     * @param connection the connection to edit
     * @param binder     the binder for form mapping
     */
    private void editConnection(DbConnection connection, Binder<DbConnection> binder) {
        boolean isNewConnection = connection == null || connection.getId() == null;
        currentConnection = isNewConnection ? connectionService.createNewConnection() : connection;

        loadingForm = true;
        binder.setBean(currentConnection);
        password.clear();
        if (isNewConnection) {
            databaseType.clear();
            connectionName.clear();
            host.clear();
            port.clear();
            databaseName.clear();
            username.clear();
            organizationSelect.clear();
            organizationClientSelect.clear();
            updateOrganizationClients(null);
            password.setHelperText("Optional; leave blank if this database has no password.");
            connectionAddressChanged = true;
        } else {
            DatabaseType detectedType = DatabaseType.fromDriverClass(currentConnection.getDriverClass()).orElse(null);
            databaseType.setValue(detectedType);
            if (detectedType != null) {
                try {
                    DatabaseType.ConnectionDetails details = detectedType.parseJdbcUrl(currentConnection.getUrl());
                    host.setValue(details.host());
                    port.setValue(details.port());
                    databaseName.setValue(details.database());
                } catch (IllegalArgumentException exception) {
                    databaseType.clear();
                    Notification.show("This saved connection cannot be opened in the guided editor.");
                }
            } else {
                host.clear();
                port.clear();
                databaseName.clear();
                Notification.show("This saved connection uses a database that is not in the guided list.");
            }
            connectionName.setValue(currentConnection.getName() == null ? "" : currentConnection.getName());
            username.setValue(currentConnection.getUsername() == null ? "" : currentConnection.getUsername());
            password.setHelperText("Leave blank to keep the saved password.");
            OrganizationClient savedClient = currentConnection.getOrganizationClient();
            Organization savedOrganization = savedClient == null ? null : savedClient.getOrganization();
            Organization selectedOrganization = savedOrganization == null
                    ? null
                    : organizationSelect.getListDataView().getItems()
                            .filter(item -> item.getId().equals(savedOrganization.getId()))
                            .findFirst()
                            .orElse(null);
            organizationSelect.setValue(selectedOrganization);
            updateOrganizationClients(selectedOrganization);
            OrganizationClient selectedClient = savedClient == null
                    ? null
                    : organizationClientSelect.getListDataView().getItems()
                            .filter(item -> item.getId().equals(savedClient.getId()))
                            .findFirst()
                            .orElse(null);
            organizationClientSelect.setValue(selectedClient);
            connectionAddressChanged = false;
        }
        updateDatabaseFields(databaseType.getValue());
        loadingForm = false;
    }

    /**
     * Saves the current database connection.
     * Uses the raw password from the password field to avoid double-encryption.
     *
     * @param binder the binder containing form values
     * @param grid   the grid to refresh after saving
     */
    private void save(Binder<DbConnection> binder, Grid<DbConnection> grid) {
        if (validateForm(binder) && prepareConnection()) {
            connectionService.saveConnection(currentConnection, password.getValue());
            updateList(grid);
            password.clear();
            connectionAddressChanged = false;
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
        if (validateForm(binder) && prepareConnection()) {
            boolean valid = connectionService.testConnection(currentConnection, password.getValue());
            if (valid) {
                Notification.show("Connection Successful", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Connection Failed", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private boolean validateForm(Binder<DbConnection> binder) {
        boolean valid = binder.validate().isOk();
        Organization selectedOrganization = organizationSelect.getValue();
        OrganizationClient selectedClient = organizationClientSelect.getValue();
        DatabaseType selectedType = databaseType.getValue();
        organizationSelect.setInvalid(selectedOrganization == null);
        organizationClientSelect.setInvalid(selectedClient == null
                || selectedOrganization == null
                || selectedClient.getOrganization() == null
                || !selectedOrganization.getId().equals(selectedClient.getOrganization().getId()));
        valid &= selectedOrganization != null && !organizationClientSelect.isInvalid();
        databaseType.setInvalid(selectedType == null);
        if (selectedType == null) {
            valid = false;
        } else {
            boolean needsServer = selectedType.isNetworkDatabase();
            boolean missingHost = needsServer && (host.getValue() == null || host.getValue().isBlank());
            boolean missingPort = needsServer
                    && (port.getValue() == null || port.getValue() < 1 || port.getValue() > 65535);
            boolean missingDatabase = databaseName.getValue() == null || databaseName.getValue().isBlank();
            boolean missingUsername = needsServer && (username.getValue() == null || username.getValue().isBlank());
            host.setInvalid(missingHost);
            port.setInvalid(missingPort);
            databaseName.setInvalid(missingDatabase);
            username.setInvalid(missingUsername);
            valid &= !missingHost && !missingPort && !missingDatabase && !missingUsername;
        }
        if (!valid) {
            Notification.show("Fill in the required connection details");
        }
        return valid;
    }

    private boolean prepareConnection() {
        DatabaseType selectedType = databaseType.getValue();
        try {
            if (connectionAddressChanged || currentConnection.getUrl() == null || currentConnection.getUrl().isBlank()) {
                currentConnection.setUrl(selectedType.buildJdbcUrl(host.getValue(), port.getValue(), databaseName.getValue()));
            }
            currentConnection.setDriverClass(selectedType.getDriverClass());
            currentConnection.setUsername(selectedType.isNetworkDatabase() ? username.getValue().trim() : "");
            currentConnection.setOrganizationClient(organizationClientSelect.getValue());
            return true;
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage());
            return false;
        }
    }

    private void updateDatabaseFields(DatabaseType selectedType) {
        boolean hasSelection = selectedType != null;
        boolean needsServer = hasSelection && selectedType.isNetworkDatabase();
        host.setVisible(needsServer);
        port.setVisible(needsServer);
        databaseName.setVisible(hasSelection);
        username.setVisible(needsServer);
        password.setVisible(needsServer);
        if (hasSelection) {
            databaseName.setLabel(selectedType.getDatabaseFieldLabel());
            databaseName.setHelperText(selectedType == DatabaseType.SQLITE
                    ? "Use the path to the SQLite database file."
                    : null);
        } else {
            databaseName.setLabel("Database");
            databaseName.setHelperText(null);
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

        databaseType = new ComboBox<>("Database type");
        databaseType.setItems(DatabaseType.values());
        databaseType.setItemLabelGenerator(DatabaseType::getLabel);
        databaseType.setRequiredIndicatorVisible(true);
        databaseType.setHelperText("Choose a database to see the connection details it needs.");
        organizationSelect = new ComboBox<>("Organization");
        organizationSelect.setItems(organizationService.getAllOrganizations());
        organizationSelect.setItemLabelGenerator(Organization::getName);
        organizationSelect.setRequiredIndicatorVisible(true);
        organizationClientSelect = new ComboBox<>("Client");
        organizationClientSelect.setItemLabelGenerator(OrganizationClient::getName);
        organizationClientSelect.setRequiredIndicatorVisible(true);
        organizationClientSelect.setEnabled(false);
        connectionName = new TextField("Connection name");
        connectionName.setRequiredIndicatorVisible(true);
        host = new TextField("Host name");
        host.setPlaceholder("e.g. db.example.com");
        host.setRequiredIndicatorVisible(true);
        port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65535);
        port.setStepButtonsVisible(true);
        port.setRequiredIndicatorVisible(true);
        databaseName = new TextField("Database");
        databaseName.setRequiredIndicatorVisible(true);
        username = new TextField("Username");
        username.setRequiredIndicatorVisible(true);
        password = new PasswordField("Password");
        password.setHelperText("Leave blank to keep the saved password.");

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        Button test = new Button("Test Connection");
        Button create = new Button("Add Connection");

        setWidthFull();

        // Grid Configuration
        grid.addColumn(DbConnection::getName).setHeader("Name");
        grid.addColumn(connection -> DatabaseType.fromDriverClass(connection.getDriverClass())
                .map(DatabaseType::getLabel).orElse("Unsupported database")).setHeader("Database");
        grid.addColumn(connection -> {
            OrganizationClient client = connection.getOrganizationClient();
            return client == null ? "Unassigned"
                    : client.getOrganization().getName() + " / " + client.getName();
        }).setHeader("Organization / client");
        grid.asSingleSelect().addValueChangeListener(event -> editConnection(event.getValue(), binder));

        // Password is handled separately to avoid double-encryption.
        binder.forField(connectionName).asRequired("Enter a connection name")
                .bind(DbConnection::getName, DbConnection::setName);
        binder.bind(username, DbConnection::getUsername, DbConnection::setUsername);

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
        databaseType.addValueChangeListener(event -> {
            if (!loadingForm && event.getValue() != null) {
                connectionAddressChanged = true;
                if (event.getOldValue() != null && event.getOldValue() != event.getValue()) {
                    host.clear();
                    databaseName.clear();
                    username.clear();
                    password.clear();
                    password.setHelperText("Optional for the new database; enter its password if needed.");
                    if (currentConnection != null) {
                        currentConnection.setPassword(null);
                    }
                }
                port.setValue(event.getValue().getDefaultPort());
                if (!event.getValue().isNetworkDatabase()) {
                    username.clear();
                    password.clear();
                }
            }
            updateDatabaseFields(event.getValue());
        });
        organizationSelect.addValueChangeListener(event -> {
            if (!loadingForm) {
                updateOrganizationClients(event.getValue());
            }
        });

        // Layout Assembly
        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel, test);
        HorizontalLayout toolbar = new HorizontalLayout(create);
        toolbar.addClassName("toolbar");

        VerticalLayout editorLayout = new VerticalLayout(
                toolbar,
                new FormLayout(
                        databaseType,
                        organizationSelect,
                        organizationClientSelect,
                        connectionName,
                        host,
                        port,
                        databaseName,
                        username,
                        password),
                buttons);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(grid, editorLayout);
        splitLayout.setSplitterPosition(40);
        splitLayout.setWidthFull();
        splitLayout.setHeight("60vh");
        splitLayout.setMinHeight("480px");

        add(new ClientCredentialUsersPanel(organizationService), new H2("Database Connections"), splitLayout);

        // Initial State
        updateList(grid);
        updateDatabaseFields(null);

        // Role-Based Access Control
        if (!isAdmin()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
        }
    }
}
