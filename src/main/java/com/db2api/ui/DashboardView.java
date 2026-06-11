package com.db2api.ui;

import com.db2api.service.api.ApiDefinitionService;
import com.db2api.service.connection.ConnectionService;
import com.db2api.service.organization.OrganizationService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Dashboard view providing an overview of the application with key metrics.
 */
@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    private final ApiDefinitionService apiDefinitionService;
    private final ConnectionService connectionService;
    private final OrganizationService organizationService;

    /**
     * Constructs the DashboardView.
     *
     * @param apiDefinitionService the service for API definitions
     * @param connectionService    the service for database connections
     * @param organizationService the service for organizations
     */
    public DashboardView(ApiDefinitionService apiDefinitionService,
            ConnectionService connectionService,
            OrganizationService organizationService) {
        this.apiDefinitionService = apiDefinitionService;
        this.connectionService = connectionService;
        this.organizationService = organizationService;
        initUI();
    }

    /**
     * Creates a stat card with a title and value.
     */
    private Div createStatCard(String title, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XXXLARGE);
        valueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        Paragraph titlePara = new Paragraph(title);
        titlePara.addClassName(LumoUtility.TextColor.SECONDARY);
        Div card = new Div(valueSpan, titlePara);
        card.addClassName(LumoUtility.Padding.LARGE);
        card.addClassName(LumoUtility.Border.ALL);
        card.addClassName(LumoUtility.BorderRadius.LARGE);
        card.getStyle().set("min-width", "150px");
        card.getStyle().set("text-align", "center");
        return card;
    }

    /**
     * Initializes the UI components and layout for the view.
     */
    private void initUI() {
        H2 title = new H2("DB2API Dashboard");

        int connectionCount = connectionService.getAllConnections().size();
        int apiCount = apiDefinitionService.getAllApiDefinitions().size();
        int orgCount = organizationService.getAllOrganizations().size();

        HorizontalLayout stats = new HorizontalLayout(
                createStatCard("Connections", String.valueOf(connectionCount)),
                createStatCard("API Definitions", String.valueOf(apiCount)),
                createStatCard("Organizations", String.valueOf(orgCount)));
        stats.setWidthFull();

        add(title, stats);
    }
}
