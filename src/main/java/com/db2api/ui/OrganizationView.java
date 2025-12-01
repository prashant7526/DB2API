package com.db2api.ui;

import com.db2api.persistent.Client;
import com.db2api.persistent.Organization;
import com.db2api.service.OrganizationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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

@PageTitle("Organizations")
@Route(value = "organizations", layout = MainLayout.class)
public class OrganizationView extends VerticalLayout {

    private final OrganizationService organizationService;
    private final Grid<Organization> grid = new Grid<>(Organization.class, false);
    private final Binder<Organization> binder = new Binder<>(Organization.class);

    private final TextField name = new TextField("Name");
    private final ComboBox<String> status = new ComboBox<>("Status");

    private final Grid<Client> clientGrid = new Grid<>(Client.class, false);

    private Organization currentOrganization;

    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button cancel = new Button("Cancel");
    private final Button create = new Button("New Organization");
    private final Button addClient = new Button("Add Client");

    public OrganizationView(OrganizationService organizationService) {
        this.organizationService = organizationService;
        setSizeFull();

        configureGrid();
        configureBinder();
        configureButtons();

        SplitLayout splitLayout = new SplitLayout(grid, createEditorLayout());
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("Organization Management"), splitLayout);

        updateList();
        applyRbac();
    }

    private void configureGrid() {
        grid.addColumn(Organization::getName).setHeader("Name");
        grid.addColumn(Organization::getStatus).setHeader("Status");
        grid.asSingleSelect().addValueChangeListener(event -> editOrganization(event.getValue()));
    }

    private void configureBinder() {
        binder.bind(name, Organization::getName, Organization::setName);
        binder.bind(status, Organization::getStatus, Organization::setStatus);
    }

    private void configureButtons() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> save());

        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> delete());

        cancel.addClickListener(event -> editOrganization(null));

        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editOrganization(organizationService.createNewOrganization());
        });

        addClient.addClickListener(e -> {
            if (currentOrganization != null && currentOrganization.getObjectId() != null
                    && !currentOrganization.getObjectId().isTemporary()) {
                Client client = organizationService.createNewClient(currentOrganization);
                organizationService.saveClient(client, currentOrganization);
                updateClientList();
            } else {
                Notification.show("Save organization first");
            }
        });
    }

    private void applyRbac() {
        if (isViewer()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
            addClient.setVisible(false);
        }
    }
    
    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
    }

    private VerticalLayout createEditorLayout() {
        status.setItems("Active", "Inactive", "Suspended");

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);

        // Client Section
        clientGrid.addColumn(Client::getClientId).setHeader("Client ID");
        clientGrid.addColumn(Client::getClientSecret).setHeader("Client Secret");
        clientGrid.addComponentColumn(client -> {
            Button deleteClient = new Button("Delete");
            deleteClient.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteClient.addClickListener(e -> {
                organizationService.deleteClient(client);
                updateClientList();
            });
            // RBAC for client delete
            if (isViewer()) deleteClient.setVisible(false);
            return deleteClient;
        });

        VerticalLayout editorLayout = new VerticalLayout(
                create,
                new FormLayout(name, status),
                buttons,
                new H3("Clients"),
                clientGrid,
                addClient);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);
        return editorLayout;
    }

    private void updateList() {
        grid.setItems(organizationService.getAllOrganizations());
    }

    private void updateClientList() {
        if (currentOrganization != null) {
            clientGrid.setItems(organizationService.getClients(currentOrganization));
        } else {
            clientGrid.setItems();
        }
    }

    private void editOrganization(Organization organization) {
        if (organization == null) {
            editOrganization(organizationService.createNewOrganization());
        } else {
            currentOrganization = organization;
            binder.setBean(organization);
            updateClientList();
        }
    }

    private void save() {
        if (binder.validate().isOk()) {
            organizationService.saveOrganization(currentOrganization);
            updateList();
            Notification.show("Organization saved");
        }
    }

    private void delete() {
        if (currentOrganization != null) {
            organizationService.deleteOrganization(currentOrganization);
            updateList();
            editOrganization(null);
            Notification.show("Organization deleted");
        }
    }
}
