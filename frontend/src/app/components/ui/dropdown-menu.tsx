"use client";

import * as React from "react";
import * as DropdownMenuPrimitive from "@radix-ui/react-dropdown-menu";
import { CheckIcon, ChevronRightIcon, CircleIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * Componente DropdownMenu que utiliza Radix UI para crear un menú desplegable.
 * Este componente es altamente personalizable y se compone de varios subcomponentes para construir la estructura del menú, como el trigger, el contenido, los items, etc.
 * Cada subcomponente acepta props específicos para su funcionalidad y estilo.
 * @param props
 * @constructor
 */
function DropdownMenu({
                          ...props
                      }: React.ComponentProps<typeof DropdownMenuPrimitive.Root>) {
    return <DropdownMenuPrimitive.Root data-slot="dropdown-menu" {...props} />;
}

/**
 * Componente DropdownMenuPortal que se utiliza para renderizar el contenido del menú en un portal, lo que permite que el menú se muestre correctamente incluso si el trigger está dentro de un contenedor con overflow oculto.
 * @param props
 * @constructor
 */
function DropdownMenuPortal({
                                ...props
                            }: React.ComponentProps<typeof DropdownMenuPrimitive.Portal>) {
    return (
        <DropdownMenuPrimitive.Portal data-slot="dropdown-menu-portal" {...props} />
    );
}

/**
 * Componente DropdownMenuTrigger que se utiliza para activar la apertura del menú desplegable. Este componente puede ser cualquier elemento interactivo, como un botón o un enlace, y acepta props para personalizar su apariencia y comportamiento.
 * @param props
 * @constructor
 */
function DropdownMenuTrigger({
                                 ...props
                             }: React.ComponentProps<typeof DropdownMenuPrimitive.Trigger>) {
    return (
        <DropdownMenuPrimitive.Trigger
            data-slot="dropdown-menu-trigger"
            {...props}
        />
    );
}

/**
 * Componente DropdownMenuContent que representa el contenido del menú desplegable.
 * Este componente se renderiza dentro de un portal para asegurar que se muestre correctamente en la pantalla.
 * Acepta props para personalizar su apariencia, como el className para estilos personalizados y sideOffset para ajustar la posición del menú con respecto al trigger.
 * @param className
 * @param sideOffset
 * @param props
 * @constructor
 */
function DropdownMenuContent({
                                 className,
                                 sideOffset = 4,
                                 ...props
                             }: React.ComponentProps<typeof DropdownMenuPrimitive.Content>) {
    return (
        <DropdownMenuPrimitive.Portal>
            <DropdownMenuPrimitive.Content
                data-slot="dropdown-menu-content"
                sideOffset={sideOffset}
                className={cn(
                    "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 max-h-(--radix-dropdown-menu-content-available-height) min-w-[8rem] origin-(--radix-dropdown-menu-content-transform-origin) overflow-x-hidden overflow-y-auto rounded-md border p-1 shadow-md",
                    className,
                )}
                {...props}
            />
        </DropdownMenuPrimitive.Portal>
    );
}

/**
 * Componente DropdownMenuGroup que se utiliza para agrupar elementos relacionados dentro del menú desplegable.
 * Este componente no tiene estilos propios, pero puede ser útil para organizar visualmente los items del menú y aplicar estilos personalizados a través de props.
 * @param props
 * @constructor
 */
function DropdownMenuGroup({
                               ...props
                           }: React.ComponentProps<typeof DropdownMenuPrimitive.Group>) {
    return (
        <DropdownMenuPrimitive.Group data-slot="dropdown-menu-group" {...props} />
    );
}

/**
 * Componente DropdownMenuItem que representa un elemento individual dentro del menú desplegable.
 * Este componente acepta props para personalizar su apariencia y comportamiento, como className para estilos personalizados, inset para agregar un margen a la izquierda y variant para aplicar estilos específicos según el tipo de item (por ejemplo, "destructive" para acciones peligrosas).
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente en todo el menú.
 * @param className
 * @param inset
 * @param variant
 * @param props
 * @constructor
 */
function DropdownMenuItem({
                              className,
                              inset,
                              variant = "default",
                              ...props
                          }: React.ComponentProps<typeof DropdownMenuPrimitive.Item> & {
    inset?: boolean;
    variant?: "default" | "destructive";
}) {
    return (
        <DropdownMenuPrimitive.Item
            data-slot="dropdown-menu-item"
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
 * Componente DropdownMenuCheckboxItem que representa un elemento de tipo checkbox dentro del menú desplegable.
 * Este componente acepta props para personalizar su apariencia y comportamiento, como className para estilos personalizados y checked para indicar si el checkbox está marcado o no.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, incluyendo la visualización de un icono de check cuando el checkbox está marcado.
 * @param className
 * @param children
 * @param checked
 * @param props
 * @constructor
 */
function DropdownMenuCheckboxItem({
                                      className,
                                      children,
                                      checked,
                                      ...props
                                  }: React.ComponentProps<typeof DropdownMenuPrimitive.CheckboxItem>) {
    return (
        <DropdownMenuPrimitive.CheckboxItem
            data-slot="dropdown-menu-checkbox-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            checked={checked}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <DropdownMenuPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
            {children}
        </DropdownMenuPrimitive.CheckboxItem>
    );
}

/**
 * Componente DropdownMenuRadioGroup que se utiliza para agrupar elementos de tipo radio dentro del menú desplegable.
 * Este componente no tiene estilos propios, pero es esencial para organizar los radio items y asegurar que solo uno de ellos pueda estar seleccionado a la vez.
 * Acepta props específicos para su funcionalidad, como value para indicar el valor del radio seleccionado y onValueChange para manejar los cambios en la selección.
 * @param props
 * @constructor
 */
function DropdownMenuRadioGroup({
                                    ...props
                                }: React.ComponentProps<typeof DropdownMenuPrimitive.RadioGroup>) {
    return (
        <DropdownMenuPrimitive.RadioGroup
            data-slot="dropdown-menu-radio-group"
            {...props}
        />
    );
}

/**
 * Componente DropdownMenuRadioItem que representa un elemento de tipo radio dentro del menú desplegable.
 * Este componente acepta props para personalizar su apariencia y comportamiento, como className para estilos personalizados.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, incluyendo la visualización de un icono de círculo cuando el radio está seleccionado.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function DropdownMenuRadioItem({
                                   className,
                                   children,
                                   ...props
                               }: React.ComponentProps<typeof DropdownMenuPrimitive.RadioItem>) {
    return (
        <DropdownMenuPrimitive.RadioItem
            data-slot="dropdown-menu-radio-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-sm py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <DropdownMenuPrimitive.ItemIndicator>
          <CircleIcon className="size-2 fill-current" />
        </DropdownMenuPrimitive.ItemIndicator>
      </span>
            {children}
        </DropdownMenuPrimitive.RadioItem>
    );
}

/**
 * Componente DropdownMenuLabel que se utiliza para mostrar un título o etiqueta dentro del menú desplegable.
 * Este componente acepta props para personalizar su apariencia, como className para estilos personalizados e inset para agregar un margen a la izquierda.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, como un tamaño de fuente más pequeño y un peso de fuente medio.
 * @param className
 * @param inset
 * @param props
 * @constructor
 */
function DropdownMenuLabel({
                               className,
                               inset,
                               ...props
                           }: React.ComponentProps<typeof DropdownMenuPrimitive.Label> & {
    inset?: boolean;
}) {
    return (
        <DropdownMenuPrimitive.Label
            data-slot="dropdown-menu-label"
            data-inset={inset}
            className={cn(
                "px-2 py-1.5 text-sm font-medium data-[inset]:pl-8",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente DropdownMenuSeparator que se utiliza para agregar una línea divisoria entre los elementos del menú desplegable, ayudando a organizar visualmente los items en grupos.
 * Este componente acepta props para personalizar su apariencia, como className para estilos personalizados.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, como un fondo de color de borde y un tamaño de altura de 1 píxel.
 * @param className
 * @param props
 * @constructor
 */
function DropdownMenuSeparator({
                                   className,
                                   ...props
                               }: React.ComponentProps<typeof DropdownMenuPrimitive.Separator>) {
    return (
        <DropdownMenuPrimitive.Separator
            data-slot="dropdown-menu-separator"
            className={cn("bg-border -mx-1 my-1 h-px", className)}
            {...props}
        />
    );
}

/**
 * Componente DropdownMenuShortcut que se utiliza para mostrar un atajo de teclado asociado a un elemento del menú desplegable, proporcionando una indicación visual de la combinación de teclas que se puede usar para activar esa acción.
 * Este componente acepta props para personalizar su apariencia, como className para estilos personalizados.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, como un color de texto atenuado, un tamaño de fuente más pequeño y un espaciado a la izquierda automático para alinear el atajo a la derecha del item.
 * @param className
 * @param props
 * @constructor
 */
function DropdownMenuShortcut({
                                  className,
                                  ...props
                              }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="dropdown-menu-shortcut"
            className={cn(
                "text-muted-foreground ml-auto text-xs tracking-widest",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente DropdownMenuSub que se utiliza para crear un submenú dentro del menú desplegable principal, permitiendo organizar las opciones en niveles jerárquicos y mejorar la navegación dentro del menú.
 * Este componente acepta props para personalizar su apariencia y comportamiento, como className para estilos personalizados.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, como un fondo de color de popover, un borde redondeado y una sombra para destacar el submenú.
 * @param props
 * @constructor
 */
function DropdownMenuSub({
                             ...props
                         }: React.ComponentProps<typeof DropdownMenuPrimitive.Sub>) {
    return <DropdownMenuPrimitive.Sub data-slot="dropdown-menu-sub" {...props} />;
}

/**
 * Componente DropdownMenuSubTrigger que se utiliza para activar la apertura de un submenú dentro del menú desplegable principal.
 * Este componente se muestra como un elemento del menú principal y, al hacer clic en él, se despliega el submenú asociado.
 * Acepta props para personalizar su apariencia y comportamiento, como className para estilos personalizados e inset para agregar un margen a la izquierda, lo que ayuda a diferenciar visualmente el trigger del submenú de los demás items del menú principal.
 * Además, se aplican estilos predeterminados para asegurar una apariencia consistente, como un fondo de acento y un color de texto específico cuando el submenú está abierto o cuando el trigger está enfocado.
 * @param className
 * @param inset
 * @param children
 * @param props
 * @constructor
 */
function DropdownMenuSubTrigger({
                                    className,
                                    inset,
                                    children,
                                    ...props
                                }: React.ComponentProps<typeof DropdownMenuPrimitive.SubTrigger> & {
    inset?: boolean;
}) {
    return (
        <DropdownMenuPrimitive.SubTrigger
            data-slot="dropdown-menu-sub-trigger"
            data-inset={inset}
            className={cn(
                "focus:bg-accent focus:text-accent-foreground data-[state=open]:bg-accent data-[state=open]:text-accent-foreground flex cursor-default items-center rounded-sm px-2 py-1.5 text-sm outline-hidden select-none data-[inset]:pl-8",
                className,
            )}
            {...props}
        >
            {children}
            <ChevronRightIcon className="ml-auto size-4" />
        </DropdownMenuPrimitive.SubTrigger>
    );
}

/**
 * Componente DropdownMenuSubContent que representa el contenido del submenú dentro del menú desplegable principal.
 * Este componente se renderiza cuando se activa el trigger del submenú y muestra las opciones relacionadas con esa sección específica del menú.
 * Acepta props para personalizar su apariencia, como className para estilos personalizados, y se aplican estilos predeterminados para asegurar una apariencia consistente, como un fondo de color de popover, un borde redondeado y una sombra para destacar el submenú.
 * @param className
 * @param props
 * @constructor
 */
function DropdownMenuSubContent({
                                    className,
                                    ...props
                                }: React.ComponentProps<typeof DropdownMenuPrimitive.SubContent>) {
    return (
        <DropdownMenuPrimitive.SubContent
            data-slot="dropdown-menu-sub-content"
            className={cn(
                "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 min-w-[8rem] origin-(--radix-dropdown-menu-content-transform-origin) overflow-hidden rounded-md border p-1 shadow-lg",
                className,
            )}
            {...props}
        />
    );
}

export {
    DropdownMenu,
    DropdownMenuPortal,
    DropdownMenuTrigger,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuLabel,
    DropdownMenuItem,
    DropdownMenuCheckboxItem,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuShortcut,
    DropdownMenuSub,
    DropdownMenuSubTrigger,
    DropdownMenuSubContent,
};
