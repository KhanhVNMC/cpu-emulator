package cpu.test;

public class FL516CPU {
	/** opcode labels **/
	/* data controls */
	public static final int MOV  = 0x01; // move from registers to registers
	public static final int LDI  = 0x02; // load immediate (load a value to reg)
	public static final int LDM  = 0x03; // load a value from memory to reg
	public static final int STO  = 0x04; // write to memory from register
	public static final int STOI = 0x05; // write to memory from immediate value
	public static final int BLNK = 0x06; // blank instruction
	
	/* arithmetic & bitwise controls*/
	// ADDITION
	public static final int ADD  = 0x07;
	public static final int ADI  = 0x08; // ADD IMMEDIATE
	// SUBTRACTION
	public static final int SUB  = 0x09;
	public static final int SUBI = 0x0A; // SUBTRACT IMMEDIATE
	// MULTIPLICATION
	public static final int MUL  = 0x0B;
	public static final int MULI = 0x0C; // MULTIPLY IMMEDIATE
	// DIVISON
	public static final int DIV  = 0x0D;
	public static final int DIVI = 0x0E; // DIVIDE IMMEDIATE
	// BITWISE AND
	public static final int AND  = 0x0F;
	public static final int ANDI = 0x10; // AND IMMEDIATE
	// BITWISE OR
	public static final int OR   = 0x11;
	public static final int ORI  = 0x12; // OR IMMEDIATE
	// BITWISE XOR
	public static final int XOR  = 0x13;
	public static final int XORI = 0x14; // XOR IMMEDIATE
	// BITWISE SHIFT RIGHT (UNSIGNED)
	public static final int SHR  = 0x15;
	public static final int SHRI = 0x16; // SHIFT RIGHT IMMEDIATE
	// BITWISE SHIFT LEFT (UNSIGNED)
	public static final int SHL  = 0x17;
	public static final int SHLI = 0x18; // SHIFT LEFT IMMEDIATE
	// BITWISE NOT (flip flop)
	public static final int NOT  = 0x19;
	
	/* stack operations */
	public static final int PUSH = 0x1A;
	public static final int IPUSH = 0x1B; // PUSH immediate
	public static final int POP  = 0x1C;
	
	/* flow controls */
	public static final int NOP  = 0x00; // NONE instruction
	public static final int HLT  = 0xF0; // HALT the CPU
	public static final int JMP  = 0xF1; // JUMP to an address
	public static final int CMP  = 0xF2; // COMPARE (store result in CFL--compare flag and ZFL--zero flag)
	public static final int IFEQ = 0xF3; // IF EQUAL then JUMP
	public static final int IFLT = 0xF4; // IF LESS THAN then JUMP (CFL = TRUE)
	public static final int IFGT = 0xF5; // IF GREATER THAN then JUMP (CFL = FALSE)
	// reserved
	public static final int DBGP = 0xFF; // debug cpu pause
	
	/* CPU Internal parts (registers & memory & internal flags) */
	// registers
	static char[]   REGS = new char[10]; // char = 2bytes = 16-bit registers
	// special registers (convention)
	static int      DIV_REMAINDER = 8;
	static int      STACK_PTR     = 9;
	
	// 16-bit addressable space
	static byte[]   MEMORY       = new byte[0xFFFF + 1]; // basically 65536 addressable bytes
	static int      STACK_REGION = MEMORY.length - 1; // or 0xFFFF
	
	// special CPU flags and program counter
	static boolean  ZFL = false;   // zero flag (for CMP)
	static boolean  CFL = false;   // carry flag (for unsigned)
	static boolean  OFL = false;   // overflow flag
	// the program counter
	static int      PROGRAM_COUNTER = 0;
	
	// for the java emulator
	private static String[] registersName = {
		"AX", "BX", "CX", "DX", "EX", "FX", "GX", "HX", // 8 all purpose registers
		"MD", // divison remainder
		"SP", // stack pointer
	};
	
	static boolean paused = false;
	static char[] ROM = {
		IPUSH, 0XCA,0xFE, 0xCA,0xFE,
		POP,00,00,00,00,
		POP,00,00,00,00,
		HLT,00,00,00,00
	};
	
