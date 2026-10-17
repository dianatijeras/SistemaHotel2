"use client";

import * as React from "react";
import * as SelectPrimitive from "@radix-ui/react-select";
import {
  CheckIcon,
  ChevronDownIcon,
  ChevronUpIcon,
} from "lucide-react";

import { cn } from "./utils";

/**
 * Select es un componente de selección que permite a los usuarios elegir una opción de una lista desplegable.
 * Utiliza el componente SelectPrimitive de Radix UI para manejar la lógica de selección y el estado, mientras que se personaliza la apariencia con estilos y clases CSS.
 * El componente incluye subcomponentes como SelectTrigger, SelectContent, SelectItem, entre otros, para construir la interfaz de usuario del selector.
 * @param props
 * @constructor
 */
function Select({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Root>) {
  return <SelectPrimitive.Root data-slot="select" {...props} />;
}

/**
 * SelectGroup es un subcomponente de Select que se utiliza para agrupar opciones dentro del selector.
 * Permite organizar las opciones en categorías o secciones, mejorando la usabilidad y la navegación dentro del selector.
 * Este componente se renderiza como un contenedor para las opciones agrupadas y puede incluir un título o etiqueta para identificar el grupo.
 * @param props
 * @constructor
 */
function SelectGroup({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Group>) {
  return <SelectPrimitive.Group data-slot="select-group" {...props} />;
}

/**
 * SelectValue es un subcomponente de Select que representa el valor seleccionado actualmente en el selector.
 * Se muestra dentro del SelectTrigger y se actualiza automáticamente cuando el usuario selecciona una opción diferente.
 * Este componente es esencial para mostrar al usuario qué opción ha seleccionado, proporcionando retroalimentación visual sobre su elección.
 * @param props
 * @constructor
 */
function SelectValue({
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Value>) {
  return <SelectPrimitive.Value data-slot="select-value" {...props} />;
}

/**
 * SelectTrigger es un subcomponente de Select que actúa como el botón o área interactiva que el usuario hace clic para abrir el menú desplegable del selector.
 * Este componente es responsable de mostrar el valor seleccionado actualmente (a través de SelectValue) y de manejar la interacción del usuario para abrir o cerrar el menú.
 * Además, se personaliza con estilos para mejorar su apariencia y usabilidad, y puede incluir un ícono (como ChevronDownIcon) para indicar que es un elemento desplegable.
 * @param className
 * @param size
 * @param children
 * @param props
 * @constructor
 */
function SelectTrigger({
  className,
  size = "default",
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Trigger> & {
  size?: "sm" | "default";
}) {
  return (
    <SelectPrimitive.Trigger
      data-slot="select-trigger"
      data-size={size}
      className={cn(
        "border-input data-[placeholder]:text-muted-foreground [&_svg:not([class*='text-'])]:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 dark:hover:bg-input/50 flex w-full items-center justify-between gap-2 rounded-md border bg-input-background px-3 py-2 text-sm whitespace-nowrap transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 data-[size=default]:h-9 data-[size=sm]:h-8 *:data-[slot=select-value]:line-clamp-1 *:data-[slot=select-value]:flex *:data-[slot=select-value]:items-center *:data-[slot=select-value]:gap-2 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className,
      )}
      {...props}
    >
      {children}
      <SelectPrimitive.Icon asChild>
        <ChevronDownIcon className="size-4 opacity-50" />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  );
}

/**
 * SelectContent es un subcomponente de Select que representa el contenido desplegable del selector, donde se muestran las opciones disponibles para seleccionar.
 * Este componente se renderiza dentro de un portal para asegurar que se muestre correctamente sobre otros elementos de la interfaz.
 * Incluye estilos para controlar su apariencia, animaciones para la apertura y cierre, y puede posicionarse utilizando Popper.js para garantizar que se alinee correctamente con el SelectTrigger.
 * Además, maneja el desplazamiento si hay muchas opciones, proporcionando una experiencia de usuario fluida al navegar por las opciones del selector.
 * @param className
 * @param children
 * @param position
 * @param props
 * @constructor
 */
function SelectContent({
  className,
  children,
  position = "popper",
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Content>) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        data-slot="select-content"
        className={cn(
          "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 relative z-50 max-h-(--radix-select-content-available-height) min-w-[8rem] origin-(--radix-select-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-md border shadow-md",
          position === "popper" &&
            "data-[side=bottom]:translate-y-1 data-[side=left]:-translate-x-1 data-[side=right]:translate-x-1 data-[side=top]:-translate-y-1",
          className,
        )}
        position={position}
        {...props}
      >
        <SelectScrollUpButton />
        <SelectPrimitive.Viewport
          className={cn(
            "p-1",
            position === "popper" &&
              "h-[var(--radix-select-trigger-height)] w-full min-w-[var(--radix-select-trigger-width)] scroll-my-1",
          )}
        >
          {children}
        </SelectPrimitive.Viewport>
        <SelectScrollDownButton />
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  );
}

/**
 * SelectLabel es un subcomponente de Select que se utiliza para mostrar una etiqueta o título dentro del menú desplegable del selector.
 * Esta etiqueta puede ser útil para agrupar opciones o proporcionar contexto adicional sobre las opciones que se están mostrando.
 * El componente se estiliza para diferenciarlo visualmente de las opciones seleccionables, utilizando clases CSS para controlar su apariencia y posición dentro del menú.
 * @param className
 * @param props
 * @constructor
 */
function SelectLabel({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Label>) {
  return (
    <SelectPrimitive.Label
      data-slot="select-label"
      className={cn("text-muted-foreground px-2 py-1.5 text-xs", className)}
      {...props}
    />
  );
}

/**
 * SelectItem es un subcomponente de Select que representa una opción individual dentro del menú desplegable del selector.
 * Cada SelectItem es interactivo y puede ser seleccionado por el usuario.
 * Este componente se estiliza para proporcionar retroalimentación visual cuando se selecciona o se enfoca, utilizando clases CSS para controlar su apariencia en diferentes estados (normal, enfocado, seleccionado, deshabilitado).
 * Además, incluye un indicador (como CheckIcon) para mostrar claramente qué opción está actualmente seleccionada.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function SelectItem({
  className,
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Item>) {
  return (
    <SelectPrimitive.Item
      data-slot="select-item"
      className={cn(
        "focus:bg-accent focus:text-accent-foreground [&_svg:not([class*='text-'])]:text-muted-foreground relative flex w-full cursor-default items-center gap-2 rounded-sm py-1.5 pr-8 pl-2 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4 *:[span]:last:flex *:[span]:last:items-center *:[span]:last:gap-2",
        className,
      )}
      {...props}
    >
      <span className="absolute right-2 flex size-3.5 items-center justify-center">
        <SelectPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </SelectPrimitive.ItemIndicator>
      </span>
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
    </SelectPrimitive.Item>
  );
}

/**
 * SelectSeparator es un subcomponente de Select que se utiliza para agregar una línea divisoria entre grupos de opciones dentro del menú desplegable del selector.
 * Este componente ayuda a organizar visualmente las opciones, proporcionando una separación clara entre diferentes secciones o categorías de opciones.
 * Se estiliza como una línea horizontal utilizando clases CSS para controlar su apariencia, como el color y el grosor, y se posiciona adecuadamente dentro del menú para mantener una estructura clara y legible.
 * @param className
 * @param props
 * @constructor
 */
function SelectSeparator({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Separator>) {
  return (
    <SelectPrimitive.Separator
      data-slot="select-separator"
      className={cn("bg-border pointer-events-none -mx-1 my-1 h-px", className)}
      {...props}
    />
  );
}

/**
 * SelectScrollUpButton y SelectScrollDownButton son subcomponentes de Select que proporcionan botones para desplazarse hacia arriba o hacia abajo dentro del menú desplegable del selector cuando hay muchas opciones.
 * Estos botones se muestran cuando el contenido del menú excede la altura disponible, permitiendo a los usuarios navegar fácilmente por las opciones sin tener que usar la barra de desplazamiento.
 * Se estilizan para ser claramente visibles y accesibles, utilizando íconos (como ChevronUpIcon y ChevronDownIcon) para indicar su función, y se posicionan adecuadamente dentro del menú para facilitar su uso.
 * @param className
 * @param props
 * @constructor
 */
function SelectScrollUpButton({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.ScrollUpButton>) {
  return (
    <SelectPrimitive.ScrollUpButton
      data-slot="select-scroll-up-button"
      className={cn(
        "flex cursor-default items-center justify-center py-1",
        className,
      )}
      {...props}
    >
      <ChevronUpIcon className="size-4" />
    </SelectPrimitive.ScrollUpButton>
  );
}

/**
 * SelectScrollDownButton es un subcomponente de Select que proporciona un botón para desplazarse hacia abajo dentro del menú desplegable del selector cuando hay muchas opciones.
 * Este botón se muestra cuando el contenido del menú excede la altura disponible, permitiendo a los usuarios navegar fácilmente por las opciones sin tener que usar la barra de desplazamiento.
 * Se estiliza para ser claramente visible y accesible, utilizando un ícono (como ChevronDownIcon) para indicar su función, y se posiciona adecuadamente dentro del menú para facilitar su uso.
 * @param className
 * @param props
 * @constructor
 */
function SelectScrollDownButton({
  className,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.ScrollDownButton>) {
  return (
    <SelectPrimitive.ScrollDownButton
      data-slot="select-scroll-down-button"
      className={cn(
        "flex cursor-default items-center justify-center py-1",
        className,
      )}
      {...props}
    >
      <ChevronDownIcon className="size-4" />
    </SelectPrimitive.ScrollDownButton>
  );
}

export {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectScrollDownButton,
  SelectScrollUpButton,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
};
