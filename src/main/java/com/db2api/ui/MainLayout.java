package com.db2api.ui;

import com.db2api.ui.organization.OrganizationView;
import com.db2api.ui.connection.ConnectionView;
import com.db2api.ui.api.ApiBuilderView;
import com.db2api.ui.admin.AdminUserView;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main application layout that provides the common navigation interface.
 * Based on Vaadin AppLayout, it includes a navbar with a drawer toggle and a
 * side navigation menu.
 */
public class MainLayout extends AppLayout {

    /**
     * Constructs the MainLayout.
     */
    public MainLayout() {
        initUI();
    }

    /**
     * Checks if the currently authenticated user has administrative privileges.
     * 
     * @return true if the user is an admin, false otherwise
     */
    private boolean isAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                &&
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ========================== Below are the UI Initialization Code
    // ==========================
    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        // Navbar Configuration
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("DB2API Admin");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        // Side Navigation Configuration
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Organizations", OrganizationView.class, VaadinIcon.BUILDING.create()));
        nav.addItem(new SideNavItem("Connections", ConnectionView.class, VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem("API Builder", ApiBuilderView.class, VaadinIcon.MAGIC.create()));

        // Role-Based Navigation
        if (isAdmin()) {
            nav.addItem(new SideNavItem("Admin Users", AdminUserView.class, VaadinIcon.USERS.create()));
        }

        // Layout Assembly
        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToNavbar(toggle, title);
        addToDrawer(scroller);
    }
}
