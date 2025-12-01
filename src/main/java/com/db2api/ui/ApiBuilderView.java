package com.db2api.ui;

import com.db2api.persistent.ApiDefinition;
import com.db2api.persistent.DbConnection;
import com.db2api.service.ApiDefinitionService;
import com.db2api.service.ConnectionService;
import com.db2api.service.SchemaDiscoveryService;
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

@PageTitle("API Builder")
@Route(value = "api-builder", layout = MainLayout.class)
public class ApiBuilderView extends VerticalLayout {

    private final ApiDefinitionService apiDefinitionService;
    private final ConnectionService connectionService;
    private final SchemaDiscoveryService schemaDiscoveryService;

    private final Grid<ApiDefinition> grid = new Grid<>(ApiDefinition.class, false);

    private final ComboBox<DbConnection> connectionSelect = new ComboBox<>("Connection");
    private final ComboBox<String> tableSelect = new ComboBox<>("Table");
    private final CheckboxGroup<String> columnsSelect = new CheckboxGroup<>("Included Columns");
    private final CheckboxGroup<String> operationsSelect = new CheckboxGroup<>("Operations");
    private final RadioButtonGroup<String> apiTypeSelect = new RadioButtonGroup<>("API Type");

    private ApiDefinition currentApiDefinition;

    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button cancel = new Button("Cancel");
    private final Button create = new Button("New API");

    public ApiBuilderView(ApiDefinitionService apiDefinitionService,
            ConnectionService connectionService,
            SchemaDiscoveryService schemaDiscoveryService) {
        this.apiDefinitionService = apiDefinitionService;
        this.connectionService = connectionService;
        this.schemaDiscoveryService = schemaDiscoveryService;

        setSizeFull();

        configureGrid();
        configureButtons();

        SplitLayout splitLayout = new SplitLayout(grid, createEditorLayout());
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("API Builder"), splitLayout);

        updateList();
        applyRbac();
    }

    private void configureGrid() {
        grid.addColumn(api -> api.getConnection() != null ? api.getConnection().getName() : "").setHeader("Connection");
        grid.addColumn(ApiDefinition::getTableName).setHeader("Table");
        grid.addColumn(ApiDefinition::getApiType).setHeader("Type");
        grid.asSingleSelect().addValueChangeListener(event -> editApiDefinition(event.getValue()));
    }

    private void configureButtons() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> save());

        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> delete());

        cancel.addClickListener(event -> editApiDefinition(null));

        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editApiDefinition(apiDefinitionService.createNewApiDefinition());
        });
    }

    private void applyRbac() {
        if (isViewer()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
        }
    }
    
    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
    }

    private VerticalLayout createEditorLayout() {
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

        operationsSelect.setItems("GET", "PUT", "DELETE");
        apiTypeSelect.setItems("REST", "GraphQL");

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
        return editorLayout;
    }

    private void updateList() {
        grid.setItems(apiDefinitionService.getAllApiDefinitions());
    }

    private void editApiDefinition(ApiDefinition api) {
        if (api == null) {
            editApiDefinition(apiDefinitionService.createNewApiDefinition());
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

    private void save() {
        if (currentApiDefinition == null)
            return;

        currentApiDefinition.setConnection(connectionSelect.getValue());
        currentApiDefinition.setTableName(tableSelect.getValue());
        currentApiDefinition.setApiType(apiTypeSelect.getValue());
        currentApiDefinition.setAllowedOperations(String.join(",", operationsSelect.getSelectedItems()));
        currentApiDefinition.setIncludedColumns(String.join(",", columnsSelect.getSelectedItems()));

        apiDefinitionService.saveApiDefinition(currentApiDefinition);
        updateList();
        Notification.show("API Definition saved");
    }

    private void delete() {
        if (currentApiDefinition != null) {
            apiDefinitionService.deleteApiDefinition(currentApiDefinition);
            updateList();
            editApiDefinition(null);
            Notification.show("API Definition deleted");
        }
    }
}
