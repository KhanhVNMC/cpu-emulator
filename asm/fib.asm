; jump into the main entry
jmp .entry

; calculate n-th fibbonaci
.fib
    ldi r1, 0 ; first
    ldi r2, 1 ; second
    ldi r3, 0 ; current
    ldi r0, 2 ; i = 2
    ldi r6, 10

; for (int i = 2; i <= n; i++)
.loop
    cmp  r0, r6 ; if (i > n) break;
    ifgt .exit
    addi r0, 1
    addi r7, 1
    ; actual fibonnaci calculation
    mov  r3, r1 ; r3 = r1
    add  r3, r2 ; r3 += r2 = current
    mov  r1, r2 ; first = second
    mov  r2, r3 ; second = current
    ; if (r0 == 0) break;
    jmp  .loop

.entry
    jmp .fib

.exit
    mov r0, r3
    hlt
