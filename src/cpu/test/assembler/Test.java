package cpu.test.assembler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    /** Maps directives to their corresponding types */
    private static final Map<String, String> directiveToType = new HashMap<>();

    static {
        directiveToType.put(".str", "string");
        directiveToType.put(".byte", "byte");
        directiveToType.put(".char", "byte"); // .char is syntax sugar for .byte
        directiveToType.put(".hword", "hword");
    }

    public static String stripInlineComment(String line) throws SyntaxError {
    	boolean inQuote = false;
    	char[] chars = line.toCharArray();
    	for (int i = 0; i < line.length(); i++) {
    		char c = chars[i];
    		if (c == '\'' || c == '"') {
    			inQuote = !inQuote;
    		}
    		if (inQuote) continue;
    		if (c == ';') {
    			return line.substring(0, i);
    		}
    	}
    	return line;
    }
    
    public static void main(String[] args) throws SyntaxError {
    	System.out.println(stripInlineComment("hello \""));
    }

    /** Processes a single line of input */
    private static void processLine(String line) {
        // Remove comments and trim
        int commentIndex = line.indexOf(';');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }

        // Parse label, directive, and values using regex
        Pattern pattern = Pattern.compile("^(\\w+)\\s+(\\.\\w+)\\s+(.*)$");
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            System.err.println("Invalid line format: " + line);
            return;
        }

        String label = matcher.group(1);
        String directive = matcher.group(2);
        String valuesPart = matcher.group(3);

        // Get type from directive
        String type = directiveToType.get(directive);
        if (type == null) {
            System.err.println("Unknown directive: " + directive);
            return;
        }

        // Parse values based on directive
        List<Object> values;
        if (directive.equals(".str")) {
            String str = parseStringLiteral(valuesPart);
            values = new ArrayList<>();
            for (char c : str.toCharArray()) {
                values.add(c); // Store as Character objects
            }
        } else {
            String[] parts = valuesPart.split(",(?=(?:[^']*'[^']*')*[^']*$)");
            values = new ArrayList<>();
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    values.add(parseValue(part));
                }
            }
        }

        // Print in required format
        System.out.println("name: \"" + label + "\"");
        System.out.println("type: \"" + type + "\"");
        System.out.print("values: [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            Object val = values.get(i);
            if (val instanceof Character) {
                System.out.print(escapeChar((char) val));
            } else {
                System.out.print(val);
            }
        }
        System.out.println("]");
    }

    /** Parses a string literal, handling escape sequences */
    private static String parseStringLiteral(String s) {
        if (!s.startsWith("\"") || !s.endsWith("\"")) {
            throw new IllegalArgumentException("String literal must be quoted: " + s);
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = 1; i < s.length() - 1; i++) {
            char c = s.charAt(i);
            if (escape) {
                switch (c) {
                	case '0': sb.append("\0"); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: throw new IllegalArgumentException("Invalid escape sequence: \\" + c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        if (escape) {
            throw new IllegalArgumentException("Unterminated escape sequence in: " + s);
        }
        return sb.toString();
    }

    /** Parses a single value (number or character literal) */
    private static Object parseValue(String s) {
        s = s.trim();
        if (s.startsWith("'")) {
            // Character literal
            if (s.length() == 3 && s.endsWith("'")) {
                return s.charAt(1);
            } else if (s.length() == 4 && s.charAt(1) == '\\' && s.endsWith("'")) {
                char c = s.charAt(2);
                switch (c) {
                    case 'n': return '\n';
                    case 't': return '\t';
                    case '\\': return '\\';
                    case '\'': return '\'';
                    default: throw new IllegalArgumentException("Invalid escape in char: " + s);
                }
            } else {
                throw new IllegalArgumentException("Invalid char literal: " + s);
            }
        } else if (s.startsWith("0x")) {
            // Hexadecimal number
            try {
                return Integer.parseInt(s.substring(2), 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex number: " + s);
            }
        } else {
            // Decimal number
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid decimal number: " + s);
            }
        }
    }

    /** Escapes a character for printing as a Java literal */
    private static String escapeChar(char c) {
        switch (c) {
            case '\n': return "'\\n'";
            case '\t': return "'\\t'";
            case '\\': return "'\\\\'";
            case '\'': return "'\\''";
            default:
                if (Character.isISOControl(c)) {
                    return String.format("'\\u%04x'", (int) c);
                } else {
                    return "'" + c + "'";
                }
        }
    }
}