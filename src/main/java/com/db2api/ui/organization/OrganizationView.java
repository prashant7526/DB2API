package com.db2api.ui.organization;

import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.service.organization.OrganizationService;
import com.db2api.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.function.Consumer;

/**
 * View for managing organizations and their named clients.
 */
@PageTitle("Organizations & Clients")
@Route(value = "organizations", layout = MainLayout.class)
@PermitAll
public class OrganizationView extends VerticalLayout {

    private final OrganizationService organizationService;

    private Organization currentOrganization;

    public OrganizationView(OrganizationService organizationService) {
        this.organizationService = organizationService;
        initUI();
    }

    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VIEWER"));
    }

    private void updateOrganizationList(Grid<Organization> grid) {
        grid.setItems(organizationService.getAllOrganizations());
        grid.getDataProvider().refreshAll();
    }

    private void updateClientList(
            Grid<OrganizationClient> clientGrid, Long selectedClientId) {
        List<OrganizationClient> clients = currentOrganization == null
                ? List.of()
                : organizationService.getOrganizationClients(currentOrganization);
        clientGrid.setItems(clients);
        if (selectedClientId != null) {
            clients.stream()
                    .filter(client -> selectedClientId.equals(client.getId()))
                    .findFirst()
                    .ifPresent(clientGrid::select);
        }
    }

    private void editOrganization(
            Organization organization,
            Binder<Organization> binder,
            Grid<OrganizationClient> clientGrid,
            Button delete) {
        currentOrganization = organization == null
                ? organizationService.createNewOrganization()
                : organization;
        binder.setBean(currentOrganization);
        delete.setVisible(currentOrganization.getId() != null && !isViewer());
        updateClientList(clientGrid, null);
    }

    private void saveOrganization(
            Binder<Organization> binder, Grid<Organization> grid, ComboBox<String> status) {
        if (binder.validate().isOk()) {
            try {
                organizationService.saveOrganization(currentOrganization);
                updateOrganizationList(grid);
                Notification.show("Organization saved");
            } catch (IllegalStateException exception) {
                currentOrganization.setStatus("Active");
                status.setValue("Active");
                Notification.show(exception.getMessage());
            }
        }
    }

    private void deleteOrganization(
            Grid<Organization> grid,
            Binder<Organization> binder,
            Grid<OrganizationClient> clientGrid,
            Button deleteButton) {
        if (currentOrganization != null && currentOrganization.getId() != null) {
            try {
                organizationService.deleteOrganization(currentOrganization);
                updateOrganizationList(grid);
                editOrganization(null, binder, clientGrid, deleteButton);
                Notification.show("Organization deleted");
            } catch (IllegalStateException exception) {
                Notification.show(exception.getMessage());
            }
        }
    }

    private void showRenameDialog(String title, String value, Consumer<String> saveName) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        TextField name = new TextField("Name");
        name.setValue(value == null ? "" : value);
        name.setRequiredIndicatorVisible(true);
        name.setWidthFull();
        Button save = new Button("Save", event -> {
            if (name.getValue() == null || name.getValue().isBlank()) {
                name.setInvalid(true);
                return;
            }
            saveName.accept(name.getValue().trim());
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(name);
        dialog.getFooter().add(save, new Button("Cancel", event -> dialog.close()));
        dialog.open();
    }

    private void initUI() {
        Grid<Organization> organizationGrid = new Grid<>(Organization.class, false);
        Grid<OrganizationClient> clientGrid = new Grid<>(OrganizationClient.class, false);
        Binder<Organization> binder = new Binder<>(Organization.class);

        TextField organizationName = new TextField("Organization name");
        ComboBox<String> status = new ComboBox<>("Status");
        TextField newClientName = new TextField("Client name");

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        Button create = new Button("New Organization");
        Button addClient = new Button("Add Client");
        delete.setVisible(false);

        setWidthFull();

        organizationGrid.addColumn(Organization::getName).setHeader("Name");
        organizationGrid.addColumn(Organization::getStatus).setHeader("Status");
        organizationGrid.asSingleSelect().addValueChangeListener(event ->
                editOrganization(event.getValue(), binder, clientGrid, delete));

        clientGrid.addColumn(OrganizationClient::getName).setHeader("Client");
        clientGrid.addComponentColumn(client -> {
            Checkbox active = new Checkbox(client.isActive());
            active.setAriaLabel("Client active");
            active.addValueChangeListener(event -> {
                try {
                    organizationService.updateOrganizationClientActive(client.getId(), event.getValue());
                    updateClientList(clientGrid, client.getId());
                } catch (IllegalStateException exception) {
                    active.setValue(client.isActive());
                    Notification.show(exception.getMessage());
                }
            });
            active.setReadOnly(isViewer());
            return active;
        }).setHeader("Active");
        clientGrid.addComponentColumn(client -> {
            Button rename = new Button("Rename");
            rename.addClickListener(event -> showRenameDialog("Rename client", client.getName(), name -> {
                organizationService.saveOrganizationClient(client, name);
                updateClientList(clientGrid, client.getId());
                Notification.show("Client renamed");
            }));
            if (isViewer()) {
                rename.setVisible(false);
            }
            return rename;
        }).setHeader("Actions");
        binder.bind(organizationName, Organization::getName, Organization::setName);
        binder.bind(status, Organization::getStatus, Organization::setStatus);
        status.setItems("Active", "Inactive", "Suspended");

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> saveOrganization(binder, organizationGrid, status));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event ->
                deleteOrganization(organizationGrid, binder, clientGrid, delete));
        cancel.addClickListener(event -> editOrganization(null, binder, clientGrid, delete));
        create.addClickListener(event -> {
            organizationGrid.asSingleSelect().clear();
            editOrganization(organizationService.createNewOrganization(), binder, clientGrid, delete);
        });
        addClient.addClickListener(event -> {
            if (currentOrganization == null || currentOrganization.getId() == null) {
                Notification.show("Save the organization first");
                return;
            }
            if (newClientName.getValue() == null || newClientName.getValue().isBlank()) {
                newClientName.setInvalid(true);
                return;
            }
            newClientName.setInvalid(false);
            OrganizationClient client = organizationService.createOrganizationClient(currentOrganization);
            try {
                organizationService.saveOrganizationClient(client, newClientName.getValue());
                newClientName.clear();
                updateClientList(clientGrid, client.getId());
                Notification.show("Client added");
            } catch (IllegalStateException exception) {
                Notification.show(exception.getMessage());
            }
        });

        HorizontalLayout organizationButtons = new HorizontalLayout(save, delete, cancel);
        HorizontalLayout addClientRow = new HorizontalLayout(newClientName, addClient);
        VerticalLayout clientManagement = new VerticalLayout(
                new H3("Clients"),
                addClientRow,
                clientGrid);
        clientManagement.setPadding(false);

        VerticalLayout editorLayout = new VerticalLayout(
                create,
                new FormLayout(organizationName, status),
                organizationButtons,
                clientManagement);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(organizationGrid, editorLayout);
        splitLayout.setSplitterPosition(35);
        splitLayout.setWidthFull();
        splitLayout.setHeight("70vh");
        splitLayout.setMinHeight("560px");

        add(new H2("Organizations & Clients"), splitLayout);
        updateOrganizationList(organizationGrid);
        updateClientList(clientGrid, null);

        if (isViewer()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
            newClientName.setVisible(false);
            addClient.setVisible(false);
        }
    }
}
