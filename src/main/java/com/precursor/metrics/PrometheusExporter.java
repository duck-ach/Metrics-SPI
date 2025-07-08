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

public final class PrometheusExporter {
    private static final Logger LOGGER = Logger.getLogger(PrometheusExporter.class);

    private static final String USER_EVENT_PREFIX = "keycloak_user_event_";
    private static final String ADMIN_EVENT_PREFIX = "keycloak_admin_event_";
    private static final String PROVIDER_KEYCLOAK_OPENID = "keycloak";

    private static PrometheusExporter INSTANCE;

    private final Map<String, Counter> counters = new HashMap<>();

    public final Counter loginAttempts;
    public final Counter successfulLogins;
    public final Counter failedLogins;
    public final Counter registrations;
    public final Counter registrationErrors;
    public final Counter refreshTokens;
    public final Counter refreshTokenErrors;
    public final Counter clientLogins;
    public final Counter failedClientLogins;
    public final Counter codeToTokens;
    public final Counter codeToTokenErrors;
    public final Counter responseTotal;
    public final Counter responseErrors;
    public final Histogram requestDuration;

    private PrometheusExporter() {

        // 로그인 시도 횟수
        loginAttempts = Counter.build().name("keycloak_login_attempts_total")
                .help("Total login attempts")
                .labelNames("realm", "provider", "client_id").register();

        System.out.println("loginAttempts = " + loginAttempts);
        // 로그인 성공 횟수
        successfulLogins = Counter.build().name("keycloak_logins_total")
                .help("Total successful logins")
                .labelNames("realm", "provider", "client_id").register();

        System.out.println("successfulLogins = " + successfulLogins);
        // 로그인 실패 횟수
        failedLogins = Counter.build().name("keycloak_failed_login_attempts_total")
                .help("Total failed login attempts")
                .labelNames("realm", "provider", "error", "client_id").register();
        System.out.println("failedLogins = " + failedLogins);
        // 회원가입 성공
        registrations = Counter.build().name("keycloak_registrations_total")
                .help("Total user registrations")
                .labelNames("realm", "provider", "client_id").register();
        System.out.println("registrations = " + registrations);
        // 회원가입 실패
        registrationErrors = Counter.build().name("keycloak_registrations_errors")
                .help("Total user registration errors")
                .labelNames("realm", "provider", "error", "client_id").register();
        System.out.println("registrationErrors = " + registrationErrors);
        // Refresh Token 사용 성공
        refreshTokens = Counter.build().name("keycloak_refresh_tokens")
                .help("Total refresh tokens")
                .labelNames("realm", "provider", "client_id").register();
        System.out.println("refreshTokens = " + refreshTokens);
        // Refresh Token 사용 실패
        refreshTokenErrors = Counter.build().name("keycloak_refresh_tokens_errors")
                .help("Total refresh token errors")
                .labelNames("realm", "provider", "error", "client_id").register();
        System.out.println("refreshTokenErrors = " + refreshTokenErrors);
        // Client 로그인 (전체)
        clientLogins = Counter.build().name("keycloak_client_logins")
                .help("Total successful client logins")
                .labelNames("realm", "provider", "client_id").register();
        System.out.println("clientLogins = " + clientLogins);
        // client 로그인 (에러)
        failedClientLogins = Counter.build().name("keycloak_failed_client_login_attempts")
                .help("Total failed client logins")
                .labelNames("realm", "provider", "error", "client_id").register();
        System.out.println("failedClientLogins = " + failedClientLogins);
        // 토큰 교환 (전체)
        codeToTokens = Counter.build().name("keycloak_code_to_tokens")
                .help("Total successful code-to-token exchanges")
                .labelNames("realm", "provider", "client_id").register();
        System.out.println("codeToTokens = " + codeToTokens);
        // 토큰 교환 (에러)
        codeToTokenErrors = Counter.build().name("keycloak_code_to_tokens_errors")
                .help("Total failed code-to-token exchanges")
                .labelNames("realm", "provider", "error", "client_id").register();
        System.out.println("codeToTokenErrors = " + codeToTokenErrors);
        // HTTP 응답 카운터 (전체)
        responseTotal = Counter.build().name("keycloak_response_total")
                .help("Total HTTP responses")
                .labelNames("code", "method", "resource").register();
        System.out.println("responseTotal = " + responseTotal);
        // HTTP 응답 카운터 (에러)
        responseErrors = Counter.build().name("keycloak_response_errors")
                .help("Total HTTP error responses")
                .labelNames("code", "method", "resource").register();
        System.out.println("responseErrors = " + responseErrors);
        // 요청 처리 시간 (ms 기준, Histogram)
        requestDuration = Histogram.build().name("keycloak_request_duration")
                .help("Request duration in ms")
                .buckets(50, 100, 250, 500, 1000, 2000, 5000, 10000)
                .labelNames("code", "method", "resource").register();
        System.out.println("requestDuration = " + requestDuration);
        // 모든 Keycloak User 이벤트 타입별 Counter 등록
        for (EventType eventType : EventType.values()) {
            counters.put(buildUserCounterName(eventType), createUserCounter(eventType));
            System.out.println("eventType = " + eventType + ", EventType.values() = " + EventType.values());
        }
        // 모든 Admin 이벤트(OperationType) Counter 등록
        for (OperationType op : OperationType.values()) {
            counters.put(buildAdminCounterName(op), createAdminCounter(op));
            System.out.println("OperationType = " + op + ", OperationType.values() = " + OperationType.values());
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

    public void recordGenericEvent(Event event, RealmProvider provider) {
        String realmName = getRealmName(event.getRealmId(), provider);
        String name = buildUserCounterName(event.getType());
        if (counters.containsKey(name)) {
            counters.get(name).labels(realmName).inc();
        }

    }

    public void recordGenericAdminEvent(AdminEvent event, RealmProvider provider) {
        String realmName = getRealmName(event.getRealmId(), provider);
        String name = buildAdminCounterName(event.getOperationType());
        if (counters.containsKey(name)) {
            counters.get(name).labels(realmName, event.getResourceType().name()).inc();
        }

    }

    public void recordLogin(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String providerName = getProvider(event);

        System.out.println("realm = " + realm);
        System.out.println("clientId = " + clientId);
        System.out.println("providerName = " + providerName);

        loginAttempts.labels(realm, providerName, clientId).inc();
        successfulLogins.labels(realm, providerName, clientId).inc();

    }

    public void recordLoginError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String error = event.getError();
        String providerName = getProvider(event);

        loginAttempts.labels(realm, providerName, clientId).inc();
        failedLogins.labels(realm, providerName, error, clientId).inc();

    }

    public void recordClientLogin(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String providerName = getProvider(event);
        String clientId = event.getClientId();

        clientLogins.labels(realm, providerName, clientId).inc();

    }

    public void recordRegistration(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String providerName = getProvider(event);

        registrations.labels(realm, providerName, clientId).inc();

    }

    public void recordRegistrationError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String error = event.getError();
        String providerName = getProvider(event);

        registrationErrors.labels(realm, providerName, error, clientId).inc();

    }

