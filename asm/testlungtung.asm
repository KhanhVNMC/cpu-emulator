@data
    ;normal_space  .space 128
    real_number      .hword  69
    rn2              .char 'A'
    real_array       .space 128, 420
@text
call .entry
hlt

.add_r1_r2:
    add     r1, r2 ; r1 += r2
    ldi     r3, 10 ; r3 = 10
    cmp     r1, r3 ; if (r1 >= r2)
    jge     .greater_than
    ret     

.greater_than:
    ldi     r0, #real_array ; load the head ptr of it
    ldi     r1, 123 ; reg1 = 123
    smb     r0, r1  ; real_array[index] = reg1
    addi    r0, 1   ; index++
    ldi     r1, 456 ; reg1 = 456
    smb     r0, r1  ; real_array[index] = reg1
    hlt

; entry point
.entry:
    ldi     r0, #real_number
    lmh     r0, r0
    push    r0      ; push the number 69 into stack
    ldi     r0, #rn2
    lmb     r0, r0  
    push    r0      ; push the char 'A' (0x61) into stack
    pop     r1      ; r1 now holds 'A'
    pop     r2      ; r2 now holds 69
    ; ret
    call    .add_r1_r2
    ret