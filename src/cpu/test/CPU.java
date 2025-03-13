package cpu.test;

public class CPU {
	/** opcode labels **/
	// flow controls
	public static final int NOP  = 0x00;
	public static final int HLT  = 0xFF;
	public static final int JMP  = 0xFE;
	public static final int CMP  = 0xFD;
	public static final int IFEQ = 0xFC;
	public static final int IFLT = 0xFB;
	public static final int IFGT = 0xFA;
	// reserved
	public static final int DBGP = 0xF9; // debug cpu pause
	// data controls
	public static final int MOV  = 0x01;
	public static final int LDI  = 0x02;
	public static final int LDM  = 0x03;
	public static final int STO  = 0x04;
	public static final int STOI = 0x05;
	public static final int MBS  = 0x06;
	// arithmetic & bitwise controls
	public static final int ADD  = 0x07;
	public static final int SUB  = 0x08;
	public static final int ADI  = 0x09;
	public static final int SUBI = 0x0A;
	public static final int MUL  = 0x0B;
	public static final int DIV  = 0x0C;
	public static final int AND  = 0x0D;
	public static final int OR   = 0x0E;
	public static final int XOR  = 0x0F;
	public static final int NOT  = 0x10;
	public static final int BSL  = 0x11;
	public static final int BSR  = 0x12;
	// special instructions
	
	// registers
	static char[] REGS = new char[12];
	// MARKINGS
	static final int RMB = REGS.length - 1; // memory bank
	static final int RSP = REGS.length - 2; // stack pointer
	static final int RDR = REGS.length - 3; // division remainder
	static final int RRM = REGS.length - 4; // reserved multipurpose register
	
	@Deprecated
	static char[][] LTS = new char[256][256];
	static char[][] RAM = new char[256][256];
	static boolean  ZFL = false;  // zero flag
	static boolean  CFL = true;   // carry flag
	
	// program counter
	static int PC = 0;
	static int ILENGTH = 3;
	
	static boolean paused = false;
	static char[] ROM = {
		LDI, 6, 0,
		LDI, 4, 1,
		//JMP, 12, 0,
		ADD, 0, 1,
		HLT, 0, 0
	};
		
	
	public static void main(String[] args) throws InterruptedException {
		copy_rom_to_ram();
		cpu_loop: while (true) {
			Thread.sleep(2);
			
			if (paused) continue;
			
			char opcode = get_mem(PC++);
			char opr1 = get_mem(PC++);
			char opr2 = get_mem(PC++);
			
			System.out.println("[CPU | PC=" + PC +"] OPCODE: " + String.format("%02X", (int)opcode) 
				+ " OPERANDS: " + String.format("%02X", (int)opr1) 
				+ ", " + String.format("%02X", (int)opr2) 
			);
			
			// CPU DEBUG
			// DBGP [0 for hex, 1 for dec | REGS DUMP] [0 for hex, 1 for dec | MEMDMP]
			if (opcode == DBGP) {
				System.out.println("[CPU | DBG] Paused Execution for Debug! Dumping...");
				printRegisters((int)opr1 == 0);
				printMemory((int)opr2 == 0);
				paused = true;
				continue;
			}
			
			// NOP 00 00
			// empty instruction
			if (opcode == NOP) {
				continue;
			}
			
			/** CPU SIGNALS (control flow) **/
			// HLT 00, 00
			// Stop the CPU from executing
			if (opcode == HLT) {
				System.out.println("[CPU | SIG] HLT opcode found! Halting");
				break cpu_loop;
			}
			
			// JMP [Program counter / address]
			// jump to an address in the program instructions space
			if (opcode == JMP) {
				if (opr1 % ILENGTH != 0) {
					System.err.println("[CPU | JMP WARN] Jump location is not multiple of 3. Undefined behaviour may happen!");
				}
				PC = opr1;
				continue;
			}
			
			// CMP [Reg 1] [Reg 2]
			// compare register 1 and 2 by REG1 - REG2
			// if the result is == 0, Z_FLAG is set
			// if the result is negative, C_FLAG is set
			if (opcode == CMP) {
				int result = (int)REGS[opr1] - (int)REGS[opr2];
				ZFL = result == 0;
				CFL = result < 0;
				continue;
			}
			
			// IFEQ [Program counter] ; IF EQUALS
			// jump to an address in the program instructions space IF
			// and only if the Z_FLAG (ZERO) is set (result of cmp equals)
			if (opcode == IFEQ) {
				if (ZFL) PC = opr1;
				continue;
			}
			
			// IFLT [Program counter] ; IF LESS THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is set (result of cmp is: A less than B)
			if (opcode == IFLT) {
				if (CFL) PC = opr1;
				continue;
			}
			
			// IFGT [Program counter] ; IF GREATER THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is CLEAR (result of cmp is: A greater than B)
			if (opcode == IFGT) {
				if (!CFL) PC = opr1;
				continue;
			}
			
			/** CPU DATA MANIPULATION **/
			// MOV REG_A, REG_B
			if (opcode == MOV) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				// move stuff from register A to B
				REGS[opr2] = REGS[opr1];
				// assign B as A
				continue;
			}
			
			// LDI IMMEDIATE_VALUE, REG_INDEX
			// load immediate value to a register
			if (opcode == LDI) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				// assign immediate value (op1) to register[op2]
				REGS[opr2] = opr1;
				continue;
			}
			
