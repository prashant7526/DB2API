package com.db2api.ui.api;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.persistent.connection.DbConnection;
import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.service.api.ApiDefinitionService;
import com.db2api.service.connection.ConnectionService;
import com.db2api.service.api.SchemaDiscoveryService;
import com.db2api.service.organization.OrganizationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * View for designing and building dynamic APIs.
 * This view allows users to select a database connection, choose a table,
 * and define columns and operations to be exposed as a REST or GraphQL API.
 */
@PageTitle("API")
@Route(value = "api-builder", layout = MainLayout.class)
@PermitAll
public class ApiBuilderView extends VerticalLayout {

    private final ApiDefinitionService apiDefinitionService;
    private final ConnectionService connectionService;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final OrganizationService organizationService;

    private ApiDefinition currentApiDefinition;

    /**
     * Constructs the ApiBuilderView with necessary services.
     * 
     * @param apiDefinitionService   service for API definitions
     * @param connectionService      service for database connections
     * @param schemaDiscoveryService service for database schema exploration
     */
    public ApiBuilderView(ApiDefinitionService apiDefinitionService,
            ConnectionService connectionService,
            SchemaDiscoveryService schemaDiscoveryService,
            OrganizationService organizationService) {
        this.apiDefinitionService = apiDefinitionService;
        this.connectionService = connectionService;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.organizationService = organizationService;
        initUI();
    }

