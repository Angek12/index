package com.index.platform.util;

import java.util.ArrayList;
import java.util.List;

public final class PhoneUtil {
    private PhoneUtil() {}

    /** Turns 0795 288 755, 250795288755 or +250795288755 into +250795288755. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[^0-9+]", "");
        if (s.isEmpty()) return null;
        if (s.startsWith("+")) return s;
        if (s.startsWith("250")) return "+" + s;
        if (s.startsWith("0")) return "+250" + s.substring(1);
        if (s.length() == 9) return "+250" + s;
        return "+" + s;
    }

    /** All common ways the same number may be stored. */
    public static List<String> variants(String raw) {
        String n = normalize(raw);
        List<String> v = new ArrayList<>();
        if (n == null) return v;
        v.add(n);
        v.add(n.substring(1));
        if (n.startsWith("+250")) {
            String local = n.substring(4);
            v.add("0" + local);
            v.add(local);
        }
        return v;
    }
}
