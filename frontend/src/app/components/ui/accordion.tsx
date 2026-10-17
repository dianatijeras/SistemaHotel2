"use client";

import * as React from "react";
import * as AccordionPrimitive from "@radix-ui/react-accordion";
import { ChevronDownIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * El componente Accordion es un contenedor que permite mostrar y ocultar contenido de forma interactiva. Está compuesto por varios elementos, como AccordionItem, AccordionTrigger y AccordionContent, que trabajan juntos para crear una experiencia de usuario fluida y organizada.
 * El Accordion es útil para organizar información en secciones desplegables, lo que mejora la legibilidad y la navegación en interfaces con mucho contenido.
 * @param props
 * @constructor
 */
function Accordion({
                       ...props
                   }: React.ComponentProps<typeof AccordionPrimitive.Root>) {
    return <AccordionPrimitive.Root data-slot="accordion" {...props} />;
}

/**
 * El componente AccordionItem representa un elemento individual dentro del Accordion.
 * Cada AccordionItem contiene un AccordionTrigger, que es el encabezado que el usuario puede hacer clic para expandir o contraer el contenido asociado, y un AccordionContent, que es la sección de contenido que se muestra u oculta según el estado del trigger.
 * El AccordionItem se encarga de manejar la lógica de expansión y contracción, así como de aplicar estilos específicos para cada elemento dentro del Accordion.
 * @param className
 * @param props
 * @constructor
 */
function AccordionItem({
                           className,
                           ...props
                       }: React.ComponentProps<typeof AccordionPrimitive.Item>) {
    return (
        <AccordionPrimitive.Item
            data-slot="accordion-item"
            className={cn("border-b last:border-b-0", className)}
            {...props}
        />
    );
}

/**
 * El componente AccordionTrigger es el encabezado interactivo de cada AccordionItem.
 * Es el elemento que el usuario puede hacer clic para expandir o contraer el contenido asociado.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function AccordionTrigger({
                              className,
                              children,
                              ...props
                          }: React.ComponentProps<typeof AccordionPrimitive.Trigger>) {
    return (
        <AccordionPrimitive.Header className="flex">
            <AccordionPrimitive.Trigger
                data-slot="accordion-trigger"
                className={cn(
                    "focus-visible:border-ring focus-visible:ring-ring/50 flex flex-1 items-start justify-between gap-4 rounded-md py-4 text-left text-sm font-medium transition-all outline-none hover:underline focus-visible:ring-[3px] disabled:pointer-events-none disabled:opacity-50 [&[data-state=open]>svg]:rotate-180",
                    className,
                )}
                {...props}
            >
                {children}
                <ChevronDownIcon className="text-muted-foreground pointer-events-none size-4 shrink-0 translate-y-0.5 transition-transform duration-200" />
            </AccordionPrimitive.Trigger>
        </AccordionPrimitive.Header>
    );
}

/**
 * El componente AccordionContent es la sección de contenido que se muestra u oculta según el estado del AccordionTrigger.
 * Contiene el contenido que se desea mostrar cuando el trigger está expandido.
 */
function AccordionContent({
                              className,
                              children,
                              ...props
                          }: React.ComponentProps<typeof AccordionPrimitive.Content>) {
    return (
        <AccordionPrimitive.Content
            data-slot="accordion-content"
            className="data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down overflow-hidden text-sm"
            {...props}
        >
            <div className={cn("pt-0 pb-4", className)}>{children}</div>
        </AccordionPrimitive.Content>
    );
}

export { Accordion, AccordionItem, AccordionTrigger, AccordionContent };
