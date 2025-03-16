package cpu.test.assembler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static cpu.test.assembler.OpcodeInfo.*;
import static cpu.test.assembler.SyntaxError.*;
import cpu.test.assembler.coms.DataSectionResolver;
import cpu.test.assembler.coms.InstructionsResolver;

public class ShitwareAssembler {
	// Keeps track of the total number of bytes assembled so far. 
	// This is used to determine instruction addresses in memory, 
	// particularly for labels (directives) and branching instructions.
	public static int ASSEMBLED_BYTES = 0; 	
	
	public static Set<String> markerFound = new HashSet<>();
	
	static enum ParserContext {
		DATA, TEXT
	}
	
	public static ParserContext parseContext = ParserContext.TEXT;
	
    /**
     * Assembles the given source code.
     * @param fileName The name of the source file.
     * @param lines The source code lines.
     */
	public static void assemble(String fileName, String[] lines) throws RuntimeException {
		for (int i = 0; i < lines.length; i++) {
			int lineNumber = i + 1; // get the line number (+1)
			String line = lines[i].trim(); // get the line (trim())
			
			// check for marker
			try {
				if (line.startsWith("@")) {
					String marker = stripInlineComment(line).trim();
					String sectionMarker = marker.substring(1).toLowerCase();
					// validation (@data, @text allowed)
					if (!sectionMarker.equals("data") && !sectionMarker.equals("text")) {
						throw new SyntaxError("Invalid section marker! Only \"@data\" and \"@text\" are supported!", eENTIRE_LINE);
					}
					// check for duplication
					if (markerFound.contains(sectionMarker)) {
						throw new SyntaxError("Duplicated \"@" + sectionMarker + "\" section marker!", eENTIRE_LINE);
					}
					// switch context
					parseContext = ParserContext.valueOf(sectionMarker.toUpperCase());
					markerFound.add(sectionMarker); // add the section marker to track it
					continue;
				}
			} catch (SyntaxError syntaxError) {
				reportAssemblerError(fileName, "section marker", lineNumber, line, syntaxError);
				throw new RuntimeException("Aborted Task");
			}
			
			// for '@data' section
			if (parseContext == ParserContext.DATA) {
				DataSectionResolver.parseDataSectionLine(fileName, line, lineNumber);
				continue;
			}
			
			// for '@text' section
			if (parseContext == ParserContext.TEXT) {
				InstructionsResolver.parseTextSectionLine(fileName, line, lineNumber);
				continue;
			}
		}
		
		// assemble the actual instructions
		System.out.println(DataSectionResolver.assembledDataSection);
		System.out.println(DataSectionResolver.dataToAddressOffset);
		ASSEMBLED_BYTES = 0; // resets
		InstructionsResolver.assembleFromParsedLines(fileName, lines);
	}
	
	/**
	 * Cleanup a line for error reporting
	 */
	public static String tidyLine(String asm) {
		return stripInlineComment(asm).trim().replaceAll("\\s+", " ");	
	}
	
	/**
	 * Report an assembler error (Syntax Error)
	 */
	public static void reportAssemblerError(String fileName, String errorType, int lineNumber, String line, SyntaxError e) {
	    String ln = "| " + lineNumber + " | ";
	    String tidiedLine = tidyLine(line);

	    StringBuilder errorMessage = new StringBuilder();
	    errorMessage.append("File \"").append(fileName).append("\"").append(", line ").append(lineNumber).append(", " + errorType).append(":\n");
	    errorMessage.append(ln).append(line).append("\n");
	    errorMessage.append(" ".repeat(ln.length())).append("~".repeat(line.length())).append("\n");

	    errorMessage.append("Instruction at line ").append(lineNumber).append(", instruction address ").append(String.format("0x%04X (Decimal: %d)", ASSEMBLED_BYTES, ASSEMBLED_BYTES)).append(":\n");
	    errorMessage.append(ln).append(tidiedLine).append("\n");
	    
	    switch (e.errorLevel) {
	    case eENTIRE_LINE:
	        errorMessage.append(" ".repeat(ln.length())).append("~".repeat(tidiedLine.length())).append("\n");
	        errorMessage.append(" ".repeat(ln.length())).append("^\n");
	        break;
	    case eOPCODE:
	        errorMessage.append(" ".repeat(ln.length())).append("~".repeat(3)).append("\n");
	        errorMessage.append(" ".repeat(ln.length() + 1)).append("^\n");
	        break;
	    case eFIRST_OPR:
	        errorMessage.append(" ".repeat(ln.length() + 4)).append("~".repeat(3)).append("\n");
	        errorMessage.append(" ".repeat(ln.length() + 5)).append("^\n");
	        break;
	    case eSECND_OPR:
	        errorMessage.append(" ".repeat(ln.length() + 8)).append("~".repeat(3)).append("\n");
	        errorMessage.append(" ".repeat(ln.length() + 9)).append("^\n");
	        break;
	    }

	    errorMessage.append("SyntaxError: ").append(e.getMessage());

	    System.err.println(errorMessage);
	}
	
