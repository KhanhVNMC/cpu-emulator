# FL516 CPU Documentation

The **FL516** is a custom-designed 16-bit Instruction Set Architecture (ISA) with a fixed-length instruction format of 5 bytes per instruction. It operates on a 64KB (16-bit addressable) memory space and provides a versatile set of opcodes for data manipulation, arithmetic operations, bitwise operations, stack management, and program flow control. This document provides an in-depth overview of the FL516 CPU's architecture, instruction set, and behavior.

---

## Overview

- **Instruction Set Name**: FL516 (Fixed-Length 5-byte, 16-bit)
- **Addressable Memory**: 64KB (2^16 bytes, 16-bit address space)
- **Instruction Format**: Fixed-length, 5 bytes per instruction
  - 1 byte for the opcode
  - 4 bytes for operands (typically two 16-bit values)
- **Registers**: 10 general-purpose 16-bit registers, with two reserved for special purposes
- **Memory Model**: Big-endian (most significant byte stored at the lowest address)
- **Stack**: Grows downward from the top of memory (address `0xFFFF`)

The FL516 CPU is designed for simplicity and efficiency, with a focus on fixed-length instructions to streamline instruction decoding and execution. It supports a variety of operations suitable for general-purpose computing, including arithmetic, logical operations, stack-based data handling, and conditional branching.

---

## CPU Components

### Registers
The FL516 CPU features 10 16-bit registers, labeled as follows:
- **General-purpose registers**: `R0` to `R7` (8 registers)
- **Special-purpose registers**:
  - `R8`: Division remainder (`MD`)
  - `R9`: Stack pointer (`SP`)

Each register is 16 bits wide, capable of storing values from `0x0000` to `0xFFFF`.

### Memory
- **Size**: 64KB (65,536 bytes), addressed from `0x0000` to `0xFFFF`.
- **Access**: Byte-addressable, with 16-bit values stored across two consecutive bytes in big-endian format.
- **Stack Region**: Located at the top of memory, starting at `0xFFFF` and growing downward.

### Flags
The CPU maintains three key flags for arithmetic and comparison operations:
- **ZFL (Zero Flag)**: Set to `TRUE` when a result is zero.
- **CFL (Carry Flag)**: Indicates carry (unsigned overflow) or borrow (unsigned underflow).
- **OFL (Overflow Flag)**: Indicates signed overflow during arithmetic operations.

### Program Counter
- **PC (Program Counter)**: A 16-bit register tracking the address of the next instruction to execute. Instructions are 5 bytes long, so the PC typically increments by 5 unless modified by a jump instruction.

---

## Instruction Format

Each instruction in the FL516 ISA is exactly 5 bytes long:
- **Byte 0**: Opcode (1 byte, `0x00` to `0xFF`)
- **Bytes 1-2**: First 16-bit operand (big-endian)
- **Bytes 3-4**: Second 16-bit operand (big-endian)

The operands can represent register indices, immediate values, or memory addresses, depending on the opcode.

---

## Instruction Set (Opcodes)

The FL516 instruction set is divided into several categories: data controls, arithmetic/bitwise operations, stack operations, and flow controls. Below is a detailed description of each opcode.

### Data Controls
These instructions manage data movement between registers and memory.

| Opcode | Hex  | Description                                   | Operands            | Behavior                                      |
|--------|------|-----------------------------------------------|---------------------|-----------------------------------------------|
| NOP    | `0x00` | No operation                                 | None                | Does nothing, advances PC by 5.              |
| MOV    | `0x01` | Move register to register                    | Reg1, Reg2          | Copies value from Reg2 to Reg1.              |
| LDI    | `0x02` | Load immediate                               | Reg, Immediate      | Loads a 16-bit immediate value into Reg.     |
| LDM    | `0x03` | Load from memory                             | Reg, Address        | Loads a 16-bit value from Address into Reg.  |
| STO    | `0x04` | Store to memory (register)                   | Address, Reg        | Stores Reg’s 16-bit value to Address (2 bytes). |
| STOI   | `0x05` | Store to memory (immediate)                  | Address, Immediate  | Stores a 16-bit immediate to Address (2 bytes). |
| BLNK   | `0x06` | Blank instruction                            | None                | Reserved for future use; acts as NOP.        |

