package cope.client.util;

import java.util.HashMap;
import java.util.Map;

public class MCVersionUtil {
    private static final Map<String, Integer> VERSION_PROTOCOLS = new HashMap<>();

    static {
        // Latest versions
        VERSION_PROTOCOLS.put("1.20.4", 765);
        VERSION_PROTOCOLS.put("1.20.3", 765);
        VERSION_PROTOCOLS.put("1.20.2", 764);
        VERSION_PROTOCOLS.put("1.20.1", 763);
        VERSION_PROTOCOLS.put("1.20", 763);

        // 1.19.x
        VERSION_PROTOCOLS.put("1.19.4", 762);
        VERSION_PROTOCOLS.put("1.19.3", 761);
        VERSION_PROTOCOLS.put("1.19.2", 760);
        VERSION_PROTOCOLS.put("1.19.1", 760);
        VERSION_PROTOCOLS.put("1.19", 759);

        // 1.18.x
        VERSION_PROTOCOLS.put("1.18.2", 758);
        VERSION_PROTOCOLS.put("1.18.1", 757);
        VERSION_PROTOCOLS.put("1.18", 757);

        // 1.17.x
        VERSION_PROTOCOLS.put("1.17.1", 756);
        VERSION_PROTOCOLS.put("1.17", 755);

        // 1.16.x
        VERSION_PROTOCOLS.put("1.16.5", 754);
        VERSION_PROTOCOLS.put("1.16.4", 754);
        VERSION_PROTOCOLS.put("1.16.3", 753);
        VERSION_PROTOCOLS.put("1.16.2", 751);
        VERSION_PROTOCOLS.put("1.16.1", 736);
        VERSION_PROTOCOLS.put("1.16", 735);

        // 1.15.x
        VERSION_PROTOCOLS.put("1.15.2", 578);
        VERSION_PROTOCOLS.put("1.15.1", 575);
        VERSION_PROTOCOLS.put("1.15", 573);

        // 1.14.x
        VERSION_PROTOCOLS.put("1.14.4", 498);
        VERSION_PROTOCOLS.put("1.14.3", 490);
        VERSION_PROTOCOLS.put("1.14.2", 485);
        VERSION_PROTOCOLS.put("1.14.1", 480);
        VERSION_PROTOCOLS.put("1.14", 477);

        // 1.13.x
        VERSION_PROTOCOLS.put("1.13.2", 404);
        VERSION_PROTOCOLS.put("1.13.1", 401);
        VERSION_PROTOCOLS.put("1.13", 393);

        // 1.12.x
        VERSION_PROTOCOLS.put("1.12.2", 340);
        VERSION_PROTOCOLS.put("1.12.1", 338);
        VERSION_PROTOCOLS.put("1.12", 335);

        // 1.11.x
        VERSION_PROTOCOLS.put("1.11.2", 316);
        VERSION_PROTOCOLS.put("1.11.1", 316);
        VERSION_PROTOCOLS.put("1.11", 315);

        // 1.10.x
        VERSION_PROTOCOLS.put("1.10.2", 210);
        VERSION_PROTOCOLS.put("1.10.1", 210);
        VERSION_PROTOCOLS.put("1.10", 210);

        // 1.9.x
        VERSION_PROTOCOLS.put("1.9.4", 110);
        VERSION_PROTOCOLS.put("1.9.3", 110);
        VERSION_PROTOCOLS.put("1.9.2", 109);
        VERSION_PROTOCOLS.put("1.9.1", 108);
        VERSION_PROTOCOLS.put("1.9", 107);

        // 1.8.x
        VERSION_PROTOCOLS.put("1.8.9", 47);
        VERSION_PROTOCOLS.put("1.8.8", 47);
        VERSION_PROTOCOLS.put("1.8.7", 47);
        VERSION_PROTOCOLS.put("1.8.6", 47);
        VERSION_PROTOCOLS.put("1.8.5", 47);
        VERSION_PROTOCOLS.put("1.8.4", 47);
        VERSION_PROTOCOLS.put("1.8.3", 47);
        VERSION_PROTOCOLS.put("1.8.2", 47);
        VERSION_PROTOCOLS.put("1.8.1", 47);
        VERSION_PROTOCOLS.put("1.8", 47);

        // 1.7.x
        VERSION_PROTOCOLS.put("1.7.10", 5);
        VERSION_PROTOCOLS.put("1.7.9", 5);
        VERSION_PROTOCOLS.put("1.7.8", 5);
        VERSION_PROTOCOLS.put("1.7.7", 5);
        VERSION_PROTOCOLS.put("1.7.6", 5);
        VERSION_PROTOCOLS.put("1.7.5", 4);
        VERSION_PROTOCOLS.put("1.7.4", 4);
        VERSION_PROTOCOLS.put("1.7.2", 4);
    }

    public static int versionToProtocol(String version) {
        return VERSION_PROTOCOLS.getOrDefault(version.trim(), 0);
    }
}
