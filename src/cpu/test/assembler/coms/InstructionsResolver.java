package cpu.test.assembler.coms;

import static cpu.test.assembler.OpcodeInfo.*;
import static cpu.test.assembler.SyntaxError.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpu.test.FL516CPU;
import cpu.test.assembler.SyntaxError;
import cpu.test.assembler.OpcodeInfo.Opcode;

import static cpu.test.assembler.ShitwareAssembler.*;

public class InstructionsResolver {
	// A mapping of labels (directives) to their corresponding memory addresses. 
	// Labels (e.g., ".main") serve as reference points for jumps (JMP, IFEQ, etc.).
	// When an instruction references a label, the assembler looks up its address in this map.
	public static Map<String, Integer> labelsToAddress = new HashMap<>();
	
	// A temporary map to store parsed lines from the "instruction parsing" step, orders matter.
	// This map will then be used by the actual assembler process to assemble instructions
	public static Map<Integer, String[]> parsedLines = new LinkedHashMap<>();

	// Stores the assembled machine code as a list of bytes. 
	// Each instruction and its operands are converted into their corresponding 
	// byte representation and stored in this list for output or execution.
	public static List<Byte> assembledTextSection = new ArrayList<>();
	
	public static boolean isTypeCorrect(String operand, int expected) {
		if (expected == -1 && operand == null) return true; // no operand expected, and none provided.
		if (expected != -1 && operand == null) return false; // operand expected, but none provided.
		boolean isRegister = isRegister(operand);
		if (expected == OPERAND_REGISTER && isRegister) return true; // expected register, got register.
		if (expected == OPERAND_NUMBER && !isRegister) return true; // expected number, got number.
		return false; // operand type mismatch.
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
		try {
			int register = Integer.parseInt(targetRegister);
			if (register < 0 || register > 8) throw new NumberFormatException();
			return (byte) (register & 0xFF);
		} catch (NumberFormatException e) {
			throw new SyntaxError("'" + operand + "' is not a valid register operand!\nValid general-purpose registers range from R0 to R8 ('RSP' for stack register).", firstOperand ? eFIRST_OPR : eSECND_OPR);
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
		try {
			return parseNumeric(operand);
		} catch (Exception e) {
			throw new SyntaxError("\"" + operand + "\" is not a valid number operand!\nValid character literals: 'a', 'b'. Numeric examples: 0xCAFE, 0b10101, 128.", firstOperand ? eFIRST_OPR : eSECND_OPR);
		}
	}
	
	/**
     * Parses an assembly line into opcode and operands.
     * @param asm the assembly instruction line
     * @return an array containing opcode, first operand, and second operand (if present)
     * @throws SyntaxError if syntax is incorrect
     */
	private static String[] parseLine(String asm) throws SyntaxError {
		// ignore comments and blank lines
		if (asm.trim().isBlank() || asm.trim().startsWith(";")) return null;
		
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
			if (opcode.matches(".*[;,.:\\[\\]{}|*()%#].*")) {
				throw new SyntaxError("Invalid opcode grammar: '" + opcode + "', invalid character(s) found!", eOPCODE);
			}
			
			String operands = matcher.group(2).trim(); // the 2nd group
			if (operands.startsWith(",") || operands.endsWith(",") || operands.contains(",,")) {
				throw new SyntaxError("Unexpected comma in operands", eFIRST_OPR);
			}
			
			String[] operandArray = operands.split("\\s*,\\s*", 2);
			//System.out.println(operands + "; Tokens: " + Arrays.toString(operandArray));
			if (operandArray.length > 2) {
				throw new SyntaxError("Expected at most 2 operands, but received " + operandArray.length + "!", eSECND_OPR);
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
			assembledTextSection.add((byte) (num16_t >> 8)); // high byte
			assembledTextSection.add((byte) (num16_t & 0xFF)); // low byte (big endian)
			return;
		}
		if (type == OPERAND_REGISTER) {
			assembledTextSection.add((byte) 0x00); // omitted
			assembledTextSection.add((byte) parseRegister(first, value));
			return;
		}
		// for -1 (none)
		assembledTextSection.add((byte) 0x00);
		assembledTextSection.add((byte) 0x00);
	}
	
	public static void assembleFromParsedLines(String fileName, String[] originalLines) {
		for (Map.Entry<Integer, String[]> lineEntry : parsedLines.entrySet()) {
			String[] parsedInstruction = lineEntry.getValue();
			Opcode	 opcode		       = OPCODE_INFO.get(parsedInstruction[0]);
			
			try {
				// invalid opcode provided!
				if (opcode == null) {
					throw new SyntaxError("The '" + parsedInstruction[0] + "' opcode does not exist!", eOPCODE);
				}
				
				// only allow branching instructions to use this labelling feature
				int opcodeValue = opcode.getCode();
				if (opcodeValue == FL516CPU.JMP  // unconditional jump
				 || opcodeValue == FL516CPU.JEQ  // jump if equal
				 || opcodeValue == FL516CPU.JNE  // jump if not equal
				 || opcodeValue == FL516CPU.JGT  // jmp if greater than
				 || opcodeValue == FL516CPU.JLT  // jmp if less than
				 || opcodeValue == FL516CPU.JGE  // jmp if greater or equal
				 || opcodeValue == FL516CPU.JLE  // jmp if lesser or equal
				 || opcodeValue == FL516CPU.CALL // function call (the same as JMP but with RET)
				) {
					// replace .[label] with the memory address of label
					for (int oprIndex = 1; oprIndex <= 2; oprIndex++) {
						String operand = parsedInstruction[oprIndex];
						if (operand == null || !operand.startsWith(".")) continue;
						// attempt to find the label assigned address
						Integer addressMap = labelsToAddress.get(operand.substring(1).trim());
						if (addressMap == null) {
							throw new SyntaxError("Undefined label '" + operand + "'\nEnsure the label is defined somewhere in the source.", oprIndex);
						}
						// override the label stirng by the actual address
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
				assembledTextSection.add((byte) (opcode.getCode() & 0xFF));
				// append first operand
				assembleOperand(true , opcode.firstOperandType(), parsedInstruction[1]);
				assembleOperand(false, opcode.secndOperandType(), parsedInstruction[2]);
				
				// increment by 5 (one instruction)
				ASSEMBLED_BYTES += 5;
			} catch (SyntaxError syntaxError) {
				reportAssemblerError(fileName, "assembler", lineEntry.getKey(), originalLines[lineEntry.getKey() - 1], syntaxError);
				throw new RuntimeException("Aborted Task");
			}
		}
	}
	
	public static void parseTextSectionLine(String fileName, String original, int lineNumber) throws RuntimeException {
		String line = stripInlineComment(original).trim();
		try {
			// if the line starts with "."
			if (line.startsWith(".")) {
				String label = line.substring(1).trim();
				// label colon is optional, but a warning is issued if omitted
				boolean warn = true;
				if (line.endsWith(":")) {
					warn = false;
					// remove the colon while parsing name
					label = label.substring(0, label.length() - 1);
				}
				// check for illegal chars
				if (label.matches(".*[;,.:\\[\\]{}|*()%#].*")) {
					throw new SyntaxError("Invalid label grammar, special characters detected!", eENTIRE_LINE);
				}
				// after passed the precheck, raise a warning if applicable
				if (warn) {
					System.err.println("File \"" + fileName + "\", line " + lineNumber + ", warning:");
					System.err.println("   Recommended: Label '" + label + "' should end with a colon (':').");
				}
				// get the label address
				labelsToAddress.put(label, ASSEMBLED_BYTES);
				return;
			}
			
			// parse the instruction
			String[] parsedInstruction = parseLine(line);
			if (parsedInstruction == null) return; // ignored
			parsedLines.put(lineNumber, parsedInstruction);
		
			// increment by 5
			ASSEMBLED_BYTES += 5;
		} catch (SyntaxError syntaxError) {
			reportAssemblerError(fileName, "instruction parser", lineNumber, original, syntaxError);
			throw new RuntimeException("Aborted Task");
		}
	}
}