	public static String generateError(String opcodeStr, Opcode expected) {
		if (expected.firstOperandType() == -1 && expected.secndOperandType() == -1) {
			return "The instruction with opcode '" + opcodeStr + "' does not accept any operands.";
		}
		if (expected.secndOperandType() == -1) {
			return "The instruction with opcode '" + opcodeStr + "' expects a single operand: [" + expected.firstOperandTypeName() + "]";
		}
		return "The instruction with opcode '" + opcodeStr + "' expects 2 operands: [" + expected.firstOperandTypeName() + "], [" + expected.secndOperandTypeName() + "]";
	}
	
    public static String stripInlineComment(String line) {
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
	
	/**
	 * Escape a character from a string
	 * @param string given string
	 * @return escaped character (\r, \n, etc)
	 */
	public static char escapeChar(String string) {
        char c = string.charAt(1);
        switch (c) {
        	case '0': return '\0';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case '\\': return '\\';
            case '\'': return '\'';
            default: throw new NumberFormatException();
        }
	}
	
	/**
	 * Parse a string into a 16-bit number, acceptable formats
	 * 0xABCD - Hexadecimal;
	 * 0b1010 - Binary;
	 * 123 - Decimal;
	 * 'a', '\n' - Characters (escape supported);
	 * 
	 * @param number the string that contains that number
	 * @return an unsigned 16-bit integer (char)
	 */
	public static char parseNumeric(String number) {
		number = number.toLowerCase();
		// get the first two characters (or 1 if ') for prefix
		String prefix = number.substring(0, number.length() < 2 ? 0 : (number.startsWith("'") ? 1 : 2));
		
		switch (prefix) {
		case "0x": { // hexadecimal
			return (char)(Integer.parseInt(number.substring(2), 16) & 0xFFFF);
		}
		case "0b": { // binary
			return (char)(Integer.parseInt(number.substring(2), 2) & 0xFFFF);
		}
		case "'": { // character
			String input = number.substring(1);
			if (input.indexOf('\'') == -1) { // a dangling character, missing closing quote (e.g. '1)
				throw new NumberFormatException();
			}
			input = input.substring(0, input.length() - 1); // remove the closing quote
			boolean isEscapeCharacter = input.charAt(0) == '\\';
			// if isn't a character (more than one), and doesn't have an escape prefix, throw an error
			if (input.length() != 1 && !isEscapeCharacter) throw new NumberFormatException();
			if (isEscapeCharacter) {
				// parse escape character here
				return (char)(((int) escapeChar(input)) & 0xFFFF);
			}
			return (char)(((int) input.charAt(0)) & 0xFFFF);
		}
 		default: // decimals
			return (char)(Integer.parseInt(number) & 0xFFFF);
		}
	}
	
	public static void main(String... args) {
        if (args.length < 1) {
            System.err.println("Usage: java swasm <input.asm>");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = inputFile.replaceAll("\\.asm$", ".o");
        try {
            // read the .asm file
            List<String> lines = Files.readAllLines(Paths.get(inputFile));
            String[] asmLines = lines.toArray(new String[0]);

            try {
            	assemble(inputFile, asmLines);
            	System.out.println("Assembled!");
            } catch (RuntimeException e) {
            	System.exit(1);
			}

            // write to .o file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                for (byte b : InstructionsResolver.assembledTextSection) {
                    fos.write(b);
                }
            }
        } catch (IOException e) {
            System.err.println("FileError: " + e.getMessage());
        }
    }
}