	public static void main(String[] args) throws InterruptedException {
		// INIT stack to 65536
		REGS[STACK_PTR] = (char) STACK_REGION;
		
		copy_rom_to_ram();
		cpu_loop: while (true) {
			
			if (paused) continue;
			
			// fetch instructions (5 bytes)
			int opcode  = MEMORY[PROGRAM_COUNTER++] & 0xFF; // prevent sign extension
			// merge two consecutive bytes into one 16-bit operand
			// example: CA, FE
			// 1) CA << 8 => CA00  (CA left by 8 bits, pad 8 bits to the right)
			// 2) CA00 | (FE & 0xFF) => CAFE (OR merges) [0xFF to prevent sign extension, only get the 2 bytes)
			char opr1   = (char) ((MEMORY[PROGRAM_COUNTER++] << 8) | (MEMORY[PROGRAM_COUNTER++] & 0xFF));
			char opr2   = (char) ((MEMORY[PROGRAM_COUNTER++] << 8) | (MEMORY[PROGRAM_COUNTER++] & 0xFF));
			
			System.out.println("[CPU | PC=" + PROGRAM_COUNTER +"] OPCODE: " + String.format("%02X", (int)opcode) 
				+ " OPERANDS: " + String.format("%04X", (int)opr1) 
				+ ", " + String.format("%04X", (int)opr2) 
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
				if (opr1 % 5 != 0) {
					System.err.println("[CPU | JMP WARN] Jump location is not multiple of 5. Undefined behaviour may happen!");
				}
				PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// CMP [Reg 1] [Reg 2]
			// compare register 1 and 2 by REG1 - REG2
			// if the result is == 0, Z_FLAG is set
			// if the result is negative, C_FLAG is set
			if (opcode == CMP) {
				int result = (REGS[opr1] & 0xFFFF) - (REGS[opr2] & 0xFFFF);
				ZFL = (result & 0xFFFF) == 0;
				CFL = (result < 0); // there's a borrow (result < 0)
				continue;
			}
			
			// IFEQ [Program counter] ; IF EQUALS
			// jump to an address in the program instructions space IF
			// and only if the Z_FLAG (ZERO) is set (result of cmp equals)
			if (opcode == IFEQ) {
				// if there's a ZERO
				if (ZFL) PROGRAM_COUNTER = opr1; // jump to an address
				continue;
			}
			
			// IFLT [Program counter] ; IF LESS THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is set (result of cmp is: A less than B)
			if (opcode == IFLT) {
				if (CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// IFGT [Program counter] ; IF GREATER THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is CLEAR (result of cmp is: A greater than B)
			if (opcode == IFGT) {
				if (!CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			/** CPU DATA MANIPULATION **/
			// MOV REG_A, REG_B
			if (opcode == MOV) {
				// move stuff from register B to A
				REGS[opr1] = REGS[opr2];
				continue;
			}
			
			// LDI REG_INDEX, IMMEDIATE VALUE
			// load immediate value to a register
			if (opcode == LDI) {
				// assign immediate value (op1) to register[op2]
				REGS[opr1] = opr2;
				continue;
			}
			
			// LDM REG_INDEX, [MEM ADDRESS]
			// Load 16-bit value from two consecutive bytes in memory to a register
			if (opcode == LDM) {
				// MEMORY[opr2] = MSB, MEMORY[opr2+1] = LSB (big-endian model)
				// REG[opr1] = (MEMORY[opr2] << 8) | MEMORY[opr2+1]
				REGS[opr1] = (char) (((MEMORY[opr2] & 0xFF) << 8) | (MEMORY[opr2 + 1] & 0xFF));
				continue;
			}
			
			// STO [MEM ADDRESS], REG_INDEX
			// STOI [MEM ADDRESS], IMMEDIATE_VALUE
			// Store 16-bit register value to two consecutive memory addresses (big-endian model).
			if (opcode == STO || opcode == STOI) {
				char value = opcode == STOI ? opr2 : REGS[opr2];
				MEMORY[opr1]     = (byte) ((value >> 8) & 0xFF); // high byte (cut off 2 low bytes)
				MEMORY[opr1 + 1] = (byte) (value & 0xFF); // low byte (cut off 2 high bytes by AND)
				continue;
			}
			
			/**** ARITHMETIC OPERATIONS ****/
			// ADD AX, BX is ADD AX TO BX AND PUT TO AX
			
			// ADD REG_INDEX, REG_2_INDEX
			// ADI REG_INDEX, IMMEDIATE_VALUE
			// add value from reg 1 to reg2 (or IV)
			if (opcode == ADD || opcode == ADI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == ADI ? opr2 : REGS[opr2]) & 0xFFFF;
				int result = value1 + value2;
				
				// store only the lower 16 bits
				REGS[opr1] = (char) (result & 0xFFFF);
				
				// processor flags
				ZFL = (result & 0xFFFF) == 0; // if equal 0 then set
				CFL = (result > 0xFFFF); // (ovberflow upper USIGN) carry flag
				
				boolean sign1   = (value1 & 0x8000) != 0; // 1 if negative
				boolean sign2   = (value2 & 0x8000) != 0; // 1 if negative
				boolean signRes = (result & 0x8000) != 0; // 1 if negative
				
				OFL = (sign1 == sign2) && (signRes != sign1); // detects signed overflow
				continue;
			}
			
			// SUB REG_INDEX, REG_2_INDEX
			// SUB REG_INDEX, IMMEDIATE_VALUE
			// subtract VALUE of reg1 from reg2 (or IV)
			if (opcode == SUB || opcode == SUBI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == SUBI ? opr2 : REGS[opr2]) & 0xFFFF;
				int result = value1 - value2;
				// store only the lower 16 bits
				REGS[opr1] = (char) (result & 0xFFFF);
				// processor flags
				ZFL = (result & 0xFFFF) == 0;
				CFL = (result < 0); // borrow (unsigned underflow)
				
				boolean sign1   = (value1 & 0x8000) != 0;
				boolean sign2   = (value2 & 0x8000) != 0;
				boolean signRes = (result & 0x8000) != 0;
				
				OFL = (sign1 != sign2) && (signRes != sign1); //detects signed overflow
				continue;
			}
			
			// MUL REG_INDEX, REG_2_INDEX
			// MUL REG_INDEX, IMMEDIATE_VALUE
			// multiply VALUE of reg1 from reg2 (or IV)
			if (opcode == MUL || opcode == MULI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == MULI ? opr2 : REGS[opr2]) & 0xFFFF;
				int result = value1 * value2;
				REGS[opr1] = (char) (result & 0xFFFF);
				// overflow occurs if result is greater than 16 bits (more than 0xFFFF)
				OFL = (result > 0xFFFF);
				continue;
			}
			
			// DIV REG_INDEX, REG_2_INDEX
			// DIV REG_INDEX, IMMEDIATE_VALUE
			// divide register 1 by register 2, store quotient in reg 1 and
			// remainder in register DIV_REMAINDER
			// reg1 /= reg2 (int)
			// rdr = reg1 % reg2
			if (opcode == DIV || opcode == DIVI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == DIVI ? opr2 : REGS[opr2]) & 0xFFFF;
				// handle division by zero
				if (value2 == 0) {
					// no wtf
					OFL = true;
					continue;
				}
				int quotient = value1 / value2;
				REGS[opr1] 			= (char) (quotient & 0xFFFF);
				REGS[DIV_REMAINDER] = (char) ((value1 % value2) & 0xFFFF);
				continue;
			}
			
			/**** BITWISE OPERATION, ASSIGN RESULT TO THE FIRST REGISTER ****/
			// AX = AX & BX
			
			// AND REG_INDEX, REG_2_INDEX
			// AND REG_INDEX, IMMEDIATE_VALUE
			// basically REG_1 = REG_1 & REG_2 
			if (opcode == AND || opcode == ANDI) {
				REGS[opr1] &= (opcode == ANDI ? opr2 : REGS[opr2]);
				continue;
			}
			
			// OR REG_INDEX, REG_2_INDEX
			// OR REG_INDEX, IMMEDIATE_VALUE
			// basically REG_1 = REG_1 | REG_2 
			if (opcode == OR || opcode == ORI) {
				REGS[opr1] |= (opcode == ORI ? opr2 : REGS[opr2]);
				continue;
			}
			
			// XOR REG_INDEX, REG_2_INDEX
			// XOR REG_INDEX, IMMEDIATE_VALUE
			// basically REG_2 = REG_1 ^ REG_2
			if (opcode == XOR) {
				REGS[opr1] ^= (opcode == XORI ? opr2 : REGS[opr2]);
				continue;
			}
			
			// bitshift (arithmetic)
			// SHR  REG_INDEX, REG_2_INDEX
			// SHRI REG_INDEX, IMMEDIATE VALUE
			// reg1 = reg1 >> reg2 (or IV)
			if (opcode == SHR || opcode == SHRI) {
				REGS[opr1] >>= (opcode == SHRI ? opr2 : REGS[opr2]);
			}
			
			// SHL  REG_INDEX, REG_2_INDEX
			// SHLI REG_INDEX, IMMEDIATE VALUE
			// reg1 = reg1 << reg2 (or IV)
			if (opcode == SHL || opcode == SHLI) {
				REGS[opr1] <<= (opcode == SHLI ? opr2 : REGS[opr2]);
			}
			
			// NOT REG_INDEX
			// basically REG_1 = ~REG_1
			if (opcode == NOT) {
				REGS[opr1] = (char) ~REGS[opr1];
				continue;
			}
			
			/**** STACK OPERATIONS ****/
			// PUSH  REGISTER_INDEX
			// IPUSH IMMEDIATE_VALUE
			// push a value to the stack (decrementing the stack pointer)
			if (opcode == PUSH || opcode == IPUSH) {
				// example
				// 00 00 00
				//       ^^ begins here
				char value16 = (opcode == IPUSH ? opr1 : REGS[opr1]);
				// the operation below writes to it
				// 00 00 FF
				//    ^^ written the low byte and move the pointer to the left (-1)
				MEMORY[REGS[STACK_PTR]--]   = (byte) (value16 & 0xFF); // low byte
				
				// the operation below writes to it
				// 00 FF FF
				// ^^ written the HIGH byte and move the pointer to the left, ready for the next one
				MEMORY[REGS[STACK_PTR]--]   = (byte) (value16 >> 8); // high byte
				continue;
			}
			
			// POP REGISTER_INDEX
			// POP from the stack to register index (incrementing the stack pointer)
			if (opcode == POP) {
				// the minimum size of the stack (that is still pop-able) is 2 bytes, hence
				// we check that the stack pointer is at least 2 bytes away from the bottom of the stack region.
				// if the stack pointer exceeds the minimum valid address (i.e., STACK_REGION - 2), warn
				// 00 00 00 CA FE
				//       ^^ the pointer must be at least HERE to be able to pop
				if (REGS[STACK_PTR] + 2 > STACK_REGION) {
					// display a warning message if the stack pointer is too high, which could lead to an underflow condition when popping
					System.err.println("Stack underflow! instruction not fulfilled");
					continue;
				}
				// for example, we have this stack with ONE 2-bytes element
				// 00 CA FE
				// ^ current RSP is at "0"
				// this increments by 1 and get CA, increment by one again and get FE, and then assign to the register
				// after "POP", RSP is now at 2, which is the bottom of the stack
				REGS[opr1] = (char) ((MEMORY[++REGS[STACK_PTR]] << 8) | (MEMORY[++REGS[STACK_PTR]] & 0xFF));
				continue;
			}
			
			System.err.printf("[CPU | FAULT] Unknown OPCODE: %02X\n", (int) opcode);
			break;
		}
		
		System.out.println("cpu stopped or crashed, heres da dump");
		printRegisters(true);
		printMemory(true);
	}
	
	private static void printRegisters(boolean hex) {
		System.out.println("\n[CPU | DUMP] CPU Registers (RAX = 00, RBX = 01, ...)");
		System.out.print("|");
		for (int i = 0; i < REGS.length; i++) {
			System.out.print(" R" + registersName[i] + "  |");
		}
		
		System.out.println();
		System.out.print("|------+");
		System.out.print("------+".repeat(REGS.length - 2));
		System.out.print("------|");
		System.out.print("\n|");
		
		for (int i = 0; i < REGS.length; i++) {
			System.out.printf(hex ? " %04X |" : " %03d |", (int) REGS[i]);
		}
		System.out.println("\n");
	}
	
	@SuppressWarnings("resource")
	private static void printMemory(boolean hex) {
		System.out.println("First 128 bytes of the program");
		for (int i = 0; i < 128; i++) {
			if (i % 16 == 0) System.out.println();
			
			boolean is_program = i < ROM.length;
			var std = is_program ? System.err : System.out;
			
			std.printf(hex ? "%02X " : "%06d ", MEMORY[i]);
		}
		System.out.println();
		System.out.println("Last 128 bytes of the program");
		for (int i = MEMORY.length - 128; i < MEMORY.length; i++) {
			if (i % 16 == 0) System.out.println();
			
			boolean is_program = i < ROM.length;
			var std = is_program ? System.err : System.out;
			
			std.printf(hex ? "%02X " : "%06d ", MEMORY[i]);
		}
		System.out.println();
	}
	
	public static void copy_rom_to_ram() {
		for (int i = 0; i < ROM.length; ++i) {
			MEMORY[i] = (byte) (ROM[i] & 0xFF);
		}
	}
}
