.END
    HLT                   ; Halt execution

.FAILURE
    LDI   R3, 0x0          ; load 0x0000
    STO   0xCAFE, R3       ; store 0x0000 at 0xcafe
    JMP   .END            ; Jump to program end

.SUCCESS
    LDI   R3, 0xFFFF      ; Load 0xFFFF
    STO   0xCAFE, R3       ; Store 0xFFFF at .FLAG

.data
    LDI   R0, 0x14      ; load 0x0014 (20) into R0
    LDI   R1, 0xA      ; load 0x000A (10) into R1

    ADD   R0, R1          ; R0 = R0 + R1 (20 + 10 = 30)
    STO   0x123, R0     

    LDI   R2, 0x001E      
    CMP   R0, R2        

    IFEQ  .SUCCESS     
