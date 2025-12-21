package com.db2api.ui.api;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.persistent.connection.DbConnection;
import com.db2api.service.api.ApiDefinitionService;
import com.db2api.service.connection.ConnectionService;
import com.db2api.service.api.SchemaDiscoveryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.stream.Collectors;

/**
 * View for designing and building dynamic APIs.
 * This view allows users to select a database connection, choose a table,
 * and define columns and operations to be exposed as a REST or GraphQL API.
 */
@PageTitle("API Builder")
@Route(value = "api-builder", layout = MainLayout.class)
public class ApiBuilderView extends VerticalLayout {

    private final ApiDefinitionService apiDefinitionService;
    private final ConnectionService connectionService;
    private final SchemaDiscoveryService schemaDiscoveryService;

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
            SchemaDiscoveryService schemaDiscoveryService) {
        this.apiDefinitionService = apiDefinitionService;
        this.connectionService = connectionService;
        this.schemaDiscoveryService = schemaDiscoveryService;
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
            RadioButtonGroup<String> apiTypeSelect) {
        if (api == null) {
            editApiDefinition(apiDefinitionService.createNewApiDefinition(), grid, connectionSelect, tableSelect,
                    columnsSelect, operationsSelect, apiTypeSelect);
        } else {
            currentApiDefinition = api;
            if (api.getConnection() != null) {
                connectionSelect.setValue(api.getConnection());
                // Trigger table load
                if (api.getTableName() != null) {
                    tableSelect.setValue(api.getTableName());
                    // Trigger column load
                    // TODO: Parse included columns string and set selection
                }
            }
            if (api.getApiType() != null)
                apiTypeSelect.setValue(api.getApiType());
            // TODO: Parse allowed operations and set selection
        }
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
            RadioButtonGroup<String> apiTypeSelect) {
        if (currentApiDefinition == null)
            return;

        currentApiDefinition.setConnection(connectionSelect.getValue());
        currentApiDefinition.setTableName(tableSelect.getValue());
        currentApiDefinition.setApiType(apiTypeSelect.getValue());
        currentApiDefinition.setAllowedOperations(String.join(",", operationsSelect.getSelectedItems()));
        currentApiDefinition.setIncludedColumns(String.join(",", columnsSelect.getSelectedItems()));

        apiDefinitionService.saveApiDefinition(currentApiDefinition);
        updateList(grid);
        Notification.show("API Definition saved");
    }

    /**
     * Deletes the currently selected API definition.
     * 
     * @param grid             the grid to update
     * @param connectionSelect connection selection to reset
     * @param tableSelect      table selection to reset
     * @param columnsSelect    column selection to reset
     * @param operationsSelect operation selection to reset
     * @param apiTypeSelect    type selection to reset
     */
    private void delete(Grid<ApiDefinition> grid, ComboBox<DbConnection> connectionSelect, ComboBox<String> tableSelect,
            CheckboxGroup<String> columnsSelect, CheckboxGroup<String> operationsSelect,
            RadioButtonGroup<String> apiTypeSelect) {
        if (currentApiDefinition != null) {
            apiDefinitionService.deleteApiDefinition(currentApiDefinition);
            updateList(grid);
            editApiDefinition(null, grid, connectionSelect, tableSelect, columnsSelect, operationsSelect,
                    apiTypeSelect);
            Notification.show("API Definition deleted");
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

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        Button create = new Button("New API");

        setSizeFull();

        // Grid Configuration
        grid.addColumn(api -> api.getConnection() != null ? api.getConnection().getName() : "").setHeader("Connection");
        grid.addColumn(ApiDefinition::getTableName).setHeader("Table");
        grid.addColumn(ApiDefinition::getApiType).setHeader("Type");
        grid.asSingleSelect().addValueChangeListener(event -> editApiDefinition(event.getValue(), grid,
                connectionSelect, tableSelect, columnsSelect, operationsSelect, apiTypeSelect));

        // Interaction Buttons
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(
                event -> save(grid, connectionSelect, tableSelect, columnsSelect, operationsSelect, apiTypeSelect));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(
                event -> delete(grid, connectionSelect, tableSelect, columnsSelect, operationsSelect, apiTypeSelect));
        cancel.addClickListener(event -> editApiDefinition(null, grid, connectionSelect, tableSelect, columnsSelect,
                operationsSelect, apiTypeSelect));
        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editApiDefinition(apiDefinitionService.createNewApiDefinition(), grid, connectionSelect, tableSelect,
                    columnsSelect, operationsSelect, apiTypeSelect);
        });

        // Editor Form Logic - Cascading Selections
        connectionSelect.setItems(connectionService.getAllConnections());
        connectionSelect.setItemLabelGenerator(DbConnection::getName);
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
        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        VerticalLayout editorLayout = new VerticalLayout(
                create,
                connectionSelect,
                tableSelect,
                apiTypeSelect,
                operationsSelect,
                columnsSelect,
                buttons);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(grid, editorLayout);
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("API Builder"), splitLayout);

        // Initial State
        updateList(grid);

        // Role-Based Access Control
        if (isViewer()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
        }
    }
}
