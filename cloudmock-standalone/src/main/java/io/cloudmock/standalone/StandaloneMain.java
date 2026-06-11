package io.cloudmock.standalone;

import io.cloudmock.core.CloudMock;
import io.cloudmock.core.spi.CloudMockApiService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StandaloneMain {

    // Debug detection runs before logger acquisition so the level takes effect when slf4j-simple
    // binds its configuration. CLOUDMOCK_DEBUG or -Dcloudmock.debug=true both work.
    static {
        if ("true".equalsIgnoreCase(System.getProperty("cloudmock.debug"))
                || "true".equalsIgnoreCase(System.getenv("CLOUDMOCK_DEBUG"))) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StandaloneMain.class);

    public static void main(String[] args) throws Exception {
        int port = PortResolver.resolve(args);
        int apiPort = ApiPortResolver.resolve(args);
        int maxHistory = MaxHistoryResolver.resolve(args);
        List<String> available = ServiceDiscovery.discoverServiceIds();
        Set<String> requested = ServiceSelector.resolve(args);
        List<String> enabled = resolveEnabled(available, requested);
        Path storeDir = StoreDirectoryResolver.resolve(args);

        System.out.println("[CloudMock] Available services: " + join(available));
        System.out.println("[CloudMock] Enabled services: " + join(enabled));
        if (enabled.isEmpty()) {
            System.out.println(
                    "[CloudMock] WARNING: no services enabled — the mock will serve nothing.");
            System.out.println(
                    "[CloudMock]          Enable services with --services=<id>[,<id>...] "
                            + "or CLOUDMOCK_SERVICES=<id>[,<id>...].");
            System.out.println("[CloudMock]          Available services: " + join(available));
        }
        System.out.println(
                "[CloudMock] State storage: "
                        + (storeDir != null
                                ? "persistent (" + storeDir + ")"
                                : "in-memory (not persisted)"));
        System.out.println(
                "[CloudMock] Request history: "
                        + (maxHistory > 0 ? "last " + maxHistory + " entries" : "unlimited"));

        CloudMock cloudMock = new CloudMock().withPort(port).withMaxRequestHistory(maxHistory);
        // Always pass the enabled set (even when empty): the default is "no services", so the
        // filter must be applied unconditionally rather than falling back to "all discovered".
        cloudMock.withEnabledServices(enabled);
        if (storeDir != null) {
            cloudMock.withStoreDirectory(storeDir);
        }

        // API routes must track the enabled modules: a disabled service has no stubs, so it must
        // not advertise REST routes (or CLI commands) either, otherwise the two views disagree.
        List<CloudMockApiService> apiServices =
                discoverApiServices().stream()
                        .filter(svc -> enabled.contains(svc.serviceId()))
                        .toList();

        try (cloudMock;
                ApiServer apiServer = new ApiServer(cloudMock, apiPort, apiServices)) {
            cloudMock.start();
            apiServer.start();

            System.out.println("CloudMock started on port " + cloudMock.port());
            System.out.println("CloudMock API on port " + apiPort);
            System.out.flush();

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(
                                    () -> {
                                        System.out.println("[CloudMock] Shutting down...");
                                        apiServer.stop();
                                        cloudMock.stop();
                                    }));

            Thread.currentThread().join();
        }
    }

    private static List<String> resolveEnabled(List<String> available, Set<String> requested) {
        if (requested == null) {
            // No --services / CLOUDMOCK_SERVICES selection: load nothing. Services are opt-in,
            // matching embedded mode where only modules placed on the classpath load.
            return List.of();
        }
        List<String> unknown = requested.stream().filter(id -> !available.contains(id)).toList();
        if (!unknown.isEmpty()) {
            System.err.println(
                    "[CloudMock] Unknown service(s): "
                            + join(unknown)
                            + ". Available: "
                            + join(available));
            System.exit(1);
        }
        return requested.stream().toList();
    }

    private static List<CloudMockApiService> discoverApiServices() {
        List<CloudMockApiService> services = new ArrayList<>();
        ServiceLoader.load(
                        CloudMockApiService.class, Thread.currentThread().getContextClassLoader())
                .forEach(services::add);
        return services;
    }

    private static String join(Collection<String> ids) {
        return ids.isEmpty() ? "(none)" : String.join(", ", ids);
    }
}
