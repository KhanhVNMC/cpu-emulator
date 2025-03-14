; jump into the main entry
jmp .entry

.entry
    call    .func1   ; Call first function
    ldi     r0, 69   ; Should execute last (after all RETs)
    hlt              ; Stop execution

.func1
    ldi     r1, 111  ; Load 111 into r1
    call    .func2   ; Call deeper function
    ldi     r1, 222  ; If this executes, .func2 returned correctly
    ret              ; Return to caller (.entry)

.func2
    ldi     r2, 333  ; Load 333 into r2
    call    .func3   ; Call even deeper function
    ldi     r2, 444  ; If this executes, .func3 returned correctly
    ret              ; Return to caller (.func1)

.func3
    ldi     r3, 555  ; Load 555 into r3 (deepest function)
    ret              ; Return to caller (.func2)
