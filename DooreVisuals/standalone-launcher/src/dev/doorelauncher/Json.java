package dev.doorelauncher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small JSON reader and writer (objects are LinkedHashMap, arrays are ArrayList, numbers are Double). */
final class Json {
    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object value = p.value();
        p.ws();
        if (p.i != p.s.length()) {
            throw new IllegalArgumentException("Extra data at " + p.i);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return o instanceof Map ? (Map<String, Object>)o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>)o : new ArrayList<>();
    }

    static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': {
                i++;
                Map<String, Object> map = new LinkedHashMap<>();
                ws();
                if (s.charAt(i) == '}') {
                    i++;
                    return map;
                }
                while (true) {
                    ws();
                    String key = string();
                    ws();
                    i++; // :
                    ws();
                    map.put(key, value());
                    ws();
                    char d = s.charAt(i++);
                    if (d == '}') {
                        return map;
                    }
                }
            }
            case '[': {
                i++;
                List<Object> list = new ArrayList<>();
                ws();
                if (s.charAt(i) == ']') {
                    i++;
                    return list;
                }
                while (true) {
                    ws();
                    list.add(value());
                    ws();
                    char d = s.charAt(i++);
                    if (d == ']') {
                        return list;
                    }
                }
            }
            case '"':
                return string();
            case 't':
                i += 4;
                return Boolean.TRUE;
            case 'f':
                i += 5;
                return Boolean.FALSE;
            case 'n':
                i += 4;
                return null;
            default: {
                int start = i;
                while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                    i++;
                }
                return Double.parseDouble(s.substring(start, i));
            }
        }
    }

    private String string() {
        i++; // opening quote
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    out.append((char)Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> out.append(e);
            }
        }
    }

    static String write(Object o) {
        StringBuilder out = new StringBuilder();
        write(o, out, 0);
        return out.toString();
    }

    private static void write(Object o, StringBuilder out, int depth) {
        if (o == null) {
            out.append("null");
        } else if (o instanceof Map<?, ?> map) {
            out.append("{\n");
            int n = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.append("  ".repeat(depth + 1));
                quote(String.valueOf(e.getKey()), out);
                out.append(": ");
                write(e.getValue(), out, depth + 1);
                out.append(++n < map.size() ? ",\n" : "\n");
            }
            out.append("  ".repeat(depth)).append('}');
        } else if (o instanceof List<?> list) {
            out.append('[');
            for (int k = 0; k < list.size(); k++) {
                if (k > 0) {
                    out.append(", ");
                }
                write(list.get(k), out, depth + 1);
            }
            out.append(']');
        } else if (o instanceof Number n) {
            double d = n.doubleValue();
            out.append(d == Math.rint(d) ? Long.toString((long)d) : Double.toString(d));
        } else if (o instanceof Boolean b) {
            out.append(b);
        } else {
            quote(o.toString(), out);
        }
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int)c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
