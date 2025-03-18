@data ;hi
    string   .str    "Hello World" ; hello
    strlen   .hword  11  ; length
    array    .pspace 128 ; array

@text
; jump into the main entry
vms
ldi  r1, 1
ldi  r2, 0b101010
call .arr_set
hlt ; end program

; r2 = color data (11_11_11_00)
; r1 = index
.arr_set:
    muli    r1, 6
    mov     r3, r1 ; offset
    mov     r4, r1 ; byte_index
    modi    r3, 8  ; 6*i % 8
    divi    r4, 8  ; 6*i / 8
    ; r3 = offset ; r4 = byte_index
    ; r5 = the current color data
    mov     r5, r2 ; r5 = r2
    shr     r5, r3 ; r5 >> byte_index
    hlt;
    lmb     r6, r4 ; r6 = *r4
    or      r6, r5 ; merge r6 with the calculated shit??
    smb     r4, r6
    ret

hlt
.testconst:
    ldi     R1, #string ; load the pointer of string
    lmb     R2, R1 ; R2 should be 72 ('H') in ascii
    ldi     R1, #strlen ; load the pointer of the number 11 in heap
    lmh     R3, R1 ; R3 should be 11

    ; store array 0th
    ldi     R1, 0
    ldi     R2, 0xAB
    call    .heaparray_set ; same thing as array[0] = 0xAB;

    ; store array 1st
    ldi     R1, 1
    ldi     R2, 0xCD
    call    .heaparray_set ; same thing as array[1] = 0xCD;

    ret

.heaparray_set:
    ldi     R0, #array
    add     R0, R1 ; R1 = offset aka array[R1]
    smb     R0, R2 ; array[R1] = R2;
    ret

hlt ; dont let it run past this if you dont want it to
.test1: ; this cool
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
    ldi     R1, ':'     ; first number
    call    .grow_array
    ldi     R1, 0xCAFE
    call    .grow_array
    ldi     R1, 0b1010
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