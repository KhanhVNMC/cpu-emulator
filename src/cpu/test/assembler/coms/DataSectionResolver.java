package cpu.test.assembler.coms;

import cpu.test.assembler.SyntaxError;
import static cpu.test.assembler.ShitwareAssembler.*;
import static cpu.test.assembler.SyntaxError.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSectionResolver {
	public static final String TYPE_CSTRING = ".cstr";
	public static final String TYPE_STRING  = ".str";
	public static final String TYPE_BYTE    = ".byte";
	public static final String TYPE_CHAR    = ".char";
	public static final String TYPE_HWORD   = ".hword";
	// syntax sugar
	public static final String TYPE_UI8     = ".uint8";
	public static final String TYPE_UI16    = ".uint16";
	// for arrays
	public static final String TYPE_SPACE   = ".space";
	
	static final HashSet<String> AVAILABLE_TYPES = new HashSet<>(Set.of(
		TYPE_CSTRING,
		TYPE_STRING, 
		TYPE_BYTE,
		TYPE_CHAR,
		TYPE_HWORD,
		// sugar
		TYPE_UI8,
		TYPE_UI16,
		// array
		TYPE_SPACE
	));
	
	// Stores the assembled machine code as a list of bytes. 
	// Each instruction and its operands are converted into their corresponding 
	// byte representation and stored in this list for output or execution.
	public static List<Byte> assembledDataSection = new ArrayList<>();
	
	// A mapping of data defined in the @data section to memory address (offset
	// from the @text section)
	public static Map<String, Integer> dataToAddressOffset = new HashMap<>();
	
	public static int ASSEMBLED_DATA = 0;
	
	public static void parseDataSectionLine(String fileName, String original, int lineNumber) {
		// ignore comments and blank lines
		if (original.trim().isBlank() || original.trim().startsWith(";")) return;
		String line = stripInlineComment(original).trim(); // strip comments
		
		try {
	        // parse label, directive, and values using regex (capture groups)
	        Pattern pattern = Pattern.compile("^(\\w+)\\s+(\\.\\w+)\\s+(.*)$");
	        Matcher matcher = pattern.matcher(line);
	        if (!matcher.matches()) {
	            throw new SyntaxError("Invalid constant declaration format!\nExpected: [const name] .[type] [value]", eENTIRE_LINE);
	        }
	        
	        // parse the syntax line
	        String label 		= matcher.group(1); // name
	        String directive 	= matcher.group(2); // type
	        String valuesPart 	= matcher.group(3); // declaration
	        
	        if (!AVAILABLE_TYPES.contains(directive)) {
				throw new SyntaxError("Unknown constant literal type \"" + directive + "\"", eFIRST_OPR);
	        }
	        
	        // assemble strings (complex)
			if (directive.equals(TYPE_STRING) || directive.equals(TYPE_CSTRING)) {
				if (!valuesPart.startsWith("\"") || !valuesPart.endsWith("\"")) {
					throw new SyntaxError("Invalid string lateral, missing quote(s).", eENTIRE_LINE);
				}
				// remove double-quotes
				valuesPart = valuesPart.substring(1, valuesPart.length() - 1);
				if (directive.equals(TYPE_CSTRING)) valuesPart += '\0'; // null-terminated string
				
				// error if string is RAW and EMPTY at the same time
				else if (valuesPart.length() == 0) {
					throw new SyntaxError("Invalid string literal! Non-C strings cannot be empty.\nUse \".cstr\" for empty strings.", eENTIRE_LINE);
				}
				
				// the pointer (memory address) of this string 
				dataToAddressOffset.put(label, ASSEMBLED_DATA);
				// assemble the data
				for (char c : valuesPart.toCharArray()) {
					assembledDataSection.add((byte) (c & 0xFF)); // only retain ASCII (lower byte)
					ASSEMBLED_DATA++; // 1 wide because char (uint8)
				}
				return;
			}
			// assemble normal types (declarative)
			// see how many shit has been declared
			String[] parts = valuesPart.split(",(?=(?:[^']*'[^']*')*[^']*$)");
			
			// assemble an empty array with specified sizes
			if (directive.equals(TYPE_SPACE)) {
				// the address of the array's head
				dataToAddressOffset.put(label, ASSEMBLED_DATA);
				try {
					// read "how many elements should we create"
					char elements = parseNumeric(parts[0].trim());
					if (elements > 512) { // we do not allow to reserve more than 512 bytes
						throw new SyntaxError("Unable to reserve " + (int)elements + " elements! The maximum limit is 512.", eENTIRE_LINE);
					}
					// reserve n elements (bytes); regardless of if we actually allocate or not
					ASSEMBLED_DATA += elements; // 1 byte-wide
					// filler (or whatever)
					if (parts.length == 2) {
						try {
							// read "what should we fill the holes with"
							char assignment = parseNumeric(parts[1].trim());
							for (int i = 0; i < elements; i++) {
								assembledDataSection.add((byte) (assignment & 0xFF)); // only take one byte
							}
						} catch (Exception e) {
							throw new SyntaxError("Unable to fill the reserved memory region with \"" + parts[1].trim() + "\". Value is not a valid numeric!\nNumeric examples: 0xCAFE, 0b10101, 128.", eENTIRE_LINE);
						}
					} else if (parts.length > 2) { // too long
						throw new SyntaxError("'.space' allocation expected at most 2 arguments: size, [filler], got " + parts.length + " instead.", eENTIRE_LINE);
					}
				} catch (Exception e) {
					if (e instanceof SyntaxError) {
						throw e;
					}
					throw new SyntaxError("Unable to reserve \"" + parts[0].trim() + "\" elements. Value is not a valid numeric!\nNumeric examples: 0xCAFE, 0b10101, 128.", eENTIRE_LINE);
				}
				return;
			}
			
			// assemble normal value or values
			// the offset is the first element of the "array" (just like int[] arr = {1,2,3} => *arr == 1)
			dataToAddressOffset.put(label, ASSEMBLED_DATA);
			for (String part : parts) {
				// parse each part and assemble the data
				try {
					char uint16 = parseNumeric(part.trim());
					// halfword = 16bit, byte = 8bit
					if (directive.equals(TYPE_HWORD) || directive.equals(TYPE_UI16)) {
						// for 16-bit, we need to split it into 2 parts (in big edian, ofcourse)
						assembledDataSection.add((byte) (uint16 >> 8));   // higher byte
						assembledDataSection.add((byte) (uint16 & 0xFF)); // lower byte
						ASSEMBLED_DATA += 2; // 2 wide
						continue;
					}
					// assemble 8 bit (by just slapping it in)
					assembledDataSection.add((byte) (uint16 & 0xFF)); // lower byte (because char and byte is 1 byte long)
					ASSEMBLED_DATA++; // 1 wide
				} catch (Exception e) {
					throw new SyntaxError("\"" + part + "\" is not a valid number or character!\nValid character literals: 'a', 'b'. Numeric examples: 0xCAFE, 0b10101, 128.", eENTIRE_LINE);
				}
			}
		} catch (SyntaxError syntaxError) {
			reportAssemblerError(fileName, "data section mapper", lineNumber, original, syntaxError);
			throw new RuntimeException("Aborted Task");
		}
	}
}