			// LDM [MEM ADDRESS], REG_INDEX
			// load from memory address to a register
			if (opcode == LDM) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				int mem_bank_index = REGS[REGS.length - 1];
				// assign memory[mbi][opr1] to register[opr2]
				REGS[opr2] = RAM[mem_bank_index][opr1];
				continue;
			}
			
			// STO REG_INDEX, [MEM ADDRESS]
			// store a register value to a memory address
			if (opcode == STO) {
				// assign a register[opr1] to memory[mbi][opr2]
				int mem_bank_index = REGS[REGS.length - 1];
				RAM[mem_bank_index][opr2] = REGS[opr1];
				continue;
			}
			
			// STOI IMMEDIATE_VALUE, [MEM ADDRESS]
			// store an immediate value to a memory address
			if (opcode == STOI) {
				// assign a immediate value (opr1) to memory[mbi][opr2]
				int mem_bank_index = REGS[REGS.length - 1];
				RAM[mem_bank_index][opr2] = opr1;
				continue;
			}
			
			// MBS TARGET_BANK
			// memory bank switch instruction (change the last register)
			if (opcode == MBS) {
				// switch the memory bank to a new bank
				REGS[REGS.length - 1] = opr1;
				continue;
			}
			
			/**** ARITHMETIC OPERATIONS ****/
			// READ AS IT IS, DO OPERATIONS AND PUT RESULT TO BX
			// ADD AX, BX is basically ADD AX TO BX
			
			// ADD REG_INDEX, REG_2_INDEX
			// add value from reg 1 to reg2
			if (opcode == ADD) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				REGS[opr2] = (char)((REGS[opr1] + REGS[opr2]) % 256);
				continue;
			}
			
			// SUB REG_INDEX, REG_2_INDEX
			// subtract VALUE of reg1 from reg2
			if (opcode == SUB) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				// SUB AX, BX
				REGS[opr2] = (char)((int)((char)(REGS[opr2] - REGS[opr1])) % 256);
				continue;
			}
			
			// ADI IMMEDIATE_VALUE, REG_INDEX
			// add immediate value to a register
			if (opcode == ADI) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				REGS[opr2] = (char)((opr1 + REGS[opr2]) % 256);
				continue;
			}
			
			// SUBI IMMEDIATE_VALUE, REG_INDEX
			// subtract immediate value from a register
			if (opcode == SUBI) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				REGS[opr2] = (char)((int)((char)(REGS[opr2] - opr1)) % 256);
				continue;
			}
			
			// MUL REG_INDEX, REG_2_INDEX
			// multiply reg2 by reg1, store result in REG 2
			if (opcode == MUL) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr2)) break cpu_loop;
				
				// MUL AX, BX
				REGS[opr2] = (char)((REGS[opr1] * REGS[opr2]) % 256);
				continue;
			}
			
			// DIV REG_INDEX, REG_2_INDEX
			// divide register 1 by register 2, store quotient in reg 1 and
			// remainder in register RDR
			// reg1 /= reg2 (int)
			// rdr = reg1 % reg2
			if (opcode == DIV) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr1)) break cpu_loop;
				
				int reg_1_value = REGS[opr1];
				
				REGS[opr1] = (char) (reg_1_value / REGS[opr2]);
				REGS[RDR ] = (char) (reg_1_value % REGS[opr2]);
				
				continue;
			}
			
			/**** BITWISE OPERATION, ASSIGN RESULT TO THE FIRST REGISTER ****/
			// AX = AX & BX
			
			// AND REG_INDEX, REG_2_INDEX
			// basically REG_1 = REG_1 & REG_2 
			if (opcode == AND) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr1)) break cpu_loop;
				
				REGS[opr1] &= REGS[opr2];
				continue;
			}
			
			// OR REG_INDEX, REG_2_INDEX
			// basically REG_1 = REG_1 | REG_2 
			if (opcode == OR) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr1)) break cpu_loop;
				
				REGS[opr1] |= REGS[opr2];
				continue;
			}
			
			// XOR REG_INDEX, REG_2_INDEX
			// basically REG_2 = REG_1 ^ REG_2
			if (opcode == XOR) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr1)) break cpu_loop;
				
				REGS[opr1] ^= REGS[opr2];
				continue;
			}
			
			// NOT REG_INDEX
			// basically REG_1 = ~REG_1
			if (opcode == NOT) {
				// crash the CPU if register access is reserved
				if (is_reserved(opr1)) break cpu_loop;
				
				REGS[opr1] = (char) ~REGS[opr1];
				continue;
			}
			
			System.err.printf("[CPU | FAULT] Unknown OPCODE: %02X\n", (int) opcode);
			break;
		}
		
		System.out.println("cpu stopped or crashed, heres da dump");
		printRegisters(false);
		printMemory(false);
	}
	
	private static String[] registersName = {
		"AX", "BX", "CX", "DX", "EX", "FX", "GX", "HX",
		"RM", // reserved all purpose register
		"DR", // divison remainder
		"SP", // stack pointer
		"MB" // reserved register for memory bank INDEXING
	};
	
	private static void printRegisters(boolean hex) {
		System.out.println("\n[CPU | DUMP] CPU Registers (RAX = 00, RBX = 01, ...)");
		System.out.print("|");
		for (int i = 0; i < REGS.length; i++) {
			System.out.print(" R" + registersName[i] + " |");
		}
		
		System.out.println();
		System.out.print("|-----+");
		System.out.print("-----+".repeat(REGS.length - 2));
		System.out.print("-----|");
		System.out.print("\n|");
		
		for (int i = 0; i < REGS.length; i++) {
			System.out.printf(hex ? "  %02X |" : " %03d |", (int) REGS[i]);
		}
		System.out.println("\n");
	}
	
	@SuppressWarnings("resource")
	private static void printMemory(boolean hex) {
		for (int i = 0; i < RAM.length / 16; i++) {
			System.out.printf("[SRAM | DUMP] Dump for SRAM Bank 0x%02X\n", i);
			for (int j = 0; j < 256; j++) {
				if (j % 16 == 0) System.out.println();
				
				boolean is_program = (i * 256) + j < ROM.length;
				var std = is_program ? System.err : System.out;
				
				std.printf(hex ? "%02X " : "%03d ", (int)RAM[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}
	
	public static void copy_rom_to_ram() {
		for (int i = 0; i < ROM.length; ++i) {
			RAM[(int)Math.floor(i / 256)][i % 256] = (char)((int)ROM[i] % 256);
		}
	}
	
	static class mem_addr {
		char bank;
		char bank_idx;
	};
	
	static mem_addr map_from(int address) {
		mem_addr addr = new mem_addr();
		addr.bank = (char)Math.floor(address / 256);
		addr.bank_idx = (char) (address % 256);
		return addr;
	}
	
	static char get_mem(int address) {
		mem_addr m = map_from(address);
		return RAM[m.bank][m.bank_idx];
	}
	
	@Deprecated
	static char get_lts(int address) {
		mem_addr m = map_from(address);
		return LTS[m.bank][m.bank_idx];
	}

	static boolean is_reserved(int reg) {
		return reg == RDR || reg == RMB || reg == RRM || reg == RSP;
	}
}
