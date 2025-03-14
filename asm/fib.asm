; jump into the main entry
jmp .entry

.entry
    ipush   10 ; 10th fib
    jmp     .fib

; fib subroutine
.fib
    ldi     r0, 2 ; I = 2 
    pop     r1    ; R0 = N
    ; prerequisite
    ldi     r2, 1
    cmp     r1, r2 ; r1 <= r2 (aka r1 <= 1) ; r1 - r2 = 0 - 1 = -1
    jle     .premature_exit
    ; init vars
    ldi     r2, 0 ; first (seed)
    ldi     r3, 1 ; second (seed)
    ldi     r4, 0 ; current
    ; calc
    jmp     .fib_loop

; for (I = 2; I <= N; I++)
.fib_loop
    cmp     r0, r1  ; if R0 > R1 (I > N) then exit
    jgt     .exit
    ; calculate
    mov     r4, r2  ; set r4 = r2 or current = first
    add     r4, r3  ; add current = first + second (as you can see, current is now first, last IS)
    mov     r2, r3  ; first = second
    mov     r3, r4  ; second = current
    ; i++
    addi    r0, 1
    jmp     .fib_loop

; edge case (<= 1)
.premature_exit
    mov     r4, r1 ; copy the input N to the result register
    jmp     .exit  ; normal exit sequence

; stop program
.exit
    mov     r0, r4 ; copy result to R0
    ; clear all used registers
    ldi     r1, 0
    ldi     r2, 0
    ldi     r3, 0
    ldi     r4, 0
    ; halt
    hlt     


