package com.ford.riva.util;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private InputSanitizer() {
    }

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    private static final Pattern XSS = Pattern.compile(
            "(?i)(<\\s*script|javascript:|vbscript:|data:text/html|<\\s*iframe|<\\s*embed|<\\s*object|<\\s*svg|"
                    + "on(load|click|error|mouseover|mouseout|mousedown|mouseup|keydown|keyup|keypress|focus|blur|"
                    + "change|submit|input)\\s*=)"
    );

    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(\\bunion\\s+(all\\s+)?select\\b|\\bdrop\\s+table\\b|\\binsert\\s+into\\b|\\bdelete\\s+from\\b|"
                    + "\\bexec(ute)?\\s*\\(|\\bxp_cmdshell\\b|--\\s|/\\*|\\*/|;\\s*(drop|delete|update|insert|alter|create))"
    );

    private static final Pattern COMMAND_INJECTION = Pattern.compile(
            "(\\$\\([^)]*\\)|`[^`]*`|\\|\\s*(sh|bash|cmd|powershell|nc|wget|curl)\\b|"
                    + ";\\s*(rm|del|format|shutdown|reboot)\\b|&&\\s*(rm|del|format|shutdown|reboot)\\b)"
    );

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e[/\\\\]|/etc/(passwd|shadow|hosts)|/proc/|c:\\\\windows)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("\\s{2,}");

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        String result = input.trim();
        result = CONTROL_CHARS.matcher(result).replaceAll("");
        result = HTML_TAG.matcher(result).replaceAll("");
        result = EXCESSIVE_WHITESPACE.matcher(result).replaceAll(" ");
        return result;
    }

    public static boolean containsAttackPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return XSS.matcher(input).find()
                || SQL_INJECTION.matcher(input).find()
                || COMMAND_INJECTION.matcher(input).find()
                || PATH_TRAVERSAL.matcher(input).find();
    }

    public static String sanitizeAndValidate(String input) {
        String sanitized = sanitize(input);
        if (containsAttackPattern(sanitized)) {
            throw new IllegalArgumentException("Entrada contém caracteres ou padrões não permitidos");
        }
        return sanitized;
    }
}
