package cpu.test.assembler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpcodeInfo {
	public static final Map<String, Opcode> OPCODE_INFO = new HashMap<>();
	
	public static final int OPERAND_REGISTER = 0;
	public static final int OPERAND_NUMBER   = 1;
	
	static {
		// data controls
		OPCODE_INFO.put("MOV",  new Opcode(0x01,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("LDI",  new Opcode(0x02,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		OPCODE_INFO.put("LDM",  new Opcode(0x03,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		OPCODE_INFO.put("STO",  new Opcode(0x04,  List.of(OPERAND_NUMBER, OPERAND_REGISTER)));
		OPCODE_INFO.put("STOI", new Opcode(0x05,  List.of(OPERAND_NUMBER, OPERAND_NUMBER)));
		// addition
		OPCODE_INFO.put("ADD",  new Opcode(0x07,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ADDI",  new Opcode(0x08,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// subtraction
		OPCODE_INFO.put("SUB",  new Opcode(0x09,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SUBI", new Opcode(0x0A,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// multiplication
		OPCODE_INFO.put("MUL",  new Opcode(0x0B,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("MULI", new Opcode(0x0C,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// division
		OPCODE_INFO.put("DIV",  new Opcode(0x0D,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("DIVI", new Opcode(0x0E,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise AND
		OPCODE_INFO.put("AND",  new Opcode(0x0F,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ANDI", new Opcode(0x10,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise OR
		OPCODE_INFO.put("OR",   new Opcode(0x11,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ORI",  new Opcode(0x12,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise XOR
		OPCODE_INFO.put("XOR",  new Opcode(0x13,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("XORI", new Opcode(0x14,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise SHIFT RIGHT
		OPCODE_INFO.put("SHR",  new Opcode(0x15,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SHRI", new Opcode(0x16,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise SHIFT LEFT
		OPCODE_INFO.put("SHL",  new Opcode(0x17,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SHLI", new Opcode(0x18,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise NOT
		OPCODE_INFO.put("NOT",  new Opcode(0x19,  List.of(OPERAND_REGISTER)));
		// stack manipulation
		OPCODE_INFO.put("PUSH", new Opcode(0x1A,  List.of(OPERAND_REGISTER)));
		OPCODE_INFO.put("IPUSH",new Opcode(0x1B,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("POP",  new Opcode(0x1C,  List.of(OPERAND_REGISTER)));
		// control flow
		OPCODE_INFO.put("HLT",  new Opcode(0xF0,  List.of()));
		OPCODE_INFO.put("JMP",  new Opcode(0xF1,  List.of(OPERAND_NUMBER)));
		// compare (allow branch to be used *properly* after this)
		OPCODE_INFO.put("CMP",  new Opcode(0xF2,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		// branch condition
		OPCODE_INFO.put("JEQ", new Opcode(0xF3,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JLT", new Opcode(0xF4,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JGT", new Opcode(0xF5,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JLE", new Opcode(0xF6,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JGE", new Opcode(0xF7,  List.of(OPERAND_NUMBER)));
		// reserved
		OPCODE_INFO.put("NOP",  new Opcode(0x00,  List.of()));
		OPCODE_INFO.put("DBGP", new Opcode(0xFF,  List.of()));
		OPCODE_INFO.put("BLNK", new Opcode(0x06,  List.of()));
	}
	
	public static class Opcode {
		private final int code;
		private final List<Integer> operands;
		
		public Opcode(int code, List<Integer> operands) {
			this.code = code;
			this.operands = operands;
		}
		
		public int getCode() {
			return code;
		}
		
		public List<Integer> getOperands() {
			return operands;
		}
		
		public int firstOperandType() {
			return operands.size() >= 1 ? operands.get(0) : -1;
		}
		
		public int secndOperandType() {
			return operands.size() >= 2 ? operands.get(1) : -1 ;
		}
		
		public String firstOperandTypeName() {
			return firstOperandType() == OPERAND_REGISTER ? "register" : "immediate value";
		}
		
		public String secndOperandTypeName() {
			return secndOperandType() == OPERAND_REGISTER ? "register" : "immediate value";
		}
	}
}