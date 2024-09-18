(define (domain blocksword)
    (:predicates
        (sin_nada_encima ?x)
        (mas_pequeno ?x ?y)
        (encima_de ?x ?y)
    )
    (:action mover
        :parameters (?inicial ?final ?disco)
        :precondition (and 
            (sin_nada_encima ?disco) 
            (sin_nada_encima ?final)
            (encima_de ?disco ?inicial) 
            (mas_pequeno ?disco ?final)            
        )
        :effect (and 
            (sin_nada_encima ?inicial)                        
            (not 
                (sin_nada_encima ?final)
            )
            (encima_de ?disco ?final)
            (not
                (encima_de ?disco ?inicial)
            )
        )
    )    
)