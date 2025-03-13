package cpu.test.assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cpu.test.assembler.OpcodeInfo.*;
import static cpu.test.assembler.SyntaxError.*;
import cpu.test.FL516CPU;

public class ShitwareAssembler {
	public static boolean isTypeCorrect(String operand, int expected) {
		if (expected == -1 && operand == null) return true;
		boolean isRegister = isRegister(operand);
		if (expected == OPERAND_REGISTER &&  isRegister) return true;
		if (expected == OPERAND_NUMBER   && !isRegister) return true;
		return false;
	}
	
	/**
	 * @return true if the operand is intended to be a register
	 */
	public static boolean isRegister(String operand) {
		return operand == null ? false : operand.toUpperCase().startsWith("R");
	}
	
	/**
	 * Parse a register
	 * @param firstOperand if its the first operand
	 * @param operand the operand
	 * @return parsed register as byte (index)
	 * @throws SyntaxError
	 */
	public static byte parseRegister(boolean firstOperand, String operand) throws SyntaxError {
		// ignore case
		operand = operand.toUpperCase();
		// get the registers check out
		if (!isRegister(operand)) {
			throw new SyntaxError("'" + operand + "' is not a valid register!");
		}
		// get the register identifier
		String targetRegister = operand.substring(1);
		if (targetRegister.equals("SP") /* stack pointer location in regs */) {
			return (byte)(FL516CPU.STACK_PTR_LOC & 0xFF);
		}
		if (targetRegister.equals("MD") /* division remainder loc in regs */) {
			return (byte)(FL516CPU.DIV_REMAINDER & 0xFF);
		}
		try {
			int register = Integer.parseInt(targetRegister);
			if (register < 0 || register > 7) throw new NumberFormatException();
			return (byte) (register & 0xFF);
		} catch (NumberFormatException e) {
			throw new SyntaxError("'" + operand + "' is not a valid register operand!\nValid general-purpose registers range from R0 to R7.", firstOperand ? eFIRST_OPR : eSECND_OPR);
		}
	}
	
	/**
	 * Parse a number (support HEX, DEC and BIN)
	 * @param firstOperand if its the first operand
	 * @param operand the operand
	 * @return parsed num as char (uint16_t)
	 * @throws SyntaxError
	 */
	public static char parseNumbers(boolean firstOperand, String operand) throws SyntaxError {
		// ignore the prefix's case
		operand = operand.toLowerCase();
		String prefix = operand.substring(0, operand.length() < 2 ? 0 : 2); // get the first two characters
		
		try {
			switch (prefix) {
			case "0x": { // hexadecimal
				return (char)(Integer.parseInt(operand.substring(2), 16) & 0xFFFF);
			}
			case "0b": { // binary
				return (char)(Integer.parseInt(operand.substring(2), 2) & 0xFFFF);
			}
			default: // decimals
				return (char)(Integer.parseInt(operand) & 0xFFFF);
			}
		} catch (Exception e) {
			throw new SyntaxError("'" + operand + "' is not a valid number operand!\nValid examples: 0xCAFE, 0b10101, 128.", firstOperand ? eFIRST_OPR : eSECND_OPR);
		}
	}
	
