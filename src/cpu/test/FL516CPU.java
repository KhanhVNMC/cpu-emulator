package cpu.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EmptyStackException;

public class FL516CPU {
	/** opcode labels **/
	/* data controls */
	public static final int MOV  = 0x01; // move from registers to registers
	public static final int LDI  = 0x02; // load immediate (load a value to reg)
	public static final int LMH  = 0x03; // load a 16-bit (2 bytes; HALF-WORD) value from the memory address stored in a register to a register
	public static final int LMB  = 0x04; // load a 8-bit (1 byte; HALF-WORD) value from the memory address stored in a register to a register
	public static final int SMH  = 0x05; // stores a 16-bit (WORD) value from a register into the memory address stored in another register.
	public static final int SMB  = 0x06; // stores a 8-bit value (HALFWORD) from a register into the memory address stored in another register.
	public static final int VMS  = 0x07; // switch addressing mode to VRAM
	public static final int WMS  = 0x08; // switch addressing mode to RAM

	/* arithmetic & bitwise controls*/
	// ADDITION
	public static final int ADD  = 0x10;
	public static final int ADDI = 0x11; // ADD IMMEDIATE
	// SUBTRACTION
	public static final int SUB  = 0x12;
	public static final int SUBI = 0x13; // SUBTRACT IMMEDIATE
	// MULTIPLICATION
	public static final int MUL  = 0x14;
	public static final int MULI = 0x15; // MULTIPLY IMMEDIATE
	// DIVISON
	public static final int DIV  = 0x16;
	public static final int DIVI = 0x17; // DIVIDE IMMEDIATE
	// MODULO
	public static final int MOD  = 0x18;
	public static final int MODI = 0x19; // DIVIDE IMMEDIATE
	// BITWISE AND
	public static final int AND  = 0x1A;
	public static final int ANDI = 0x1B; // AND IMMEDIATE
	// BITWISE OR
	public static final int OR   = 0x1C;
	public static final int ORI  = 0x1D; // OR IMMEDIATE
	// BITWISE XOR
	public static final int XOR  = 0x1E;
	public static final int XORI = 0x1F; // XOR IMMEDIATE
	// BITWISE SHIFT RIGHT (UNSIGNED)
	public static final int SHR  = 0x20;
	public static final int SHRI = 0x21; // SHIFT RIGHT IMMEDIATE
	// BITWISE SHIFT LEFT (UNSIGNED)
	public static final int SHL  = 0x22;
	public static final int SHLI = 0x23; // SHIFT LEFT IMMEDIATE
	// BITWISE NOT (flip flop)
	public static final int NOT  = 0x24;
	
	/* stack operations */
	public static final int PUSH  = 0x3A;
	public static final int IPUSH = 0x3B; // PUSH immediate
	public static final int POP   = 0x3C;
	
	/* flow controls */
	public static final int HLT  = 0xF0; // HALT the CPU
	public static final int JMP  = 0xF1; // JUMP to an address
	public static final int CMP  = 0xF2; // COMPARE (store result in CFL--compare flag and ZFL--zero flag)
	public static final int JEQ  = 0xF3; // IF EQUAL then JUMP
	public static final int JNE  = 0xF4; // IF NOT EQUAL then JUMP
	public static final int JLT  = 0xF5; // IF LESS THAN then JUMP (CFL = TRUE)
	public static final int JGT  = 0xF6; // IF GREATER THAN then JUMP (CFL = FALSE)
	public static final int JLE  = 0xF7; // IF LESS THAN then JUMP (CFL = TRUE)
	public static final int JGE  = 0xF8; // IF GREATER THAN then JUMP (CFL = FALSE)
	public static final int CALL = 0xF9; // CALL a function (and push return address to the stack)
	public static final int RET  = 0xFA; // RET(urn) by popping the stack and execute JMP
	
	// reserved
	public static final int NOP  = 0x00; // NONE instruction
	public static final int DBGP = 0xFF; // debug cpu pause
	
	/* CPU Internal parts (registers & memory & internal flags) */
	// registers
	static char[]   REGS = new char[10]; public // char = 2bytes = 16-bit registers
	// special registers (convention)
	static int      STACK_PTR_LOC     = 9; // location in the registers
	
	// 16-bit addressable space (2 banks -- realistic)
	// bank 0 is used for the entire CPU side of things
	// bank 1 is used as video memory (VRAM)
	// only LMH, LMB, SMH, SMB are affected by this
	static byte[][] MEMORY       = new byte[2][0xFFFF + 1]; // basically 65536 addressable bytes
	static int      STACK_REGION = MEMORY[0].length - 1; // or 0xFFFF, the stack grows downward
	
