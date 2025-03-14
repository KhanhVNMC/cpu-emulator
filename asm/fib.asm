; jump into the main entry
jmp .entry

; calculate n-th fibbonaci
.fib
    ldi r1, 0 ; first
    ldi r2, 1 ; second
    ldi r3, 0 ; current

.loop
    subi r0, 1
    ; actual fibonnaci calculation
    mov  r3, r1 ; r3 = r1
    add  r3, r2 ; r3 += r2 = current
    mov  r1, r2 ; first = second
    mov  r2, r3 ; second = current
    ; if (r0 == 0) break;
    cmp  r0, r5
    ifeq .exit
    jmp  .loop

.entry
    ldi r0, 7 ; 10th fib
    jmp .fib

.exit
    mov r0, r3
    hlt
