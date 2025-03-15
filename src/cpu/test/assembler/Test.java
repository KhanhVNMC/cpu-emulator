package cpu.test.assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        String[] lines = {
            "string  				 .str  \"Hello, World\"",
            "number   .byte 10",
            "numbers  .nigga 10, 20",
            "halfword .hword 0xCAFE, 'a', 100",
            "cumshot  .char  'a' ; this is just syntax sugar, its .byte anyway"
        };

        for (String originalLine : lines) {
            // Remove comments (everything after ';')
            String line = originalLine.split(";")[0].trim();
            if (line.isEmpty()) continue;

            // Expect at least three parts: name, type and values
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3) {
                System.out.println("ERROR: Not enough tokens in line: " + line);
                continue;
            }
            String name = parts[0];
            String typeToken = parts[1];
            // Remove leading dot if present
            String type = typeToken.startsWith(".") ? typeToken.substring(1) : typeToken;
            String valuesPart = parts[2].trim();

            // Process based on type
            if (type.equals("str")) {
                // For .str, support only one string literal. If there's a comma, that's an error.
                if (valuesPart.contains(",")) {
                    System.out.println("ERROR: Multiple string literals not supported for " + name);
                    continue;
                }
                if (valuesPart.startsWith("\"") && valuesPart.endsWith("\"") && valuesPart.length() >= 2) {
                    // Remove the surrounding double quotes
                    String literal = valuesPart.substring(1, valuesPart.length() - 1);
                    char[] charArray = literal.toCharArray();
                    System.out.println("name: \"" + name + "\"");
                    System.out.println("type: \"" + type + "\"");
                    System.out.println("values: " + Arrays.toString(charArray));
                } else {
                    System.out.println("ERROR: Invalid string literal for " + name);
                }
            } else {
                // For other types: .byte, .hword, .char, etc.
                // Split the values on comma
                String[] tokens = valuesPart.split(",");
                List<Object> values = new ArrayList<>();
                for (String token : tokens) {
                    token = token.trim();
                    if (token.isEmpty()) continue;
                    // Check if it's a char literal (e.g. 'a')
                    if (token.startsWith("'") && token.endsWith("'") && token.length() >= 3) {
                        // Note: This simple approach doesn't handle escape sequences.
                        values.add(token.charAt(1));
                    } else {
                        // Assume it's a numeric literal.
                        try {
                            int num;
                            if (token.startsWith("0x") || token.startsWith("0X")) {
                                num = Integer.decode(token); // works for hexadecimal numbers
                            } else {
                                num = Integer.parseInt(token);
                            }
                            values.add(num);
                        } catch (NumberFormatException e) {
                            System.out.println("ERROR: Invalid numeric literal: " + token);
                        }
                    }
                }
                System.out.println("name: \"" + name + "\"");
                System.out.println("type: \"" + type + "\"");
                System.out.println("values: " + values);
            }
            System.out.println(); // blank line between entries
        }
    }
}