    /**
     * Checks if the current user has the VIEWER role.
     * 
     * @return true if visitor is limited to viewing
     */
    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
    }

    /**
     * Refreshes the list of API definitions.
     * 
     * @param grid the grid displaying APIs
     */
    private void updateList(Grid<ApiDefinition> grid) {
        grid.setItems(apiDefinitionService.getAllApiDefinitions());
    }

    /**
     * Opens the editor for the specified API definition.
     * 
     * @param api              the API to edit
     * @param grid             the grid for refresh
     * @param connectionSelect selection for database connection
     * @param tableSelect      selection for table
     * @param columnsSelect    selection for columns
     * @param operationsSelect selection for operations
     * @param apiTypeSelect    selection for API type
     */
    private void editApiDefinition(ApiDefinition api, Grid<ApiDefinition> grid, ComboBox<DbConnection> connectionSelect,
            ComboBox<String> tableSelect, CheckboxGroup<String> columnsSelect, CheckboxGroup<String> operationsSelect,
            RadioButtonGroup<String> apiTypeSelect, MultiSelectComboBox<Client> usersSelect, Checkbox active) {
        if (api == null) {
            api = apiDefinitionService.createNewApiDefinition();
        }
        currentApiDefinition = api;
        connectionSelect.clear();
        tableSelect.clear();
        columnsSelect.clear();
        operationsSelect.clear();
        apiTypeSelect.clear();
        if (api.getConnection() != null) {
            connectionSelect.setValue(api.getConnection());
            if (api.getTableName() != null) {
                tableSelect.setValue(api.getTableName());
            }
        }
        if (api.getApiType() != null) {
            apiTypeSelect.setValue(api.getApiType());
        }
        active.setValue(api.isActive());
        Set<Long> authorizedUserIds = apiDefinitionService.getAuthorizedUsers(api).stream()
                .map(Client::getId)
                .collect(Collectors.toSet());
        Set<Client> listedAuthorizedUsers = usersSelect.getListDataView().getItems()
                .filter(user -> authorizedUserIds.contains(user.getId()))
                .collect(Collectors.toSet());
        usersSelect.setValue(new HashSet<>(listedAuthorizedUsers));
    }

    /**
     * Saves the current API definition being edited.
     * 
     * @param grid             the grid to update
     * @param connectionSelect connection selection
     * @param tableSelect      table selection
     * @param columnsSelect    column selection
     * @param operationsSelect operation selection
     * @param apiTypeSelect    type selection
     */
    private void save(Grid<ApiDefinition> grid, ComboBox<DbConnection> connectionSelect, ComboBox<String> tableSelect,
            CheckboxGroup<String> columnsSelect, CheckboxGroup<String> operationsSelect,
            RadioButtonGroup<String> apiTypeSelect, MultiSelectComboBox<Client> usersSelect, Checkbox active) {
        if (currentApiDefinition == null)
            return;

        // Validate required fields
        if (connectionSelect.getValue() == null) {
            Notification.show("Please select a database connection", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (tableSelect.getValue() == null || tableSelect.getValue().isBlank()) {
            Notification.show("Please select a table", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (apiTypeSelect.getValue() == null || apiTypeSelect.getValue().isBlank()) {
            Notification.show("Please select an API type", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (operationsSelect.getSelectedItems().isEmpty()) {
            Notification.show("Please select at least one operation", 3000, Notification.Position.MIDDLE);
            return;
        }

        currentApiDefinition.setConnection(connectionSelect.getValue());
        currentApiDefinition.setTableName(tableSelect.getValue());
        currentApiDefinition.setApiType(apiTypeSelect.getValue());
        currentApiDefinition.setAllowedOperations(String.join(",", operationsSelect.getSelectedItems()));
        currentApiDefinition.setIncludedColumns(String.join(",", columnsSelect.getSelectedItems()));
        currentApiDefinition.setActive(active.getValue());

        try {
            apiDefinitionService.saveApiDefinition(currentApiDefinition, usersSelect.getSelectedItems().stream().toList());
            updateList(grid);
            Notification.show("API definition saved");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification.show(exception.getMessage());
        }
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        // Initialize Components
        Grid<ApiDefinition> grid = new Grid<>(ApiDefinition.class, false);

        ComboBox<DbConnection> connectionSelect = new ComboBox<>("Connection");
        ComboBox<String> tableSelect = new ComboBox<>("Table");
        CheckboxGroup<String> columnsSelect = new CheckboxGroup<>("Included Columns");
        CheckboxGroup<String> operationsSelect = new CheckboxGroup<>("Operations");
        RadioButtonGroup<String> apiTypeSelect = new RadioButtonGroup<>("API Type");
        MultiSelectComboBox<Client> usersSelect = new MultiSelectComboBox<>("Available to credential users");
        Checkbox active = new Checkbox("Active");

        Button save = new Button("Save");
        Button cancel = new Button("Cancel");
        Button create = new Button("New API");

        setWidthFull();

        // Grid Configuration
        grid.addColumn(api -> api.getConnection() != null ? api.getConnection().getName() : "").setHeader("Connection");
        grid.addColumn(ApiDefinition::getTableName).setHeader("Table");
        grid.addColumn(ApiDefinition::getApiType).setHeader("Type");
        grid.addColumn(api -> api.isActive() ? "Active" : "Inactive").setHeader("Status");
        grid.asSingleSelect().addValueChangeListener(event -> editApiDefinition(event.getValue(), grid,
                connectionSelect, tableSelect, columnsSelect, operationsSelect, apiTypeSelect, usersSelect, active));

        // Interaction Buttons
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(
                event -> save(grid, connectionSelect, tableSelect, columnsSelect, operationsSelect, apiTypeSelect,
                        usersSelect, active));
        cancel.addClickListener(event -> editApiDefinition(null, grid, connectionSelect, tableSelect, columnsSelect,
                operationsSelect, apiTypeSelect, usersSelect, active));
        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editApiDefinition(apiDefinitionService.createNewApiDefinition(), grid, connectionSelect, tableSelect,
                    columnsSelect, operationsSelect, apiTypeSelect, usersSelect, active);
        });

        // Editor Form Logic - Cascading Selections
        connectionSelect.setItems(connectionService.getAllConnections());
        usersSelect.setItems(organizationService.getAllClientUsers());
        usersSelect.setHelperText("An active API must have at least one active user assigned.");
        usersSelect.setItemLabelGenerator(user -> {
            OrganizationClient organizationClient = user.getOrganizationClient();
            String clientPath = organizationClient == null
                    ? "Unassigned"
                    : organizationClient.getOrganization().getName() + " / " + organizationClient.getName();
            String userName = user.getUserName() == null ? "API user" : user.getUserName();
            String status = user.isActiveInHierarchy() ? "active" : "inactive";
            return userName + " — " + clientPath + " (" + status + ", " + user.getClientId() + ")";
        });
        connectionSelect.setItemLabelGenerator(connection -> {
            OrganizationClient client = connection.getOrganizationClient();
            return client == null
                    ? connection.getName() + " (unassigned)"
                    : connection.getName() + " — "
                            + client.getOrganization().getName() + " / " + client.getName();
        });
        connectionSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                tableSelect.setItems(schemaDiscoveryService.getTables(e.getValue()));
            } else {
                tableSelect.setItems();
            }
        });

        tableSelect.addValueChangeListener(e -> {
            if (e.getValue() != null && connectionSelect.getValue() != null) {
                columnsSelect.setItems(schemaDiscoveryService.getColumns(connectionSelect.getValue(), e.getValue()));
                columnsSelect.select(schemaDiscoveryService.getColumns(connectionSelect.getValue(), e.getValue()));
            } else {
                columnsSelect.setItems();
            }
        });

        // Selection Items
        operationsSelect.setItems("GET", "PUT", "DELETE");
        apiTypeSelect.setItems("REST", "GraphQL");

        // Layout Assembly
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        VerticalLayout editorLayout = new VerticalLayout(
                create,
                connectionSelect,
                tableSelect,
                apiTypeSelect,
                operationsSelect,
                columnsSelect,
                usersSelect,
                active,
                buttons);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(grid, editorLayout);
        splitLayout.setSplitterPosition(40);
        splitLayout.setWidthFull();
        splitLayout.setHeight("70vh");
        splitLayout.setMinHeight("560px");

        add(new H2("API"), splitLayout);

        // Initial State
        updateList(grid);

        // Role-Based Access Control
        if (isViewer()) {
            save.setVisible(false);
            create.setVisible(false);
            usersSelect.setReadOnly(true);
            active.setReadOnly(true);
        }
    }
}
