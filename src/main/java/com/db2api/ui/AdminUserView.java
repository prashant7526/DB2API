package com.db2api.ui;

import com.db2api.persistent.AdminUser;
import com.db2api.service.AdminUserService;
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

@Route(value = "admin-users", layout = MainLayout.class)
@PageTitle("Admin Users | DB2API")
@RolesAllowed("ADMIN")
public class AdminUserView extends VerticalLayout {

    private final AdminUserService adminUserService;
    private final Grid<AdminUser> grid = new Grid<>(AdminUser.class);
    private final Binder<AdminUser> binder = new Binder<>(AdminUser.class);
    
    private TextField username = new TextField("Username");
    private PasswordField password = new PasswordField("Password");
    private ComboBox<String> role = new ComboBox<>("Role");
    
    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");
    private Button delete = new Button("Delete");

    private AdminUser currentUser;

    public AdminUserView(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
        
        addClassName("admin-user-view");
        setSizeFull();
        
        configureGrid();
        configureForm();
        
        add(getToolbar(), new HorizontalLayout(grid, createFormLayout()));
        
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassName("contact-grid");
        grid.setSizeFull();
        grid.setColumns("username", "role");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        
        grid.asSingleSelect().addValueChangeListener(event -> editUser(event.getValue()));
    }

    private void configureForm() {
        role.setItems("ADMIN", "EDITOR", "VIEWER");
        
        binder.bind(username, AdminUser::getUsername, AdminUser::setUsername);
        binder.bind(role, AdminUser::getRole, AdminUser::setRole);
        // Password handled manually
        
        save.addClickListener(e -> saveUser());
        delete.addClickListener(e -> deleteUser());
        cancel.addClickListener(e -> closeEditor());
    }
    
    private HorizontalLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(username, password, role);
        
        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        return new HorizontalLayout(new VerticalLayout(formLayout, buttons));
    }

    private HorizontalLayout getToolbar() {
        Button addUserButton = new Button("Add User");
        addUserButton.addClickListener(click -> addUser());

        HorizontalLayout toolbar = new HorizontalLayout(addUserButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addUser() {
        grid.asSingleSelect().clear();
        editUser(adminUserService.createNewUser());
    }

    private void editUser(AdminUser user) {
        if (user == null) {
            closeEditor();
        } else {
            currentUser = user;
            binder.setBean(user);
            password.clear(); // Don't show hash
            setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        currentUser = null;
        binder.setBean(null);
        password.clear();
        // setVisible(false); // In a real app we'd hide the form
        // For simplicity, just clearing
    }

    private void saveUser() {
        if (currentUser != null) {
            try {
                binder.writeBean(currentUser);
                adminUserService.saveUser(currentUser, password.getValue());
                updateList();
                closeEditor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteUser() {
        if (currentUser != null) {
            adminUserService.deleteUser(currentUser);
            updateList();
            closeEditor();
        }
    }

    private void updateList() {
        grid.setItems(adminUserService.getAllUsers());
    }
}
