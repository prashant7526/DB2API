package com.db2api.ui.organization;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.service.organization.OrganizationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

/**
 * Manages API credential users for a selected organization client.
 */
public class ClientCredentialUsersPanel extends VerticalLayout {

    private final OrganizationService organizationService;
    private final ComboBox<Organization> organizationSelect = new ComboBox<>("Organization");
    private final ComboBox<OrganizationClient> clientSelect = new ComboBox<>("Client");
    private final TextField newUserName = new TextField("User name");
    private final ComboBox<ClientAuthenticationMode> newUserAuthMode =
            new ComboBox<>("Authentication type");
    private final Grid<Client> userGrid = new Grid<>(Client.class, false);

    public ClientCredentialUsersPanel(OrganizationService organizationService) {
        this.organizationService = organizationService;
        initUI();
    }

    private boolean isViewer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VIEWER"));
    }

    private void updateOrganizationClients(Organization organization) {
        clientSelect.clear();
        clientSelect.setEnabled(organization != null);
        clientSelect.setItems(organization == null
                ? List.of()
                : organizationService.getOrganizationClients(organization));
        updateUserList();
    }

    private void updateUserList() {
        OrganizationClient selectedClient = clientSelect.getValue();
        userGrid.setItems(selectedClient == null
                ? List.of()
                : organizationService.getClientUsers(selectedClient));
    }

    private void showCredentials(Client user, String credential, String credentialLabel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Save API user credentials");

        TextField clientId = new TextField("Client ID");
        clientId.setValue(user.getClientId());
        clientId.setReadOnly(true);
        clientId.setWidthFull();

        TextField secret = new TextField(credentialLabel);
        secret.setValue(credential);
        secret.setReadOnly(true);
        secret.setWidthFull();

        HorizontalLayout clientIdRow = new HorizontalLayout(clientId, createCopyButton(user.getClientId(), "Client ID"));
        HorizontalLayout credentialRow = new HorizontalLayout(secret, createCopyButton(credential, credentialLabel));
        clientIdRow.setWidthFull();
        credentialRow.setWidthFull();
        clientIdRow.setAlignItems(Alignment.END);
        credentialRow.setAlignItems(Alignment.END);

        dialog.add(new Paragraph("Copy and store these values now. The credential will not be shown again."));
        dialog.add(clientIdRow, credentialRow);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private Button createCopyButton(String value, String label) {
        return new Button("Copy", event -> getUI().ifPresent(ui -> ui.getPage()
                .executeJs("if (!navigator.clipboard) return false; "
                        + "return navigator.clipboard.writeText($0).then(() => true).catch(() => false);", value)
                .then(Boolean.class, copied -> Notification.show(Boolean.TRUE.equals(copied)
                        ? label + " copied"
                        : "Clipboard unavailable; select and copy the " + label + " value."))));
    }

    private void addUser() {
        OrganizationClient selectedClient = clientSelect.getValue();
        if (selectedClient == null) {
            Notification.show("Select an organization and client first");
            return;
        }
        if (newUserName.getValue() == null || newUserName.getValue().isBlank()) {
            newUserName.setInvalid(true);
            return;
        }
        newUserName.setInvalid(false);

        ClientAuthenticationMode authMode = newUserAuthMode.getValue();
        if (authMode == null) {
            Notification.show("Select an authentication type");
            return;
        }

        Client user = organizationService.createNewClient(selectedClient);
        user.setAuthenticationMode(authMode);
        try {
            String rawCredential = organizationService.saveClient(user, selectedClient, newUserName.getValue());
            newUserName.clear();
            updateUserList();
            if (rawCredential != null) {
                String credentialType = authMode == ClientAuthenticationMode.BEARER
                        ? "Bearer API token"
                        : "Client secret";
                showCredentials(user, rawCredential, credentialType);
            }
        } catch (IllegalStateException exception) {
            Notification.show(exception.getMessage());
        }
    }

    private void initUI() {
        setPadding(false);
        setWidthFull();

        organizationSelect.setItems(organizationService.getAllOrganizations());
        organizationSelect.setItemLabelGenerator(Organization::getName);
        organizationSelect.setRequiredIndicatorVisible(true);
        organizationSelect.addValueChangeListener(event -> updateOrganizationClients(event.getValue()));

        clientSelect.setItemLabelGenerator(OrganizationClient::getName);
        clientSelect.setRequiredIndicatorVisible(true);
        clientSelect.setEnabled(false);
        clientSelect.addValueChangeListener(event -> updateUserList());

        newUserAuthMode.setItems(ClientAuthenticationMode.values());
        newUserAuthMode.setItemLabelGenerator(ClientAuthenticationMode::getLabel);
        newUserAuthMode.setValue(ClientAuthenticationMode.OAUTH2);
        newUserAuthMode.setHelperText("These are API credentials, not admin passwords.");

        userGrid.addColumn(Client::getUserName).setHeader("User");
        userGrid.addComponentColumn(user -> {
            Checkbox active = new Checkbox(user.isActive());
            active.setAriaLabel("User active");
            active.addValueChangeListener(event -> {
                try {
                    organizationService.updateClientActive(user.getId(), event.getValue());
                    updateUserList();
                } catch (IllegalStateException exception) {
                    active.setValue(user.isActive());
                    Notification.show(exception.getMessage());
                }
            });
            active.setReadOnly(isViewer());
            return active;
        }).setHeader("Active");
        userGrid.addComponentColumn(user -> {
            TextField clientId = new TextField();
            clientId.setValue(user.getClientId());
            clientId.setReadOnly(true);
            clientId.setWidth("12em");
            HorizontalLayout row = new HorizontalLayout(clientId, createCopyButton(user.getClientId(), "Client ID"));
            row.setAlignItems(Alignment.CENTER);
            return row;
        }).setHeader("API credential");
        userGrid.addComponentColumn(user -> {
            Button rename = new Button("Rename", event -> {
                TextField name = new TextField("User name");
                name.setValue(user.getUserName());
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Rename user");
                Button save = new Button("Save", saveEvent -> {
                    if (name.getValue() == null || name.getValue().isBlank()) {
                        name.setInvalid(true);
                        return;
                    }
                    organizationService.renameClientUser(user, name.getValue());
                    updateUserList();
                    dialog.close();
                });
                save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                dialog.add(name);
                dialog.getFooter().add(save, new Button("Cancel", cancelEvent -> dialog.close()));
                dialog.open();
            });
            rename.setVisible(!isViewer());
            return rename;
        }).setHeader("User actions");
        userGrid.addComponentColumn(user -> {
            ComboBox<ClientAuthenticationMode> authMode = new ComboBox<>();
            authMode.setItems(ClientAuthenticationMode.values());
            authMode.setItemLabelGenerator(ClientAuthenticationMode::getLabel);
            authMode.setValue(user.getAuthenticationMode());
            authMode.addValueChangeListener(event -> {
                ClientAuthenticationMode selectedMode = event.getValue();
                if (selectedMode == null || selectedMode == user.getAuthenticationMode()) {
                    return;
                }
                String rawCredential =
                        organizationService.updateClientAuthenticationMode(user, selectedMode);
                updateUserList();
                String credentialType = selectedMode == ClientAuthenticationMode.BEARER
                        ? "Bearer API token"
                        : "New client secret";
                showCredentials(user, rawCredential, credentialType);
            });
            authMode.setReadOnly(isViewer());
            return authMode;
        }).setHeader("Authentication type");
        userGrid.setWidthFull();
        userGrid.setHeight("300px");

        Button addUser = new Button("Add User", event -> addUser());
        addUser.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout clientSelectors = new HorizontalLayout(organizationSelect, clientSelect);
        clientSelectors.setWidthFull();
        clientSelectors.setAlignItems(Alignment.BASELINE);
        HorizontalLayout addUserRow = new HorizontalLayout(newUserName, newUserAuthMode, addUser);
        addUserRow.setWidthFull();
        addUserRow.setAlignItems(Alignment.BASELINE);

        add(new H3("Users / API credentials"), clientSelectors, addUserRow, userGrid);

        if (isViewer()) {
            newUserName.setVisible(false);
            newUserAuthMode.setVisible(false);
            addUser.setVisible(false);
        }
    }
}
