(define (domain blocksword)
(:predicates
(sin_nada_encima ?x)
(encima_mesa ?x)
(brazo_libre)
(sujeto ?x)
(encima_bloque ?x ?y)
)
(:action coger
:parameters (?ob)
:precondition (and (sin_nada_encima ?ob)(encima_mesa ?ob)(brazo_libre))
:effect (and (sujeto ?ob) (not (sin_nada_encima ?ob)) (not (encima_mesa ?ob))(not
(brazo_libre)))
)
(:action soltar
:parameters (?ob)
:precondition (and (sujeto ?ob))
:effect (and (sin_nada_encima ?ob) (brazo_libre) (encima_mesa ?ob) (not (sujeto ?ob)))
)
(:action apilar
:parameters (?ob ?underob)
:precondition (and (sin_nada_encima ?underob)(sujeto ?ob))
:effect (and (sin_nada_encima ?ob) (brazo_libre) (encima_bloque ?ob ?underob) (not (sujeto
?ob))(not (sin_nada_encima ?underob)))
)
(:action desapilar
:parameters (?ob ?underob)
:precondition (and (encima_bloque ?ob ?underob) (sin_nada_encima ?ob) (brazo_libre))
:effect (and (sujeto ?ob) (sin_nada_encima ?underob) (not (encima_bloque ?ob ?underob)) (not
(sin_nada_encima ?ob)) (not (brazo_libre))))
)