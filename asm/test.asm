jmp .entry

; enter the program formally
.entry
    ldi     r0, 69
    ldi     r3, 0
    push    r0     ; the initial 69

; do a while (r0 != 0) { r0--; }
.loop
    subi    r0, 1
    push    r0      ; push the current reg0 value to the stack
    addi    r1, 1
    cmp     r0, r3
    jeq     .exit
    jmp     .loop

.exit
    pop     r5 ; 0
    pop     r5 ; 1
    pop     r5 ; 2
    pop     r5 ; 3
    pop     r5 ; 4
    ; expected value when halt is 4 (on RFX aka R5)
    hlt