	// special CPU flags and program counter
	/** True if the last CMP operation is zero */
	static boolean  ZFL = false;   // zero flag (for CMP)
	/** True if the last CMP operation is negative (negative = smaller, positive = greater) */
	static boolean  CFL = false;   // carry flag (for unsigned)
	/** True if the last artithmetic operation overflows (wraps around unsigned 16bit range) */
	static boolean  OFL = false;   // overflow flag
	/** The program counter (instruction pointer), point at what byte (ISA) to execute (multiples of 5) */
	static int      PROGRAM_COUNTER = 0;
	
	// declarations
	static final int RAM = 0;
	static final int VRAM = 1;
	// there're two "64KB" ram "chips", one for normal CPU memory and one for the
	// "VPS" (video processing subroutine; which is just the CPU) memory (VRAM)
	// 0 for working ram, 1 for vram
	static int 		 MEMORY_MODE = RAM; // default memory mode is WORKING RAM, change to 1 for VRAM
	
	// for the java emulator
	private static String[] registersName = {
		"0 ", "1 ", "2 ", "3 ", "4 ", "5 ", "6 ", "7 ", "8 ", // 9 all purpose registers
		"SP", // stack pointer
	};
	
	// if the CPU is temporaily stopped from executing tasks
	static boolean PAUSED = false;
	
	/** push an unsigned 16bit integer to the stack (governed by the RSP) */
	static void stackPush(char value16) {
		// example
		// 00 00 00
		//       ^^ begins here
		// the operation below writes to it
		// 00 00 FF
		//    ^^ written the low byte and move the pointer to the left (-1)
		MEMORY[RAM][REGS[STACK_PTR_LOC]--]   = (byte) (value16 & 0xFF); // low byte
		
		// the operation below writes to it
		// 00 FF FF
		// ^^ written the HIGH byte and move the pointer to the left, ready for the next one
		MEMORY[RAM][REGS[STACK_PTR_LOC]--]   = (byte) (value16 >> 8); // high byte
	}
	
	/** pop a value from a stack (moves RSP up) and return it */
	static char stackPop() throws EmptyStackException {
		// the minimum size of the stack (that is still pop-able) is 2 bytes, hence
		// we check that the stack pointer is at least 2 bytes away from the bottom of the stack region.
		// if the stack pointer exceeds the minimum valid address (i.e., STACK_REGION - 2), warn
		// 00 00 00 CA FE
		//       ^^ the pointer must be at least HERE to be able to pop
		if (REGS[STACK_PTR_LOC] > STACK_REGION - Character.BYTES) {
			// display a warning message if the stack pointer is too high, which could lead to an underflow condition when popping
			System.err.println("Stack underflow! instruction not fulfilled");
			throw new EmptyStackException();
		}
		// for example, we have this stack with ONE 2-bytes element
		// 00 CA FE
		// ^ current RSP is at "0"
		// this increments by 1 and get CA, increment by one again and get FE, and then assign to the register
		// after "POP", RSP is now at 2, which is the bottom of the stack
		return (char) ((MEMORY[RAM][++REGS[STACK_PTR_LOC]] << 8) | (MEMORY[RAM][++REGS[STACK_PTR_LOC]] & 0xFF));
	}
	
