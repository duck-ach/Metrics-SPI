package com.precursor.metrics;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MetricsEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "pre-metrics-listener-spi";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MetricsEventListenerProvider(session);
    }

    /**
     * 초기 설정
     * @param config
     */
    @Override
    public void init(Config.Scope config) {}

    /**
     * 초기화 이후 설정
     * @param factory
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    /**
     * 종료 처리 필요 시
     */
    @Override
    public void close() {}

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
