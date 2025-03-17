@data
    screen_width      .uint8   320 ; 320 x 240 (240p 4 : 3)
    screen_height     .uint8   240
    vram_working_ptr  .uint16  0
@text
; start program
call    .test_color
hlt

.test_color:
    vms     ; switch to VRAM addressing mode
    ; TODO
    ret

; set_pixel (subroutine) - set a pixel on the screen
; r0 = X-coordinate (0, 0 top left)
; r1 = Y-coordinate
; r2 = color (6-bit wide)
.set_pixel
    vms     ; switch to VRAM addressing mode
    muli    r1, 240 ; y = (y * height)
    add     r1, r0 ; y += x
    ; normalize color
    andi    r2, 0b111111 ; 6 bit for color - r1 & 0b11_11_11
    ; set pixel  (r1 now has the VRAM position, r2 has color)
    ; to set pixel, it isnt easy, because each byte has 8 bits, but we need
    ; to set 6 and spare 2 for later use
    wms     ; switch back to CPU addressing mode  
    ret

; r2 = color data (11_11_11_00)
; r1 = index
.arr_set
    muli    r1, 6
    mov     r3, r1 ; offset
    mov     r4, r1 ; byte index
    mod     r3, 8  ; 6*i % 8
    div     r4, 8  ; 6*i / 8
    ; r5 will be the scapegoat
    lmb     r5, r4 ; load byte at "byte index" to r4
    mov     r6, r2 ; r6 will be the placeholder of R2
    shr     r6, r3 ; cut off X bytes from the tail of R2
    or      r5, r6 ; merge first byte + part of byte 2 together
    smb     r4, r5 ; write the byte back to index
    ; write second byte (depends)
    ldi     r5, 2  ; compare
    cmp     r3, r5 
    jgt     .write_sec_byte
    ret

.write_sec_byte
    addi    r4, 1
    ldi     r5, 8
    sub     r5, r3
    ret

    
    
