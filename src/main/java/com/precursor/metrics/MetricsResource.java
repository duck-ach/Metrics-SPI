package com.precursor.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.keycloak.services.resource.RealmResourceProvider;

import java.io.StringWriter;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/metrics")
public class MetricsResource implements RealmResourceProvider {
    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces(TEXT_PLAIN)
    public Response metrics() {
        try {
            StringWriter writer = new StringWriter();
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            return Response.ok(writer.toString()).build();
        } catch (Exception e) {
            return Response.serverError().entity("Failed to export metrics").build();
        }
    }

    @Override
    public void close() {}
}
