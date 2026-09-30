package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * The route contract, docs/architecture/routes.yml, against the routes the application really
 * registers, in both directions: a controller added without its contract fails here, and so does a
 * route the contract calls built that no controller serves. Path variables are compared by position,
 * not name, since the contract says {@code {title}} where the code says {@code {address}}.
 */
@SpringBootTest
public class RouteContractTest {

    static final Path CONTRACT = Path.of("../docs/architecture/routes.yml");

    /** Spring Boot's own error endpoint: it serves every route's error page and belongs to none. */
    private static final Set<String> FRAMEWORK = Set.of("/error");

    /**
     * Routes Spring Security's filters answer before any controller, so no handler mapping lists them.
     * ModeratorLoginTest checks that each one answers.
     */
    private static final Set<String> SECURITY_FILTERS = Set.of("POST /moderate/logout");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void every_route_the_application_serves_is_in_the_contract_and_every_built_one_is_served() throws IOException {
        var served = new TreeSet<String>();
        mappings.getHandlerMethods().keySet().forEach(info -> {
            for (var path : info.getPatternValues()) {
                if (FRAMEWORK.contains(path)) {
                    continue;
                }
                var methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    // A mapping with no method answers every method; it can never match the contract.
                    served.add("ANY " + shape(path));
                }
                methods.forEach(method -> served.add(method + " " + shape(path)));
            }
        });

        served.addAll(SECURITY_FILTERS);
        var built = builtRoutes();

        assertThat(served)
                .as("routes served by a controller but missing from %s, or not marked built there", CONTRACT)
                .isSubsetOf(built);
        assertThat(built)
                .as("routes %s marks built that no controller serves", CONTRACT)
                .isSubsetOf(served);
    }

    /** Every method and path of the routes whose status is {@code built}, in the compared shape. */
    @SuppressWarnings("unchecked")
    public static Set<String> builtRoutes() throws IOException {
        Map<String, Object> contract = new Yaml().load(Files.readString(CONTRACT));
        var built = new TreeSet<String>();
        for (var route : (List<Map<String, Object>>) contract.get("routes")) {
            if (!"built".equals(route.get("status"))) {
                continue;
            }
            for (var method : (List<String>) route.get("methods")) {
                built.add(method + " " + shape((String) route.get("path")));
            }
        }
        return built;
    }

    private static String shape(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }
}
