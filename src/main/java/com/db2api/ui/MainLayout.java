package com.db2api.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("DB2API Admin");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        addToNavbar(toggle, title);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Organizations", OrganizationView.class, VaadinIcon.BUILDING.create()));
        nav.addItem(new SideNavItem("Connections", ConnectionView.class, VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem("API Builder", ApiBuilderView.class, VaadinIcon.MAGIC.create()));

        // Add Admin Users link only if user has ADMIN role
        // Note: In a real app, we'd check SecurityContextHolder
        // For now, let's add it and rely on @RolesAllowed to block access if clicked, 
        // or better, check role here.
        // Since I don't have easy access to SecurityContext here without more code, 
        // I'll add it and let the view handle access denial, or I can try to check.
        
        if (isAdmin()) {
            nav.addItem(new SideNavItem("Admin Users", AdminUserView.class, VaadinIcon.USERS.create()));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);
    }

    private boolean isAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null &&
               org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