- **Notes**:
  - `LDM` and `STO`/`STOI` operate on 16-bit values across two consecutive memory bytes (big-endian).
  - Register indices are 0 to 9 (for `R0` to `R9`).

### Arithmetic and Bitwise Operations
These instructions perform computations and logical operations, storing results in the first register operand.

| Opcode | Hex  | Description                                   | Operands            | Behavior                                      |
|--------|------|-----------------------------------------------|---------------------|-----------------------------------------------|
| ADD    | `0x07` | Add registers                                | Reg1, Reg2          | Reg1 = Reg1 + Reg2; updates ZFL, CFL, OFL.   |
| ADI    | `0x08` | Add immediate                                | Reg, Immediate      | Reg = Reg + Immediate; updates flags.        |
| SUB    | `0x09` | Subtract registers                           | Reg1, Reg2          | Reg1 = Reg1 - Reg2; updates ZFL, CFL, OFL.   |
| SUBI   | `0x0A` | Subtract immediate                           | Reg, Immediate      | Reg = Reg - Immediate; updates flags.        |
| MUL    | `0x0B` | Multiply registers                           | Reg1, Reg2          | Reg1 = Reg1 * Reg2; updates OFL if > 16 bits.|
| MULI   | `0x0C` | Multiply immediate                           | Reg, Immediate      | Reg = Reg * Immediate; updates OFL.          |
| DIV    | `0x0D` | Divide registers                             | Reg1, Reg2          | Reg1 = Reg1 / Reg2, R8 = remainder; OFL if div by 0. |
| DIVI   | `0x0E` | Divide immediate                            | Reg, Immediate      | Reg = Reg / Immediate, R8 = remainder.       |
| AND    | `0x0F` | Bitwise AND registers                        | Reg1, Reg2          | Reg1 = Reg1 & Reg2.                          |
| ANDI   | `0x10` | Bitwise AND immediate                        | Reg, Immediate      | Reg = Reg & Immediate.                       |
| OR     | `0x11` | Bitwise OR registers                         | Reg1, Reg2          | Reg1 = Reg1 \| Reg2.                         |
| ORI    | `0x12` | Bitwise OR immediate                         | Reg, Immediate      | Reg = Reg \| Immediate.                      |
| XOR    | `0x13` | Bitwise XOR registers                        | Reg1, Reg2          | Reg1 = Reg1 ^ Reg2.                          |
| XORI   | `0x14` | Bitwise XOR immediate                        | Reg, Immediate      | Reg = Reg ^ Immediate.                       |
| SHR    | `0x15` | Shift right registers                        | Reg1, Reg2          | Reg1 = Reg1 >> Reg2 (logical shift).         |
| SHRI   | `0x16` | Shift right immediate                        | Reg, Immediate      | Reg = Reg >> Immediate (logical shift).      |
| SHL    | `0x17` | Shift left registers                         | Reg1, Reg2          | Reg1 = Reg1 << Reg2.                         |
| SHLI   | `0x18` | Shift left immediate                         | Reg, Immediate      | Reg = Reg << Immediate.                      |
| NOT    | `0x19` | Bitwise NOT                                  | Reg, None           | Reg = ~Reg.                                  |

- **Notes**:
  - Arithmetic operations update flags:
    - `ZFL`: Set if result is zero.
    - `CFL`: Set on unsigned overflow (carry) or underflow (borrow).
    - `OFL`: Set on signed overflow or division by zero.
  - Bitwise shifts are logical (unsigned); sign bits are not preserved.

### Stack Operations
The stack grows downward from `0xFFFF`, with the stack pointer (`R9`) decrementing on push and incrementing on pop.

| Opcode | Hex  | Description                                   | Operands            | Behavior                                      |
|--------|------|-----------------------------------------------|---------------------|-----------------------------------------------|
| PUSH   | `0x1A` | Push register to stack                       | Reg, None           | Stores Reg to stack, decrements SP by 2.     |
| IPUSH  | `0x1B` | Push immediate to stack                      | Immediate, None     | Stores Immediate to stack, decrements SP by 2. |
| POP    | `0x1C` | Pop from stack to register                   | Reg, None           | Loads 16-bit value from stack to Reg, increments SP by 2. |

