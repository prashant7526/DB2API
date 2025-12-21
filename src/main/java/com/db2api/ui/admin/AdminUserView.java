package com.db2api.ui.admin;

import com.db2api.ui.MainLayout;

import com.db2api.persistent.admin.AdminUser;
import com.db2api.service.admin.AdminUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * View for managing administrative users.
 * Provides functionality to list, add, edit, and delete admin users with role
 * assignments.
 */
@Route(value = "admin-users", layout = MainLayout.class)
@PageTitle("Admin Users | DB2API")
@RolesAllowed("ADMIN")
public class AdminUserView extends VerticalLayout {

    private final AdminUserService adminUserService;

    private AdminUser currentUser;

    /**
     * Constructs the AdminUserView.
     * 
     * @param adminUserService the service for managing admin users
     */
    public AdminUserView(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
        initUI();
    }

    /**
     * Initializes the state for adding a new user.
     * 
     * @param grid     the grid displaying the users
     * @param binder   the binder for form mapping
     * @param password the password field to be cleared
     */
    private void addUser(Grid<AdminUser> grid, Binder<AdminUser> binder, PasswordField password) {
        grid.asSingleSelect().clear();
        editUser(adminUserService.createNewUser(), binder, password);
    }

    /**
     * Opens the user editor for the specified user.
     * 
     * @param user     the user to edit
     * @param binder   the binder for form mapping
     * @param password the password field to be cleared
     */
    private void editUser(AdminUser user, Binder<AdminUser> binder, PasswordField password) {
        if (user == null) {
            closeEditor(binder, password);
        } else {
            currentUser = user;
            binder.setBean(user);
            password.clear(); // Don't show hash
            // setVisible(true); // Assuming the main layout handles visibility or it's
            // always visible
        }
    }

    /**
     * Resets the editor state.
     * 
     * @param binder   the binder to clear
     * @param password the password field to clear
     */
    private void closeEditor(Binder<AdminUser> binder, PasswordField password) {
        currentUser = null;
        binder.setBean(null);
        password.clear();
    }

    /**
     * Saves the current user being edited.
     * 
     * @param binder   the binder containing form values
     * @param password the password field to retrieve the password from
     * @param grid     the grid to refresh after saving
     */
    private void saveUser(Binder<AdminUser> binder, PasswordField password, Grid<AdminUser> grid) {
        if (currentUser != null) {
            try {
                binder.writeBean(currentUser);
                adminUserService.saveUser(currentUser, password.getValue());
                updateList(grid);
                closeEditor(binder, password);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the currently selected user.
     * 
     * @param grid     the grid to refresh after deletion
     * @param binder   the binder to reset
     * @param password the password field to reset
     */
    private void deleteUser(Grid<AdminUser> grid, Binder<AdminUser> binder, PasswordField password) {
        if (currentUser != null) {
            adminUserService.deleteUser(currentUser);
            updateList(grid);
            closeEditor(binder, password);
        }
    }

    /**
     * Refreshes the user list from the service.
     * 
     * @param grid the grid to update
     */
    private void updateList(Grid<AdminUser> grid) {
        grid.setItems(adminUserService.getAllUsers());
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        // Initialize Components
        Grid<AdminUser> grid = new Grid<>(AdminUser.class);
        Binder<AdminUser> binder = new Binder<>(AdminUser.class);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        ComboBox<String> role = new ComboBox<>("Role");

        Button cancel = new Button("Cancel");
        Button save = new Button("Save");
        Button delete = new Button("Delete");

        addClassName("admin-user-view");
        setSizeFull();

        // Grid Configuration
        grid.addClassName("contact-grid");
        grid.setSizeFull();
        grid.setColumns("username", "role");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.asSingleSelect().addValueChangeListener(event -> editUser(event.getValue(), binder, password));

        // Form Configuration
        role.setItems("ADMIN", "EDITOR", "VIEWER");
        binder.bind(username, AdminUser::getUsername, AdminUser::setUsername);
        binder.bind(role, AdminUser::getRole, AdminUser::setRole);

        // Action Listeners
        save.addClickListener(e -> saveUser(binder, password, grid));
        delete.addClickListener(e -> deleteUser(grid, binder, password));
        cancel.addClickListener(e -> closeEditor(binder, password));

        // Toolbar Configuration
        Button addUserButton = new Button("Add User");
        addUserButton.addClickListener(click -> addUser(grid, binder, password));
        HorizontalLayout toolbar = new HorizontalLayout(addUserButton);
        toolbar.addClassName("toolbar");

        // Layout Assembly
        FormLayout formLayout = new FormLayout();
        formLayout.add(username, password, role);
        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        HorizontalLayout mainContent = new HorizontalLayout(grid, new VerticalLayout(formLayout, buttons));

        add(toolbar, mainContent);

        // Initial State
        updateList(grid);
        closeEditor(binder, password);
    }

}