	/**
     * Parses an assembly line into opcode and operands.
     * @param asm the assembly instruction line
     * @return an array containing opcode, first operand, and second operand (if present)
     * @throws SyntaxError if syntax is incorrect
     */
	public static String[] parseLine(String asm) throws SyntaxError {
		// ignore comments
		if (asm.trim().startsWith(";")) return new String[0];
		
		asm = asm.split(";", 2)[0].trim(); // strip comments
		
		// capture block (\S+ - any characters that isnt a whitespace)
		// the middle part must be whitespaces \\s+ (any spaces, including tabs)
		// the last part (.*) can be anything
		Pattern pattern = Pattern.compile("^(\\S+)\\s*(.*)$");
		Matcher matcher = pattern.matcher(asm);
		
		// if we cannot find a matching pattern (i.e. shit code), return null
		if (matcher.find()) {
			// extract the opcode name by getting the first group
			String opcode = matcher.group(1).toUpperCase();
			// check for abnormal characters in the instruction name
			if (opcode.matches(".*[;,.:\\[\\]{}|*()%].*")) {
				throw new SyntaxError("Invalid opcode grammar: '" + opcode + "'", eOPCODE);
			}
			
			String operands = matcher.group(2).trim(); // the 2nd group
			if (operands.startsWith(",") || operands.endsWith(",") || operands.contains(",,")) {
				throw new SyntaxError("Unexpected comma in operands");
			}
			
			String[] operandArray = operands.split("\\s*,\\s*");
			if (operandArray.length > 2) {
				throw new SyntaxError("Expected at most 2 operands, but received " + operandArray.length + "!");
			}
			
			// extract operands
			String firstOp = operandArray.length > 0 ? operandArray[0].trim() : null;
			String secondOp = operandArray.length > 1 ? operandArray[1].trim() : null;
			
			// basically instruction op1, op2 (they can be null at the same time)
			return new String[] { 
				opcode, 
				(firstOp == null || firstOp.isEmpty()) ? null : firstOp, // first op cannot be null, regardless
				secondOp // second op is handled already
			};
		}
		throw new SyntaxError("Expected format is 'INSTRUCTION [OPERAND1, OPERAND2]'", eENTIRE_LINE);
	}
	
    /**
     * Assembles an instruction by appending opcode and operand bytes to the program.
     * @param first true if first operand, false if second
     * @param type expected operand type (REGISTER or NUMBER)
     * @param value operand value
     * @throws SyntaxError if operand is invalid
     */
	public static void assembleOperand(boolean first, int type, String value) throws SyntaxError {
		if (type == OPERAND_NUMBER) {
			char num16_t = parseNumbers(first, value);
			assembledProgram.add((byte) (num16_t >> 8));
			assembledProgram.add((byte) (num16_t & 0xFF));
			return;
		}
		if (type == OPERAND_REGISTER) {
			assembledProgram.add((byte) 0x00);
			assembledProgram.add((byte) parseRegister(first, value));
			return;
		}
		// for -1
		assembledProgram.add((byte) 0x00);
		assembledProgram.add((byte) 0x00);
	}
	
	// Keeps track of the total number of bytes assembled so far. 
	// This is used to determine instruction addresses in memory, 
	// particularly for labels (directives) and branching instructions.
	public static int ASSEMBLED_BYTES = 0; 

	// A mapping of labels (directives) to their corresponding memory addresses. 
	// Labels (e.g., ".main") serve as reference points for jumps (JMP, IFEQ, etc.).
	// When an instruction references a label, the assembler looks up its address in this map.
	public static Map<String, Integer> directiveToAddress = new HashMap<>();

	// Stores the assembled machine code as a list of bytes. 
	// Each instruction and its operands are converted into their corresponding 
	// byte representation and stored in this list for output or execution.
	public static List<Byte> assembledProgram = new ArrayList<>();
	
