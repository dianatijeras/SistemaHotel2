"use client";

import * as React from "react";
import * as ContextMenuPrimitive from "@radix-ui/react-context-menu";
import { CheckIcon, ChevronRightIcon, CircleIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * Componenete de menú contextual basado en Radix UI Context Menu.
 * Proporciona una estructura para crear menús contextuales con soporte para submenús, grupos, elementos de menú, etc.
 * Cada componente se estiliza utilizando la función `cn` para combinar clases de Tailwind CSS y admite props adicionales para personalización.
 * @param props
 * @constructor
 */
function ContextMenu({
                         ...props
                     }: React.ComponentProps<typeof ContextMenuPrimitive.Root>) {
    return <ContextMenuPrimitive.Root data-slot="context-menu" {...props} />;
}

/**
 * Componente que actúa como el disparador del menú contextual.
 * Se utiliza para envolver el elemento que activará la aparición del menú contextual al hacer clic derecho o mediante otros eventos de interacción.
 * @param props
 * @constructor
 */
function ContextMenuTrigger({
                                ...props
                            }: React.ComponentProps<typeof ContextMenuPrimitive.Trigger>) {
    return (
        <ContextMenuPrimitive.Trigger data-slot="context-menu-trigger" {...props} />
    );
}

/**
 * Componente que representa un grupo de elementos dentro del menú contextual.
 * Se utiliza para organizar elementos relacionados dentro del menú, proporcionando una separación visual entre ellos.
 * @param props
 * @constructor
 */
function ContextMenuGroup({
                              ...props
                          }: React.ComponentProps<typeof ContextMenuPrimitive.Group>) {
    return (
        <ContextMenuPrimitive.Group data-slot="context-menu-group" {...props} />
    );
}

/**
 * Componente que actúa como un portal para renderizar el contenido del menú contextual en un contenedor separado del DOM.
 * Esto es útil para evitar problemas de superposición y garantizar que el menú contextual se muestre correctamente en la interfaz de usuario, incluso si el disparador está dentro de un contenedor con estilos específicos.
 * @param props
 * @constructor
 */
function ContextMenuPortal({
                               ...props
                           }: React.ComponentProps<typeof ContextMenuPrimitive.Portal>) {
    return (
        <ContextMenuPrimitive.Portal data-slot="context-menu-portal" {...props} />
    );
}

/**
 * Componente que representa un submenú dentro del menú contextual.
 * Permite anidar menús contextuales dentro de otros, proporcionando una estructura jerárquica para organizar opciones relacionadas.
 * El submenú se activa al interactuar con un elemento que tiene un submenú asociado, mostrando opciones adicionales relacionadas con esa opción específica.
 * @param props
 * @constructor
 */
function ContextMenuSub({
                            ...props
                        }: React.ComponentProps<typeof ContextMenuPrimitive.Sub>) {
    return <ContextMenuPrimitive.Sub data-slot="context-menu-sub" {...props} />;
}

/**
 * Componente que representa un grupo de elementos de radio dentro del menú contextual.
 * Se utiliza para agrupar opciones mutuamente exclusivas, donde solo una opción puede estar seleccionada a la vez.
 * Al seleccionar una opción dentro del grupo de radio, las demás opciones se deseleccionan automáticamente, lo que garantiza que solo una opción esté activa en ese grupo específico.
 * @param props
 * @constructor
 */
function ContextMenuRadioGroup({
                                   ...props
                               }: React.ComponentProps<typeof ContextMenuPrimitive.RadioGroup>) {
    return (
        <ContextMenuPrimitive.RadioGroup
            data-slot="context-menu-radio-group"
            {...props}
        />
    );
}

/**
 * Componente que representa un disparador para un submenú dentro del menú contextual.
 * @param className
 * @param inset
 * @param children
 * @param props
 * @constructor
 */
function ContextMenuSubTrigger({
                                   className,
                                   inset,
                                   children,
                                   ...props
                               }: React.ComponentProps<typeof ContextMenuPrimitive.SubTrigger> & {
    inset?: boolean;
}) {
    return (
        <ContextMenuPrimitive.SubTrigger
            data-slot="context-menu-sub-trigger"
            data-inset={inset}
            className={cn(
                "focus:bg-accent focus:text-accent-foreground data-[state=open]:bg-accent data-[state=open]:text-accent-foreground flex cursor-default items-center rounded-sm px-2 py-1.5 text-sm outline-hidden select-none data-[inset]:pl-8 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        >
            {children}
            <ChevronRightIcon className="ml-auto" />
        </ContextMenuPrimitive.SubTrigger>
    );
}

/**
 * Componente que representa el contenido de un submenú dentro del menú contextual.
 * @param className
 * @param props
 * @constructor
 */
function ContextMenuSubContent({
                                   className,
                                   ...props
                               }: React.ComponentProps<typeof ContextMenuPrimitive.SubContent>) {
    return (
        <ContextMenuPrimitive.SubContent
            data-slot="context-menu-sub-content"
            className={cn(
                "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 min-w-[8rem] origin-(--radix-context-menu-content-transform-origin) overflow-hidden rounded-md border p-1 shadow-lg",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que representa el contenido principal del menú contextual.
 * @param className
 * @param props
 * @constructor
 */
function ContextMenuContent({
                                className,
                                ...props
                            }: React.ComponentProps<typeof ContextMenuPrimitive.Content>) {
    return (
        <ContextMenuPrimitive.Portal>
            <ContextMenuPrimitive.Content
                data-slot="context-menu-content"
                className={cn(
                    "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 max-h-(--radix-context-menu-content-available-height) min-w-[8rem] origin-(--radix-context-menu-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-md border p-1 shadow-md",
                    className,
                )}
                {...props}
            />
        </ContextMenuPrimitive.Portal>
    );
}

/**
 * Componente que representa un elemento de menú dentro del menú contextual.
 * Puede tener variantes como "default" o "destructive" para indicar la naturaleza de la acción asociada con ese elemento.
 * Además, puede tener un estilo de inserción para proporcionar una apariencia visual diferente, como un sangrado adicional para elementos anidados dentro de submenús.
 * @param className
 * @param inset
 * @param variant
 * @param props
 * @constructor
 */
function ContextMenuItem({
                             className,
                             inset,
                             variant = "default",
                             ...props
                         }: React.ComponentProps<typeof ContextMenuPrimitive.Item> & {
    inset?: boolean;
    variant?: "default" | "destructive";
}) {
    return (
        <ContextMenuPrimitive.Item
            data-slot="context-menu-item"
            data-inset={inset}
            data-variant={variant}
            className={cn(
                "focus:bg-accent focus:text-accent-foreground data-[variant=destructive]:text-destructive data-[variant=destructive]:focus:bg-destructive/10 dark:data-[variant=destructive]:focus:bg-destructive/20 data-[variant=destructive]:focus:text-destructive data-[variant=destructive]:*:[svg]:!text-destructive [&_svg:not([class*='text-'])]:text-muted-foreground relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 data-[inset]:pl-8 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que representa un elemento de menú con una casilla de verificación dentro del menú contextual.
 * Permite a los usuarios seleccionar o deseleccionar opciones dentro del menú, proporcionando una indicación visual clara de si una opción está activa o no.
 * El componente utiliza un icono de verificación para mostrar el estado seleccionado y se estiliza de manera similar a otros elementos de menú para mantener la coherencia visual.
 * @param className
 * @param children
 * @param checked
 * @param props
 * @constructor
 */
function ContextMenuCheckboxItem({
                                     className,
                                     children,
                                     checked,
                                     ...props
                                 }: React.ComponentProps<typeof ContextMenuPrimitive.CheckboxItem>) {
    return (
        <ContextMenuPrimitive.CheckboxItem
            data-slot="context-menu-checkbox-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            checked={checked}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <ContextMenuPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </ContextMenuPrimitive.ItemIndicator>
      </span>
            {children}
        </ContextMenuPrimitive.CheckboxItem>
    );
}

/**
 * Componente que representa un elemento de menú con una opción de radio dentro del menú contextual.
 * Permite a los usuarios seleccionar una opción dentro de un grupo de opciones mutuamente exclusivas, proporcionando una indicación visual clara de cuál opción está seleccionada.
 * El componente utiliza un icono de círculo para mostrar el estado seleccionado y se estiliza de manera similar a otros elementos de menú para mantener la coherencia visual.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function ContextMenuRadioItem({
                                  className,
                                  children,
                                  ...props
                              }: React.ComponentProps<typeof ContextMenuPrimitive.RadioItem>) {
    return (
        <ContextMenuPrimitive.RadioItem
            data-slot="context-menu-radio-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <ContextMenuPrimitive.ItemIndicator>
          <CircleIcon className="size-2 fill-current" />
        </ContextMenuPrimitive.ItemIndicator>
      </span>
            {children}
        </ContextMenuPrimitive.RadioItem>
    );
}

/**
 * Componente que representa una etiqueta dentro del menú contextual.
 * Se utiliza para proporcionar información adicional o descripciones dentro del menú, ayudando a los usuarios a comprender mejor las opciones disponibles.
 * La etiqueta se estiliza de manera distintiva para diferenciarla de los elementos de menú interactivos, y puede tener un estilo de inserción para proporcionar una apariencia visual diferente, como un sangrado adicional para elementos anidados dentro de submenús.
 * @param className
 * @param inset
 * @param props
 * @constructor
 */
function ContextMenuLabel({
                              className,
                              inset,
                              ...props
                          }: React.ComponentProps<typeof ContextMenuPrimitive.Label> & {
    inset?: boolean;
}) {
    return (
        <ContextMenuPrimitive.Label
            data-slot="context-menu-label"
            data-inset={inset}
            className={cn(
                "text-foreground px-2 py-1.5 text-sm font-medium data-[inset]:pl-8",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que representa un separador dentro del menú contextual.
 * Se utiliza para dividir visualmente grupos de elementos dentro del menú, proporcionando una separación clara entre diferentes secciones o categorías de opciones.
 * El separador se estiliza como una línea horizontal para mejorar la legibilidad y organización del menú.
 * @param className
 * @param props
 * @constructor
 */
function ContextMenuSeparator({
                                  className,
                                  ...props
                              }: React.ComponentProps<typeof ContextMenuPrimitive.Separator>) {
    return (
        <ContextMenuPrimitive.Separator
            data-slot="context-menu-separator"
            className={cn("bg-border -mx-1 my-1 h-px", className)}
            {...props}
        />
    );
}

/**
 * Componente que representa un atajo de teclado dentro del menú contextual.
 * Se utiliza para mostrar combinaciones de teclas asociadas con acciones específicas dentro del menú, proporcionando a los usuarios una forma rápida de acceder a esas acciones sin tener que navegar por el menú.
 * El atajo de teclado se estiliza de manera distintiva para diferenciarlo de otros elementos del menú, y se posiciona generalmente al final del elemento de menú para mejorar la legibilidad.
 * @param className
 * @param props
 * @constructor
 */
function ContextMenuShortcut({
                                 className,
                                 ...props
                             }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="context-menu-shortcut"
            className={cn(
                "text-muted-foreground ml-auto text-xs tracking-widest",
                className,
            )}
            {...props}
        />
    );
}

export {
    ContextMenu,
    ContextMenuTrigger,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuCheckboxItem,
    ContextMenuRadioItem,
    ContextMenuLabel,
    ContextMenuSeparator,
    ContextMenuShortcut,
    ContextMenuGroup,
    ContextMenuPortal,
    ContextMenuSub,
    ContextMenuSubContent,
    ContextMenuSubTrigger,
    ContextMenuRadioGroup,
};
