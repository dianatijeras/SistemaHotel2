"use client";

import * as React from "react";
import * as PopoverPrimitive from "@radix-ui/react-popover";

import { cn } from "./utils";

/**
 * Popover es un componente de interfaz de usuario que muestra contenido adicional en una ventana emergente cuando el usuario interactúa con un elemento específico.
 * Es útil para mostrar información contextual, opciones adicionales o acciones relacionadas sin ocupar espacio permanente en la interfaz.
 * @param props
 * @constructor
 */
function Popover({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Root>) {
  return <PopoverPrimitive.Root data-slot="popover" {...props} />;
}

function PopoverTrigger({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Trigger>) {
  return <PopoverPrimitive.Trigger data-slot="popover-trigger" {...props} />;
}

/**
 * PopoverContent es el componente que representa el contenido que se muestra dentro del Popover.
 * Este componente se renderiza dentro de un Portal, lo que significa que se monta en un nodo separado del DOM para evitar problemas de superposición y posicionamiento.
 * El contenido del Popover se puede alinear y posicionar utilizando las propiedades align y sideOffset, respectivamente.
 * @param className
 * @param align
 * @param sideOffset
 * @param props
 * @constructor
 */
function PopoverContent({
  className,
  align = "center",
  sideOffset = 4,
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Content>) {
  return (
    <PopoverPrimitive.Portal>
      <PopoverPrimitive.Content
        data-slot="popover-content"
        align={align}
        sideOffset={sideOffset}
        className={cn(
          "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 w-72 origin-(--radix-popover-content-transform-origin) rounded-md border p-4 shadow-md outline-hidden",
          className,
        )}
        {...props}
      />
    </PopoverPrimitive.Portal>
  );
}

/**
 * PopoverAnchor es un componente que se utiliza para anclar el Popover a un elemento específico en la interfaz de usuario.
 * Este componente se utiliza para definir el punto de referencia desde el cual se posicionará el Popover.
 * Al usar PopoverAnchor, el Popover se posicionará en relación con el elemento al que está anclado, lo que permite una experiencia de usuario más intuitiva y contextual.
 * @param props
 * @constructor
 */
function PopoverAnchor({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Anchor>) {
  return <PopoverPrimitive.Anchor data-slot="popover-anchor" {...props} />;
}

export { Popover, PopoverTrigger, PopoverContent, PopoverAnchor };
