package com.precursor.metrics;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;

/**
 * 	Keycloak 이벤트 수신 및 PrometheusExporter 호출
 */
public class MetricsEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(MetricsEventListenerProvider.class);

    private final KeycloakSession session;

    public MetricsEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        RealmProvider realmProvider = session.realms();

        if (event == null || event.getType() == null) {
            logger.warn("Received null or unknown user event.");
            return;
        }

        logger.infof("✅ Received event: type=%s, realmId=%s, clientId=%s, error=%s, details=%s",
                event.getType(),
                event.getRealmId(),
                event.getClientId(),
                event.getError(),
                event.getDetails());

        switch (event.getType()) {
            case LOGIN -> PrometheusExporter.instance().recordLogin(event, realmProvider);
            case LOGIN_ERROR -> PrometheusExporter.instance().recordLoginError(event, realmProvider);
            case CLIENT_LOGIN -> PrometheusExporter.instance().recordClientLogin(event, realmProvider);
            case REGISTER -> PrometheusExporter.instance().recordRegistration(event, realmProvider);
            case REGISTER_ERROR -> PrometheusExporter.instance().recordRegistrationError(event, realmProvider);
            case REFRESH_TOKEN -> PrometheusExporter.instance().recordRefreshToken(event, realmProvider);
            case REFRESH_TOKEN_ERROR -> PrometheusExporter.instance().recordRefreshTokenError(event, realmProvider);
            case CODE_TO_TOKEN -> PrometheusExporter.instance().recordCodeToToken(event, realmProvider);
            case CODE_TO_TOKEN_ERROR -> PrometheusExporter.instance().recordCodeToTokenError(event, realmProvider);
            case CLIENT_LOGIN_ERROR -> PrometheusExporter.instance().recordClientLoginError(event, realmProvider);
            default -> PrometheusExporter.instance().recordGenericEvent(event, realmProvider);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        if (adminEvent == null || adminEvent.getOperationType() == null) {
            logger.warn("Received null or unknown admin event.");
            return;
        }

        logger.debugf("Processing admin event: %s", adminEvent.getOperationType());

        RealmProvider realmProvider = session.realms();
        PrometheusExporter.instance().recordGenericAdminEvent(adminEvent, realmProvider);
    }

    @Override
    public void close() {
        // No-op
    }
}