(define (problem phanoi)
  (:domain blocksword)
  (:objects
    disco1 disco2 disco3 disco4 disco5 torre1 torre2 torre3
  )
  (:init
    (mas_pequeno disco1 torre1)    
    (mas_pequeno disco1 torre2)
    (mas_pequeno disco1 torre3)

    (mas_pequeno disco2 torre1)    
    (mas_pequeno disco2 torre2)
    (mas_pequeno disco2 torre3)

    (mas_pequeno disco3 torre1)    
    (mas_pequeno disco3 torre2)
    (mas_pequeno disco3 torre3)

    (mas_pequeno disco4 torre1)    
    (mas_pequeno disco4 torre2)
    (mas_pequeno disco4 torre3)

    (mas_pequeno disco5 torre1)    
    (mas_pequeno disco5 torre2)
    (mas_pequeno disco5 torre3)

    (mas_pequeno disco1 disco2)
    (mas_pequeno disco1 disco3)
    (mas_pequeno disco1 disco4)
    (mas_pequeno disco1 disco5)

    (mas_pequeno disco2 disco3)
    (mas_pequeno disco2 disco4)
    (mas_pequeno disco2 disco5)
    
    (mas_pequeno disco3 disco4)
    (mas_pequeno disco3 disco5)    
    (mas_pequeno disco4 disco5)

    (sin_nada_encima torre2)
    (sin_nada_encima torre3)
    (sin_nada_encima disco1)

    (encima_de disco5 torre1)
    (encima_de disco4 disco5)
    (encima_de disco3 disco4)
    (encima_de disco2 disco3)
    (encima_de disco1 disco2)
  )
  (:goal
    (and
      (encima_de disco5 torre3)
      (encima_de disco4 disco5)
      (encima_de disco3 disco4)
      (encima_de disco2 disco3)
      (encima_de disco1 disco2)
    )
  )
)