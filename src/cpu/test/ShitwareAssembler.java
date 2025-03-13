package cpu.test;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShitwareAssembler {
	public static byte parseRegister(String operand) throws SyntaxError {
		// ignore case
		operand = operand.toUpperCase();
		// get the registers check out
		if (!operand.startsWith("R")) {
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
			throw new SyntaxError("'" + operand + "' is not a valid register operand! Valid general-purpose registers range from R0 to R7.");
		}
	}
	
	public static char parseNumbers(String operand) throws SyntaxError {
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
			throw new SyntaxError("'" + operand + "' is not a valid number operand! Valid examples: 0xCAFE, 0b10101, 128.");
		}
	}
	
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
			// extract the instruction name by getting the first group
			String instructionName = matcher.group(1).toUpperCase();
			// check for abnormal characters in the instruction name
			if (instructionName.matches(".*[;,.:\\[\\]{}|*()%].*")) {
				throw new SyntaxError("Invalid instruction grammar: '" + instructionName + "'");
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
				instructionName, 
				(firstOp == null || firstOp.isEmpty()) ? null : firstOp, // first op cannot be null, regardless
				secondOp // second op is handled already
			};
		}
		throw new SyntaxError("Expected format is \"INSTRUCTION [OPERAND1, OPERAND2]\"");
	}
	
	public static void main(String... args) throws SyntaxError {
		String asm = "s r0,r2";
		System.out.println(Arrays.toString(parseLine(asm)));
		System.out.println((int)parseRegister("Rmd"));
	}
}