- **Notes**:
  - Stack operations store/load 16-bit values across two bytes.
  - Stack underflow (popping an empty stack) is undefined behavior and may trigger a warning.

### Flow Controls
These instructions manage program execution flow.

| Opcode | Hex  | Description                                   | Operands            | Behavior                                      |
|--------|------|-----------------------------------------------|---------------------|-----------------------------------------------|
| HLT    | `0xF0` | Halt CPU                                     | None                | Stops execution.                             |
| JMP    | `0xF1` | Jump to address                              | Address, None       | Sets PC to Address.                          |
| CMP    | `0xF2` | Compare registers                            | Reg1, Reg2          | Sets ZFL if Reg1 = Reg2, CFL if Reg1 < Reg2. |
| IFEQ   | `0xF3` | Jump if equal                                | Address, None       | Jumps to Address if ZFL is TRUE.             |
| IFLT   | `0xF4` | Jump if less than                            | Address, None       | Jumps to Address if CFL is TRUE.             |
| IFGT   | `0xF5` | Jump if greater than                         | Address, None       | Jumps to Address if CFL is FALSE.            |
| DBGP   | `0xFF` | Debug pause                                  | None                | Pauses execution for debugging.              |

- **Notes**:
  - `CMP` performs `Reg1 - Reg2` to set flags:
    - `ZFL`: Result is zero (Reg1 = Reg2).
    - `CFL`: Result is negative (Reg1 < Reg2, unsigned).
  - Jump addresses should be multiples of 5 (instruction size); misaligned jumps may cause undefined behavior.

---

## CPU Behavior

### Instruction Fetch and Execution
1. The CPU fetches a 5-byte instruction starting at the address in the Program Counter (PC).
2. The opcode (first byte) determines the operation.
3. The two 16-bit operands are decoded based on the opcode (e.g., register indices, immediate values, or memory addresses).
4. The operation is executed, updating registers, memory, or flags as needed.
5. The PC is incremented by 5, unless modified by a jump or halt instruction.

### Stack Management
- The stack starts at `0xFFFF` and grows downward.
- Each push operation writes a 16-bit value (2 bytes) and decrements the stack pointer (`R9`) by 2.
- Each pop operation reads a 16-bit value and increments the stack pointer by 2.
- Stack underflow or overflow is not explicitly trapped; it’s the programmer’s responsibility to manage the stack bounds.

### Flag Updates
- **Zero Flag (ZFL)**: Set when a result is `0x0000`.
- **Carry Flag (CFL)**:
  - Set on unsigned overflow (e.g., addition exceeding `0xFFFF`).
  - Set on unsigned underflow (e.g., subtraction resulting in a borrow).
  - Used by `CMP` to indicate if the first operand is less than the second (unsigned).
- **Overflow Flag (OFL)**:
  - Set on signed overflow in arithmetic operations.
  - Set on division by zero.

### Endianness
- The FL516 uses a **big-endian** memory model:
  - For a 16-bit value `0xCAFE` stored at address `0x1000`:
    - `MEMORY[0x1000] = 0xCA` (high byte)
    - `MEMORY[0x1001] = 0xFE` (low byte)

---

## Example Program
Here’s a conceptual example of an FL516 program (in hex):
```
IPUSH CAFE 0000  ; Push 0xCAFE onto the stack
IPUSH BABE 0000  ; Push 0xBABE onto the stack
POP   0000 0000  ; Pop top value into R0
POP   0001 0000  ; Pop next value into R1
HLT   0000 0000  ; Halt the CPU
```
- **Execution**:
  1. Pushes `0xCAFE` and `0xBABE` onto the stack.
  2. Pops `0xBABE` into `R0` and `0xCAFE` into `R1`.
  3. Halts execution.

---

This documentation provides a comprehensive guide to the FL516 CPU and its instruction set. For further details or implementation specifics, consult the architecture’s reference materials or contact the designers (there's none)