	// initialize the stack register and start the processor
	public static void startProcessor() throws InterruptedException {
		// INIT stack to 65536
		REGS[STACK_PTR_LOC] = (char) STACK_REGION;
		
		// start the CPU
		cpu_loop: while (true) {
			Thread.sleep(1);
			
			// if the CPU is paused, then dont do anything
			if (PAUSED) continue;
			
			// fetch instructions (5 bytes)
			int opcode  = MEMORY[RAM][PROGRAM_COUNTER++] & 0xFF; // prevent sign extension
			// merge two consecutive bytes into one 16-bit operand
			// example: CA, FE
			// 1) CA << 8 => CA00  (CA left by 8 bits, pad 8 bits to the right)
			// 2) CA00 | (FE & 0xFF) => CAFE (OR merges) [0xFF to prevent sign extension, only get the 2 bytes)
			char opr1   = (char) ((MEMORY[RAM][PROGRAM_COUNTER++] << 8) | (MEMORY[RAM][PROGRAM_COUNTER++] & 0xFF));
			char opr2   = (char) ((MEMORY[RAM][PROGRAM_COUNTER++] << 8) | (MEMORY[RAM][PROGRAM_COUNTER++] & 0xFF));
			
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
				PAUSED = true;
				continue;
			}
			
			// NOP 00 00
			// empty instruction, use to fill up the processor
			if (opcode == NOP) {
				continue;
			}
			
			// switch memory addressing mode
			if (opcode == VMS || opcode == WMS) {
				// if VMS (Video memory switch), switch to VRAM addressing mode,
				// to switch back, use WMS (Working memory switch)
				MEMORY_MODE = opcode == VMS ? 1 : 0;
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
			// jump unconditionally to an address in the program instructions space
			if (opcode == JMP) {
				if (opr1 % 5 != 0) {
					System.err.println("[CPU | JMP WARN] Jump location is not a multiple of 5. Undefined behaviour may happen!");
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
			
			// JEQ [Program counter] ; IF EQUAL
			// jump to an address in the program instructions space IF
			// and only if the Z_FLAG (ZERO) is set (result of cmp equals)
			if (opcode == JEQ) {
				// if there's a ZERO
				if (ZFL) PROGRAM_COUNTER = opr1; // jump to an address
				continue;
			}
			
			// JNE [Program counter] ; IF NOT EQUAL
			// jump to an address in the program instructions space IF
			// and only if the Z_FLAG (ZERO) is CLEAR (result of cmp equals)
			if (opcode == JNE) {
				// if there's NOT a ZERO
				if (!ZFL) PROGRAM_COUNTER = opr1; // jump to an address
				continue;
			}
			
			// JLT [Program counter] ; IF LESS THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is set (result of cmp is: A less than B)
			if (opcode == JLT) {
				if (!ZFL && CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// JLE [Program counter] ; IF LESS OR EQUAL
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is set (result of cmp is: A less than B)
			// OR A equals B
			if (opcode == JLE) {
				if (ZFL || CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// JGT [Program counter] ; IF GREATER THAN
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is CLEAR (result of cmp is: A greater than B)
			if (opcode == JGT) {
				if (!ZFL && !CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// JGE [Program counter] ; IF GREATER OR EQUAL
			// jump to an address in the program instructions space IF
			// and only if the C_FLAG (CARRY) is CLEAR (result of cmp is: A greater than B)
			// OR A equals B
			if (opcode == JGE) {
				if (ZFL || !CFL) PROGRAM_COUNTER = opr1;
				continue;
			}
			
			/** CALL AND RET (STACK BASED) **/
			// CALL [Program counter / address]
			// push the current instruction pointer to the stack and
			// jump unconditionally to an address in the program instructions space
			if (opcode == CALL) {
				// push the current call address to the stack
				stackPush((char)(PROGRAM_COUNTER % 65536));
				// jump to the determined address
				if (opr1 % 5 != 0) {
					System.err.println("[CPU | CALL WARN] Function location is not a multiple of 5. Undefined behaviour may happen!");
				}
				PROGRAM_COUNTER = opr1;
				continue;
			}
			
			// RET (Return from Function)
			// pops the return address from the stack and jumps back to it
			// this effectively resumes execution at the point after a CALL
			if (opcode == RET) {
				try {
					// resumes execution
					PROGRAM_COUNTER = stackPop();
				} catch (EmptyStackException e) {}
				continue;
			}
			
			/** CPU REGISTERS DATA MANIPULATION **/
			// MOV REG_A, REG_B
			// PROGRAM COUNTER CAN BE ACCESSED VIA: MOV REG_A 0xFF
			// REGA = REGB
			if (opcode == MOV) {
				// move stuff from register B to A
				REGS[opr1] = opr2 == 0xFF ? (char)(PROGRAM_COUNTER % 65536) : REGS[opr2];
				continue;
			}
			
			// LDI REG_INDEX, IMMEDIATE VALUE
			// load immediate value to a register
			// REGISTER = value
			if (opcode == LDI) {
				// assign immediate value (op1) to register[op2]
				REGS[opr1] = opr2;
				continue;
			}
			
			/** CPU MEMORY (MMU) DATA MANIPULATION **/
			// LMH REG_A, REG_B
			// Loads a 16-bit value from the memory address stored in a REG_B register into REG_A
			// This was a mistake
			if (opcode == LMH) {
				// MEMORY[REG[opr2]] = MSB, MEMORY[REG[opr2] + 1] = LSB (big-endian model)
				// REG[opr1] = (MEMORY[REG[opr2]] << 8) | MEMORY[REG[opr2]+1]
				REGS[opr1] = (char) (((MEMORY[MEMORY_MODE][REGS[opr2]] & 0xFF) << 8) | (MEMORY[MEMORY_MODE][REGS[opr2] + 1] & 0xFF));
				continue;
			}
			
			// LMB REG_A, REG_B
			// Loads a 8-bit value from the memory address stored in a REG_B register into REG_A
			// This was a mistake
			if (opcode == LMB) {
				// REG[opr1] = (MEMORY[REG[opr2]] & 0xFF)
				REGS[opr1] = (char) (MEMORY[MEMORY_MODE][REGS[opr2]] & 0xFF);
				continue;
			}
			
			// SMH REG_A, REG_B
			// Stores a 16-bit value from register B into the memory address stored in register A
			// basically MEMORY[register value A] (2x) = MEMORY[register value B)
			if (opcode == SMH) {
				char value = REGS[opr2];
				MEMORY[MEMORY_MODE][REGS[opr1]]     = (byte) ((value >> 8) & 0xFF); // high byte
				MEMORY[MEMORY_MODE][REGS[opr1] + 1] = (byte) (value & 0xFF); // low byte
				continue;
			}

			// SMH REG_A, REG_B
			// Stores a 8-bit value from register B into the memory address stored in register A
			// basically MEMORY[register value A] = MEMORY[register value B)
			if (opcode == SMB) {
				MEMORY[MEMORY_MODE][REGS[opr1]] = (byte) (REGS[opr2] & 0xFF);
				continue;
			}
			
			/**** ARITHMETIC OPERATIONS ****/
			// ADD AX, BX is ADD AX TO BX AND PUT TO AX
			
			// ADD REG_INDEX, REG_2_INDEX
			// ADDI REG_INDEX, IMMEDIATE_VALUE
			// add value from reg 1 to reg2 (or IV)
			if (opcode == ADD || opcode == ADDI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == ADDI ? opr2 : REGS[opr2]) & 0xFFFF;
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
			
			// DIV  REG_INDEX, REG_2_INDEX
			// DIVI REG_INDEX, IMMEDIATE_VALUE
			// divide register 1 by register 2, store quotient in reg 1 (no remainder, use MOD isntead)
			// reg1 /= reg2 (int)
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
				REGS[opr1] 	 = (char) (quotient & 0xFFFF);
				continue;
			}
			
			// MOD  REG_INDEX, REG_2_INDEX
			// MODI REG_INDEX, IMMEDIATE_VALUE
			// modulate register 1 by register 2, store in reg 1
			// reg1 %= reg2 (int)
			if (opcode == MOD || opcode == MODI) {
				int value1 = REGS[opr1] & 0xFFFF;
				int value2 = (opcode == MODI ? opr2 : REGS[opr2]) & 0xFFFF;
				// handle division by zero
				if (value2 == 0) {
					// no wtf
					OFL = true;
					continue;
				}
				int remainder = value1 % value2;
				REGS[opr1] 	  = (char) (remainder & 0xFFFF);
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
				continue;
			}
			
			// SHL  REG_INDEX, REG_2_INDEX
			// SHLI REG_INDEX, IMMEDIATE VALUE
			// reg1 = reg1 << reg2 (or IV)
			if (opcode == SHL || opcode == SHLI) {
				REGS[opr1] <<= (opcode == SHLI ? opr2 : REGS[opr2]);
				continue;
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
				char value16 = (opcode == IPUSH ? opr1 : REGS[opr1]);
				// push a value from either a register or immediate value to the stack
				stackPush(value16);
				continue;
			}
			
			// POP REGISTER_INDEX
			// POP from the stack to register index (incrementing the stack pointer)
			if (opcode == POP) {
				try {
					REGS[opr1] = stackPop(); // pop out of the stack
				} catch (EmptyStackException ignored) {}
				continue;
			}
			
			System.err.printf("[CPU | FAULT] Unknown OPCODE: %02X\n", (int) opcode);
			break;
		}
		
		System.out.println("cpu stopped or crashed, heres da dump");
		printRegisters(false);
		printMemory(true);
	}
	
	// test
	
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
		System.out.println("First 512 bytes of the program (CPU)");
		for (int i = 0; i < 512; i++) {
			if (i % 16 == 0) System.out.println();
			
			boolean is_program = i < ROM.length;
			var std = is_program ? System.err : System.out;
			
			std.printf(hex ? "%02X " : "%06d ", MEMORY[RAM][i]);
		}
		System.out.println();
		System.out.println("\nFirst 512 bytes of the VRAM (VPS)");
		for (int i = 0; i < 512; i++) {
			if (i % 16 == 0) System.out.println();
			System.out.printf(hex ? "%02X " : "%06d ", MEMORY[VRAM][i]);
		}
		System.out.println();
	}
	
	public static void copy_rom_to_ram() {
		for (int i = 0; i < ROM.length; ++i) {
			MEMORY[RAM][i] = (byte) (ROM[i] & 0xFF);
		}
	}
	
	public static byte[] ROM;
	public static void main(String... args) throws InterruptedException {
        if (args.length < 1) {
            System.err.println("Usage: java fl516emu <program.o>"); 
            System.exit(1);
        }
        String inputFile = args[0];
        if (!inputFile.endsWith(".o")) {
        	System.err.println("[FL516 Emulator] Given file is not a valid FL516 binary file!");
            System.exit(1);
        }
        try {
            ROM = Files.readAllBytes(Paths.get(inputFile));
            copy_rom_to_ram();
            startProcessor();
        } catch (IOException e) {
            System.err.println("[FL516 Emulator] Error reading binary file: " + e.getMessage());
            System.exit(1);
        }
    }
}
