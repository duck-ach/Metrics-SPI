package com.precursor.metrics;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * SPI 등록용 Factory (Keycloak SPI 표준 요구사항)
 */
public class MetricsResourceProviderFactory implements RealmResourceProviderFactory {
    // REST Endpoint
    public static final String ID = "metrics";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new MetricsResource();
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return ID;
    }
}