    public void recordRefreshToken(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String providerName = getProvider(event);

        refreshTokens.labels(realm, providerName, clientId).inc();
    }

    public void recordRefreshTokenError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String error = event.getError();
        String providerName = getProvider(event);

        refreshTokenErrors.labels(realm, providerName, error, clientId).inc();
    }

    public void recordCodeToToken(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String providerName = getProvider(event);

        codeToTokens.labels(realm, providerName, clientId).inc();
    }

    public void recordCodeToTokenError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String error = event.getError();
        String providerName = getProvider(event);

        codeToTokenErrors.labels(realm, providerName, error, clientId).inc();

    }

    public void recordClientLoginError(Event event, RealmProvider provider) {
        String realm = getRealmName(event.getRealmId(), provider);
        String clientId = event.getClientId();
        String error = event.getError();
        String providerName = getProvider(event);

        failedClientLogins.labels(realm, providerName, error, clientId).inc();
    }

    private String getProvider(Event event) {
        if (event.getDetails() != null && event.getDetails().containsKey("identity_provider")) {
            return event.getDetails().get("identity_provider");
        }
        return PROVIDER_KEYCLOAK_OPENID;
    }

    private String getRealmName(String realmId, RealmProvider realmProvider) {
        if (realmId == null || realmProvider == null) return "";
        RealmModel realm = realmProvider.getRealm(realmId);
        return realm != null ? realm.getName() : "";
    }

    public void export(OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        writer.flush();
    }

}