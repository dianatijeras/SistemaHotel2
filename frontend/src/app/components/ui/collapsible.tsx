"use client";

import * as CollapsiblePrimitive from "@radix-ui/react-collapsible";

/**
 * Desplegable que se puede expandir o contraer para mostrar u ocultar contenido adicional.
 * Es útil para organizar información de manera compacta y mejorar la experiencia del usuario al permitirles controlar la visibilidad de ciertos elementos en la interfaz.
 * @param props
 * @constructor
 */
function Collapsible({
                         ...props
                     }: React.ComponentProps<typeof CollapsiblePrimitive.Root>) {
    return <CollapsiblePrimitive.Root data-slot="collapsible" {...props} />;
}

/**
 * Elemento que actúa como disparador para expandir o contraer el contenido del Collapsible.
 * Al hacer clic en este elemento, se alterna la visibilidad del contenido asociado.
 * @param props
 * @constructor
 */
function CollapsibleTrigger({
                                ...props
                            }: React.ComponentProps<typeof CollapsiblePrimitive.CollapsibleTrigger>) {
    return (
        <CollapsiblePrimitive.CollapsibleTrigger
            data-slot="collapsible-trigger"
            {...props}
        />
    );
}

/**
 * Elemento que contiene el contenido adicional que se muestra u oculta cuando se interactúa con el CollapsibleTrigger.
 * Este contenido puede incluir texto, imágenes u otros elementos de la interfaz que se deseen mostrar de manera condicional.
 * @param props
 * @constructor
 */
function CollapsibleContent({
                                ...props
                            }: React.ComponentProps<typeof CollapsiblePrimitive.CollapsibleContent>) {
    return (
        <CollapsiblePrimitive.CollapsibleContent
            data-slot="collapsible-content"
            {...props}
        />
    );
}

export { Collapsible, CollapsibleTrigger, CollapsibleContent };