    /**
     * Assembles the given source code.
     * @param fileName The name of the source file.
     * @param lines The source code lines.
     */
	public static void assemble(String fileName, String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			int lineNumber = i + 1;
			String line = lines[i];
			try {
				if (line.startsWith(".")) {
					directiveToAddress.put(line.substring(1), ASSEMBLED_BYTES);
					continue;
				}
				// parse the instruction
				String[]   parsedInstruction = parseLine(line);
				Opcode	   opcode			 = OPCODE_INFO.get(parsedInstruction[0]);
				
				// invalid opcode provided!
				if (opcode == null) {
					throw new SyntaxError("The '" + parsedInstruction[0] + "' opcode does not exist!", eOPCODE);
				}
				
				// only allow branching instructions to use this directive feature
				int opcodeValue = opcode.getCode();
				if (opcodeValue == FL516CPU.JMP  // unconditional jump
				 || opcodeValue == FL516CPU.IFEQ  // jump if equal
				 || opcodeValue == FL516CPU.IFGT // jmp if greater than
				 || opcodeValue == FL516CPU.IFLT // jmp if less than
				) {
					// replace .[directive] with the memory address of directive
					for (int oprIndex = 1; oprIndex <= 2; oprIndex++) {
						String operand = parsedInstruction[oprIndex];
						if (operand == null || !operand.startsWith(".")) continue;
						// attempt to find the directive assigned address
						Integer addressMap = directiveToAddress.get(operand.substring(1));
						if (addressMap == null) {
							throw new SyntaxError("Undefined directive '" + operand + "'", oprIndex);
						}
						// override the directive stirng by the actual address
						parsedInstruction[oprIndex] = String.valueOf(addressMap);
					}
				}
				
				// prechecks for the two operands
				if (!isTypeCorrect(parsedInstruction[1], opcode.firstOperandType())) {
					throw new SyntaxError(generateError(parsedInstruction[0], opcode), eFIRST_OPR);
				}
				if (!isTypeCorrect(parsedInstruction[2], opcode.secndOperandType())) {
					throw new SyntaxError(generateError(parsedInstruction[0], opcode), eSECND_OPR);
				}
				
				// append opcode
				assembledProgram.add((byte) (opcode.getCode() & 0xFF));
				// append first operand
				assembleOperand(true , opcode.firstOperandType(), parsedInstruction[1]);
				assembleOperand(false, opcode.secndOperandType(), parsedInstruction[2]);
				
				// increment by 5
				ASSEMBLED_BYTES += 5;
			} catch (SyntaxError syntaxError) {
				reportAssemblerError(fileName, lineNumber, line, syntaxError);
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Cleanup a line for error reporting
	 */
	public static String tidyLine(String asm) {
		return asm.split(";")[0].trim().replaceAll("\\s+", " ");	
	}
	
	/**
	 * Report an assembler error (Syntax Error)
	 */
	public static void reportAssemblerError(String fileName, int lineNumber, String line, SyntaxError e) {
	    String ln = "| " + lineNumber + " | ";
	    String tidiedLine = tidyLine(line);

	    StringBuilder errorMessage = new StringBuilder();
	    errorMessage.append("File \"").append(fileName).append("\"").append(", line ").append(lineNumber).append(", assembler").append(":\n");
	    errorMessage.append(ln).append(line).append("\n");
	    errorMessage.append(" ".repeat(ln.length())).append("~".repeat(line.length())).append("\n");

	    errorMessage.append("Instruction at line ").append(lineNumber).append(", instruction address ").append(String.format("0x%04X", ASSEMBLED_BYTES)).append(":\n");
	    errorMessage.append(ln).append(tidiedLine).append("\n");

	    int errorPosition = getErrorPosition(e.errorLevel, ln.length());
	    if (errorPosition != -1) {
	        errorMessage.append(" ".repeat(errorPosition)).append("~".repeat(3)).append("\n");
	        errorMessage.append(" ".repeat(errorPosition + 1)).append("^\n");
	    }

	    errorMessage.append("SyntaxError: ").append(e.getMessage());

	    System.err.println(errorMessage);
	}

	private static int getErrorPosition(int errorLevel, int baseOffset) {
	    return switch (errorLevel) {
	        case eENTIRE_LINE -> baseOffset;
	        case eOPCODE -> baseOffset;
	        case eFIRST_OPR -> baseOffset + 4;
	        case eSECND_OPR -> baseOffset + 8;
	        default -> -1;
	    };
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
	
	public static void main(String... args) throws SyntaxError {
		String[] asm = {
			".main",
			"nop ; hiii",
			"hlt ; hiii",
			"no ; hiii",
			".sex",
			".a",
			"jmp .a"
		};
		assemble("main.asm", asm);
		for (byte b : assembledProgram) {
			System.out.printf("%02X ", (int) b & 0xFF);
		}
	}
}
