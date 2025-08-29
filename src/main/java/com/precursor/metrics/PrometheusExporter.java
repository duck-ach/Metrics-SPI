package com.precursor.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.common.TextFormat;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PrometheusExporter {

    private static final Logger LOGGER = Logger.getLogger(PrometheusExporter.class);
    private static final String USER_EVENT_PREFIX = "keycloak_user_event_";
    private static final String ADMIN_EVENT_PREFIX = "keycloak_admin_event_";
    private static final String PROVIDER_KEYCLOAK_OPENID = "keycloak";

    private static PrometheusExporter INSTANCE;
    private static boolean initialized = false;

    private final Map<String, Counter> counters = new HashMap<>();

    private Counter loginAttempts;
    private Counter successfulLogins;
    private Counter failedLogins;
    private Counter registrations;
    private Counter registrationErrors;
    private Counter refreshTokens;
    private Counter refreshTokenErrors;
    private Counter clientLogins;
    private Counter failedClientLogins;
    private Counter codeToTokens;
    private Counter codeToTokenErrors;
    private Counter responseTotal;
    private Counter responseErrors;
    private Histogram requestDuration;

    private PrometheusExporter() {
        synchronized (PrometheusExporter.class) {
            if (initialized) return;

            LOGGER.info("🔧 Initializing PrometheusExporter...");

            try {
                // 로그인 시도 횟수
                loginAttempts = Counter.build()
                        .name("keycloak_login_attempts_total")
                        .help("Total login attempts")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_login_attempts_total");
            }

            try {
                // 로그인 성공 횟수
                successfulLogins = Counter.build()
                        .name("keycloak_logins_total")
                        .help("Total successful logins")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_logins_total");
            }

            try {
                // 로그인 실패 횟수
                failedLogins = Counter.build()
                        .name("keycloak_failed_login_attempts_total")
                        .help("Total failed login attempts")
                        .labelNames("realm", "provider", "error", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_failed_login_attempts_total");
            }

            try {
                // 유저 등록 전체 수
                registrations = Counter.build()
                        .name("keycloak_registrations_total")
                        .help("Total user registrations")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_registrations_total");
            }

            try {
                // 등록(회원가입) 에러
                registrationErrors = Counter.build()
                        .name("keycloak_registrations_errors")
                        .help("Total user registration errors")
                        .labelNames("realm", "provider", "error", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_registrations_errors");
            }

            try {
                // Refresh Token
                refreshTokens = Counter.build()
                        .name("keycloak_refresh_tokens")
                        .help("Total refresh tokens")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_refresh_tokens");
            }

            try {
                // Refresh Token Error
                refreshTokenErrors = Counter.build()
                        .name("keycloak_refresh_tokens_errors")
                        .help("Total refresh token errors")
                        .labelNames("realm", "provider", "error", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_refresh_tokens_errors");
            }

            try {
                // Client 별 로그인 수
                clientLogins = Counter.build()
                        .name("keycloak_client_logins")
                        .help("Total successful client logins")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_client_logins");
            }

            try {
                // Client 별 로그인 실패 수
                failedClientLogins = Counter.build()
                        .name("keycloak_failed_client_login_attempts")
                        .help("Total failed client logins")
                        .labelNames("realm", "provider", "error", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_failed_client_login_attempts");
            }

            try {
                // Code - Token Exchanged
                codeToTokens = Counter.build()
                        .name("keycloak_code_to_tokens")
                        .help("Total successful code-to-token exchanges")
                        .labelNames("realm", "provider", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_code_to_tokens");
            }

            try {
                // Code - Token Exchanged Error
                codeToTokenErrors = Counter.build()
                        .name("keycloak_code_to_tokens_errors")
                        .help("Total failed code-to-token exchanges")
                        .labelNames("realm", "provider", "error", "client_id")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_code_to_tokens_errors");
            }

            try {
                // keycloak HTTP Response 수
                responseTotal = Counter.build()
                        .name("keycloak_response_total")
                        .help("Total HTTP responses")
                        .labelNames("code", "method", "resource")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_response_total");
            }

            try {
                // Keycloak HTTP Response Error 수
                responseErrors = Counter.build()
                        .name("keycloak_response_errors")
                        .help("Total HTTP error responses")
                        .labelNames("code", "method", "resource")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_response_errors");
            }

            try {
                // keycloak 응답 비율
                requestDuration = Histogram.build()
                        .name("keycloak_request_duration")
                        .help("Request duration in ms")
                        .buckets(50, 100, 250, 500, 1000, 2000, 5000, 10000)
                        .labelNames("code", "method", "resource")
                        .register();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Metric already registered: keycloak_request_duration");
            }

            // 모든 이벤트 타입별 카운터
            for (EventType type : EventType.values()) {
                counters.put(buildUserCounterName(type), createUserCounter(type));
            }

            for (OperationType type : OperationType.values()) {
                counters.put(buildAdminCounterName(type), createAdminCounter(type));
            }

            initialized = true;
            LOGGER.info("✅ PrometheusExporter initialized.");
        }
    }

    public static synchronized PrometheusExporter instance() {
        if (INSTANCE == null) {
            INSTANCE = new PrometheusExporter();
        }
        return INSTANCE;
    }

    private Counter createUserCounter(EventType type) {
        return Counter.build()
                .name(buildUserCounterName(type))
                .help("User event: " + type.name())
                .labelNames("realm")
                .register();
    }

    private Counter createAdminCounter(OperationType type) {
        return Counter.build()
                .name(buildAdminCounterName(type))
                .help("Admin event: " + type.name())
                .labelNames("realm", "resource")
                .register();
    }

    private String buildUserCounterName(EventType type) {
        return USER_EVENT_PREFIX + type.name().toLowerCase();
    }

    private String buildAdminCounterName(OperationType type) {
        return ADMIN_EVENT_PREFIX + type.name().toLowerCase();
    }

    private String getRealmName(String realmId, RealmProvider provider) {
        if (realmId == null || provider == null) return "unknown";
        RealmModel realm = provider.getRealm(realmId);
        return realm != null ? realm.getName() : "unknown";
    }

    private String getProvider(Event event) {
        if (event.getDetails() != null && event.getDetails().containsKey("identity_provider")) {
            return event.getDetails().get("identity_provider");
        }
        return PROVIDER_KEYCLOAK_OPENID;
    }

    // === Event 처리 ===

    public void recordGenericEvent(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String name = buildUserCounterName(event.getType());
        if (counters.containsKey(name)) {
            counters.get(name).labels(realm).inc();
        }
    }

    public void recordGenericAdminEvent(AdminEvent event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String name = buildAdminCounterName(event.getOperationType());
        if (counters.containsKey(name)) {
            counters.get(name).labels(realm, event.getResourceType().name()).inc();
        }
    }

    public void recordLogin(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String providerName = getProvider(event);

        loginAttempts.labels(realm, providerName, clientId).inc();
        successfulLogins.labels(realm, providerName, clientId).inc();
    }

    public void recordLoginError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String error = Optional.ofNullable(event.getError()).orElse("unknown");
        String providerName = getProvider(event);

        loginAttempts.labels(realm, providerName, clientId).inc();
        failedLogins.labels(realm, providerName, error, clientId).inc();
    }

    public void recordClientLogin(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String providerName = getProvider(event);

        clientLogins.labels(realm, providerName, clientId).inc();
    }

    public void recordClientLoginError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String error = Optional.ofNullable(event.getError()).orElse("unknown");
        String providerName = getProvider(event);

        failedClientLogins.labels(realm, providerName, error, clientId).inc();
    }

    public void recordRegistration(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String providerName = getProvider(event);

        registrations.labels(realm, providerName, clientId).inc();
    }

    public void recordRegistrationError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String error = Optional.ofNullable(event.getError()).orElse("unknown");
        String providerName = getProvider(event);

        registrationErrors.labels(realm, providerName, error, clientId).inc();
    }

    public void recordRefreshToken(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String providerName = getProvider(event);

        LOGGER.infof("🔁 [recordRefreshToken] realm=%s, clientId=%s, provider=%s", realm, clientId, providerName);

        refreshTokens.labels(realm, providerName, clientId).inc();
    }

    public void recordRefreshTokenError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String error = Optional.ofNullable(event.getError()).orElse("unknown");
        String providerName = getProvider(event);

        refreshTokenErrors.labels(realm, providerName, error, clientId).inc();
    }

    public void recordCodeToToken(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String providerName = getProvider(event);

        codeToTokens.labels(realm, providerName, clientId).inc();
    }

    public void recordCodeToTokenError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = Optional.ofNullable(event.getClientId()).orElse("unknown");
        String error = Optional.ofNullable(event.getError()).orElse("unknown");
        String providerName = getProvider(event);

        codeToTokenErrors.labels(realm, providerName, error, clientId).inc();
    }

    public void export(OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        writer.flush();
    }
}