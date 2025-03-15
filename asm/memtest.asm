; jump into the main entry
call .test3
hlt ; end program

.test1:
    ldi     R0, 100     ; load address 100 to R0 (memory)
    ldi     R1, 0xCAFE  ; load value CAFE into R1
    smh     R0, R1      ; store 0xCAFE (16-bit) into R0 (100)
    lmh     R2, R0      ; load half word from address in R0 to R2, basically R2 = *((uint16*) R0)
    lmb     R3, R0      ; load a byte from address in R0 to R3, basically R3 = *((uint8*) R0)
    ret

hlt ; dont let it run past this if you dont want it to
.test2:
    ldi     R0, 0x64    ; load address 100 (reg 0)
    ldi     R1, 0xCA    ; load CA to reg 1
    smb     R0, R1      ; *((uint8_t*) r0) = r1;
    lmb     R2, R0      ; r2 = *((uint8_t*) r0)
    ret

hlt ; same thing

; int16[] arr = {10, 20, 30, 40, 50};
.test3:
    ldi     R0, 255     ; first address (arr[0])
    mov     R2, R0      ; store address for later use
    ldi     R1, 'a'     ; first number
    call    .grow_array
    ldi     R1, 'b'
    call    .grow_array
    ldi     R1, 30
    call    .grow_array
    ldi     R1, 40
    call    .grow_array
    ldi     R1, 50
    call    .grow_array

    ; *ptr = 100
    ldi     R1, 100
    smh     R0, R1

    ret

.grow_array:
    smh     R0, R1
    addi    R0, 2   ; sizeof(R1) == 2
    ret