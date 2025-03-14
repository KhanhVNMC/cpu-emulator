; jump into the main entry
jmp .entry

; for (int i = 0; i < 100; i++)
.entry
    ldi     r0, 0
    ldi     r1, 100
    jmp     .loop

.loop
    cmp     r0, r1 ; if (r0 >= r1) break;
    ifge    .exit
    ; code executes here
    
    ; add one?
    addi    r0, 1
    jmp     .loop

.exit
    hlt

