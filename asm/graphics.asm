@data
    screen_width      .uint8   320 ; 320 x 240 (240p 4 : 3)
    screen_height     .uint8   240
    vram_working_ptr  .uint16  0
@text
; start program
call    .test_color

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
    muli    r1, #screen_height ; y = (y * height)
    add     r1, r0 ; y += x
    ; normalize color
    andi    r2, 0x3F ; get three bytes (6 bit for color) r1 & 0b11_11_11
    ; set pixel  (r1 now has the VRAM position, r2 has color)
    ; to set pixel, it isnt easy, because each byte has 8 bits, but we need
    ; to set 6 and spare 2 for later use  
    ret

