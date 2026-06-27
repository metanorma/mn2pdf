package org.metanorma.fop.global;

import java.util.LinkedHashMap;
import java.util.Map;

public class Variables {

    private static Map<String, String> variables = new LinkedHashMap<>();

    public static void setVariable (String key, String value) {
        variables.put(key, value);
    }

    public static String getVariable(String key) {
        String value = variables.get(key);
        if (key.equals("num")) {
            return value == null ? "1" : value;
        }
        return value == null ? "" : value;
    }
}
