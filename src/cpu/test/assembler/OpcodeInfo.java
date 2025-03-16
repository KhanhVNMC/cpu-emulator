package cpu.test.assembler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static cpu.test.FL516CPU.*;

public class OpcodeInfo {
	public static final Map<String, Opcode> OPCODE_INFO = new HashMap<>();
	
	public static final int OPERAND_REGISTER = 0;
	public static final int OPERAND_NUMBER   = 1;
	
	static {
		// data controls
		OPCODE_INFO.put("MOV",  new Opcode(MOV,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("LDI",  new Opcode(LDI,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		OPCODE_INFO.put("LMH",  new Opcode(LMH,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("LMB",  new Opcode(LMB,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SMH",  new Opcode(SMH,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SMB",  new Opcode(SMB,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		// addition
		OPCODE_INFO.put("ADD",  new Opcode(ADD,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ADDI", new Opcode(ADDI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// subtraction
		OPCODE_INFO.put("SUB",  new Opcode(SUB,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SUBI", new Opcode(SUBI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// multiplication
		OPCODE_INFO.put("MUL",  new Opcode(MUL,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("MULI", new Opcode(MULI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// division
		OPCODE_INFO.put("DIV",  new Opcode(DIV,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("DIVI", new Opcode(DIVI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// division
		OPCODE_INFO.put("MOD",  new Opcode(MOD,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("MODI", new Opcode(MODI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise AND
		OPCODE_INFO.put("AND",  new Opcode(AND,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ANDI", new Opcode(ANDI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise OR
		OPCODE_INFO.put("OR",   new Opcode(OR,   List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("ORI",  new Opcode(ORI,  List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise XOR
		OPCODE_INFO.put("XOR",  new Opcode(XOR,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("XORI", new Opcode(XORI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise SHIFT RIGHT
		OPCODE_INFO.put("SHR",  new Opcode(SHR,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SHRI", new Opcode(SHRI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise SHIFT LEFT
		OPCODE_INFO.put("SHL",  new Opcode(SHL,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		OPCODE_INFO.put("SHLI", new Opcode(SHLI, List.of(OPERAND_REGISTER, OPERAND_NUMBER)));
		// bitwise NOT
		OPCODE_INFO.put("NOT",  new Opcode(NOT,  List.of(OPERAND_REGISTER)));
		// stack manipulation
		OPCODE_INFO.put("PUSH", new Opcode(PUSH, List.of(OPERAND_REGISTER)));
		OPCODE_INFO.put("IPUSH",new Opcode(IPUSH,List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("POP",  new Opcode(POP,  List.of(OPERAND_REGISTER)));
		// control flow
		OPCODE_INFO.put("HLT",  new Opcode(HLT,  List.of()));
		OPCODE_INFO.put("JMP",  new Opcode(JMP,  List.of(OPERAND_NUMBER)));
		// compare (allow branch to be used *properly* after this)
		OPCODE_INFO.put("CMP",  new Opcode(CMP,  List.of(OPERAND_REGISTER, OPERAND_REGISTER)));
		// branch condition
		OPCODE_INFO.put("JEQ",  new Opcode(JEQ,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JNE",  new Opcode(JNE,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JLT",  new Opcode(JLT,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JGT",  new Opcode(JGT,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JLE",  new Opcode(JLE,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("JGE",  new Opcode(JGE,  List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("CALL", new Opcode(CALL, List.of(OPERAND_NUMBER)));
		OPCODE_INFO.put("RET",  new Opcode(RET,  List.of()));
		// memory management unit
		OPCODE_INFO.put("VMS",  new Opcode(VMS, List.of()));
		OPCODE_INFO.put("WMS",  new Opcode(WMS, List.of()));
		// reserved
		OPCODE_INFO.put("NOP",  new Opcode(NOP,  List.of()));
		OPCODE_INFO.put("DBGP", new Opcode(DBGP, List.of()));
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