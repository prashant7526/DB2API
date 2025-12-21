package com.db2api.ui.organization;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.service.organization.OrganizationService;
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

/**
 * View for managing organizations and their associated API clients.
 * Allows creation, modification, and deletion of organizations and client
 * credentials.
 */
@PageTitle("Organizations")
@Route(value = "organizations", layout = MainLayout.class)
public class OrganizationView extends VerticalLayout {

    private final OrganizationService organizationService;

    private Organization currentOrganization;

    /**
     * Constructs the OrganizationView.
     * 
     * @param organizationService the service for managing organizations
     */
    public OrganizationView(OrganizationService organizationService) {
        this.organizationService = organizationService;
        initUI();
    }

    /**
     * Checks if the current user has the VIEWER role.
     * 
     * @return true if the user is a viewer, false otherwise
     */
    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
    }

    /**
     * Refreshes the organization list.
     * 
     * @param grid the grid to update
     */
    private void updateList(Grid<Organization> grid) {
        grid.setItems(organizationService.getAllOrganizations());
    }

    /**
     * Refreshes the client list for the current organization.
     * 
     * @param clientGrid the grid to update
     */
    private void updateClientList(Grid<Client> clientGrid) {
        if (currentOrganization != null) {
            clientGrid.setItems(organizationService.getClients(currentOrganization));
        } else {
            clientGrid.setItems();
        }
    }

    /**
     * Opens the editor for the specified organization.
     * 
     * @param organization the organization to edit
     * @param binder       the binder for form mapping
     * @param clientGrid   the grid of clients to update
     */
    private void editOrganization(Organization organization, Binder<Organization> binder, Grid<Client> clientGrid) {
        if (organization == null) {
            editOrganization(organizationService.createNewOrganization(), binder, clientGrid);
        } else {
            currentOrganization = organization;
            binder.setBean(organization);
            updateClientList(clientGrid);
        }
    }

    /**
     * Saves the current organization.
     * 
     * @param binder the binder containing form values
     * @param grid   the grid to refresh after saving
     */
    private void save(Binder<Organization> binder, Grid<Organization> grid) {
        if (binder.validate().isOk()) {
            organizationService.saveOrganization(currentOrganization);
            updateList(grid);
            Notification.show("Organization saved");
        }
    }

    /**
     * Deletes the current organization.
     * 
     * @param grid       the grid to refresh after deletion
     * @param binder     the binder to reset
     * @param clientGrid the client grid to reset
     */
    private void delete(Grid<Organization> grid, Binder<Organization> binder, Grid<Client> clientGrid) {
        if (currentOrganization != null) {
            organizationService.deleteOrganization(currentOrganization);
            updateList(grid);
            editOrganization(null, binder, clientGrid);
            Notification.show("Organization deleted");
        }
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        // Initialize Components
        Grid<Organization> grid = new Grid<>(Organization.class, false);
        Binder<Organization> binder = new Binder<>(Organization.class);

        TextField name = new TextField("Name");
        ComboBox<String> status = new ComboBox<>("Status");

        Grid<Client> clientGrid = new Grid<>(Client.class, false);

        Button save = new Button("Save");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        Button create = new Button("New Organization");
        Button addClient = new Button("Add Client");

        setSizeFull();

        // Grid Configuration
        grid.addColumn(Organization::getName).setHeader("Name");
        grid.addColumn(Organization::getStatus).setHeader("Status");
        grid.asSingleSelect().addValueChangeListener(event -> editOrganization(event.getValue(), binder, clientGrid));

        // Binder Configuration
        binder.bind(name, Organization::getName, Organization::setName);
        binder.bind(status, Organization::getStatus, Organization::setStatus);

        // Main Actions
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> save(binder, grid));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> delete(grid, binder, clientGrid));
        cancel.addClickListener(event -> editOrganization(null, binder, clientGrid));
        create.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editOrganization(organizationService.createNewOrganization(), binder, clientGrid);
        });

        // Client Management Actions
        addClient.addClickListener(e -> {
            if (currentOrganization != null && currentOrganization.getId() != null) {
                Client client = organizationService.createNewClient(currentOrganization);
                organizationService.saveClient(client, currentOrganization);
                updateClientList(clientGrid);
            } else {
                Notification.show("Save organization first");
            }
        });

        // Editor Layout Configuration
        status.setItems("Active", "Inactive", "Suspended");
        clientGrid.addColumn(Client::getClientId).setHeader("Client ID");
        clientGrid.addColumn(Client::getClientSecret).setHeader("Client Secret");
        clientGrid.addComponentColumn(client -> {
            Button deleteClient = new Button("Delete");
            deleteClient.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteClient.addClickListener(e -> {
                organizationService.deleteClient(client);
                updateClientList(clientGrid);
            });
            if (isViewer())
                deleteClient.setVisible(false);
            return deleteClient;
        });

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        VerticalLayout editorLayout = new VerticalLayout(
                create,
                new FormLayout(name, status),
                buttons,
                new H3("Clients"),
                clientGrid,
                addClient);
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);

        SplitLayout splitLayout = new SplitLayout(grid, editorLayout);
        splitLayout.setSplitterPosition(40);
        splitLayout.setSizeFull();

        add(new H2("Organization Management"), splitLayout);

        // Initial State
        updateList(grid);

        // Role-Based Access Control
        if (isViewer()) {
            save.setVisible(false);
            delete.setVisible(false);
            create.setVisible(false);
            addClient.setVisible(false);
        }
    }
}
