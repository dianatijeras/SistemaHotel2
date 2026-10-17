"use client";

import * as React from "react";
import * as MenubarPrimitive from "@radix-ui/react-menubar";
import { CheckIcon, ChevronRightIcon, CircleIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * Menubar es un componente de navegación que se muestra en la parte superior de la aplicación.
 * Permite a los usuarios acceder a diferentes secciones o funcionalidades de la aplicación a través de menús desplegables.
 * @param className
 * @param props
 * @constructor
 */
function Menubar({
                     className,
                     ...props
                 }: React.ComponentProps<typeof MenubarPrimitive.Root>) {
    return (
        <MenubarPrimitive.Root
            data-slot="menubar"
            className={cn(
                "bg-background flex h-9 items-center gap-1 rounded-md border p-1 shadow-xs",
                className,
            )}
            {...props}
        />
    );
}

/**
 * MenubarMenu es un componente que representa un menú desplegable dentro del Menubar.
 * @param props
 * @constructor
 */
function MenubarMenu({
                         ...props
                     }: React.ComponentProps<typeof MenubarPrimitive.Menu>) {
    return <MenubarPrimitive.Menu data-slot="menubar-menu" {...props} />;
}

/**
 * MenubarGroup es un componente que se utiliza para agrupar elementos dentro de un MenubarMenu.
 * @param props
 * @constructor
 */
function MenubarGroup({
                          ...props
                      }: React.ComponentProps<typeof MenubarPrimitive.Group>) {
    return <MenubarPrimitive.Group data-slot="menubar-group" {...props} />;
}

/**
 * MenubarPortal es un componente que se utiliza para renderizar el contenido del menú en un portal, lo que permite que el menú se muestre por encima de otros elementos de la interfaz de usuario.
 * @param props
 * @constructor
 */
function MenubarPortal({
                           ...props
                       }: React.ComponentProps<typeof MenubarPrimitive.Portal>) {
    return <MenubarPrimitive.Portal data-slot="menubar-portal" {...props} />;
}

/**
 * MenubarRadioGroup es un componente que se utiliza para agrupar elementos de tipo radio dentro de un MenubarMenu, lo que permite que solo se pueda seleccionar una opción dentro del grupo.
 * @param props
 * @constructor
 */
function MenubarRadioGroup({
                               ...props
                           }: React.ComponentProps<typeof MenubarPrimitive.RadioGroup>) {
    return (
        <MenubarPrimitive.RadioGroup data-slot="menubar-radio-group" {...props} />
    );
}

/**
 * MenubarTrigger es un componente que se utiliza para activar el menú desplegable cuando se hace clic en él.
 * Generalmente se muestra como un botón o un enlace dentro del Menubar.
 * @param className
 * @param props
 * @constructor
 */
function MenubarTrigger({
                            className,
                            ...props
                        }: React.ComponentProps<typeof MenubarPrimitive.Trigger>) {
    return (
        <MenubarPrimitive.Trigger
            data-slot="menubar-trigger"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground data-[state=open]:bg-accent data-[state=open]:text-accent-foreground flex items-center rounded-sm px-2 py-1 text-sm font-medium outline-hidden select-none",
                className,
            )}
            {...props}
        />
    );
}

/**
 * MenubarContent es un componente que representa el contenido del menú desplegable.
 * Se muestra cuando se activa el MenubarTrigger y contiene los elementos del menú
 * @param className
 * @param align
 * @param alignOffset
 * @param sideOffset
 * @param props
 * @constructor
 */
function MenubarContent({
                            className,
                            align = "start",
                            alignOffset = -4,
                            sideOffset = 8,
                            ...props
                        }: React.ComponentProps<typeof MenubarPrimitive.Content>) {
    return (
        <MenubarPortal>
            <MenubarPrimitive.Content
                data-slot="menubar-content"
                align={align}
                alignOffset={alignOffset}
                sideOffset={sideOffset}
                className={cn(
                    "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 min-w-[12rem] origin-(--radix-menubar-content-transform-origin) overflow-hidden rounded-md border p-1 shadow-md",
                    className,
                )}
                {...props}
            />
        </MenubarPortal>
    );
}

/**
 * MenubarItem es un componente que representa un elemento individual dentro del menú desplegable.
 * Puede ser un enlace, un botón o cualquier otro tipo de elemento interactivo que el usuario pueda seleccionar.
 * @param className
 * @param inset
 * @param variant
 * @param props
 * @constructor
 */
function MenubarItem({
                         className,
                         inset,
                         variant = "default",
                         ...props
                     }: React.ComponentProps<typeof MenubarPrimitive.Item> & {
    inset?: boolean;
    variant?: "default" | "destructive";
}) {
    return (
        <MenubarPrimitive.Item
            data-slot="menubar-item"
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
 * MenubarCheckboxItem es un componente que representa un elemento de tipo checkbox dentro del menú desplegable.
 * @param className
 * @param children
 * @param checked
 * @param props
 * @constructor
 */
function MenubarCheckboxItem({
                                 className,
                                 children,
                                 checked,
                                 ...props
                             }: React.ComponentProps<typeof MenubarPrimitive.CheckboxItem>) {
    return (
        <MenubarPrimitive.CheckboxItem
            data-slot="menubar-checkbox-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-xs py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            checked={checked}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <MenubarPrimitive.ItemIndicator>
          <CheckIcon className="size-4" />
        </MenubarPrimitive.ItemIndicator>
      </span>
            {children}
        </MenubarPrimitive.CheckboxItem>
    );
}

/**
 * MenubarRadioItem es un componente que representa un elemento de tipo radio dentro del menú desplegable.
 * Permite a los usuarios seleccionar una opción de un conjunto de opciones mutuamente exclusivas.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function MenubarRadioItem({
                              className,
                              children,
                              ...props
                          }: React.ComponentProps<typeof MenubarPrimitive.RadioItem>) {
    return (
        <MenubarPrimitive.RadioItem
            data-slot="menubar-radio-item"
            className={cn(
                "focus:bg-accent focus:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-xs py-1.5 pr-2 pl-8 text-sm outline-hidden select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        >
      <span className="pointer-events-none absolute left-2 flex size-3.5 items-center justify-center">
        <MenubarPrimitive.ItemIndicator>
          <CircleIcon className="size-2 fill-current" />
        </MenubarPrimitive.ItemIndicator>
      </span>
            {children}
        </MenubarPrimitive.RadioItem>
    );
}

/**
 * MenubarLabel es un componente que se utiliza para mostrar un título o una etiqueta dentro del menú desplegable.
 * Se utiliza para organizar y categorizar los elementos del menú, proporcionando una mejor experiencia de usuario al facilitar la navegación y comprensión de las opciones disponibles.
 * @param className
 * @param inset
 * @param props
 * @constructor
 */
function MenubarLabel({
                          className,
                          inset,
                          ...props
                      }: React.ComponentProps<typeof MenubarPrimitive.Label> & {
    inset?: boolean;
}) {
    return (
        <MenubarPrimitive.Label
            data-slot="menubar-label"
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
 * MenubarSeparator es un componente que se utiliza para separar visualmente los elementos dentro del menú desplegable.
 * Proporciona una división clara entre diferentes secciones o grupos de opciones, mejorando la organización y legibilidad del menú.
 * @param className
 * @param props
 * @constructor
 */
function MenubarSeparator({
                              className,
                              ...props
                          }: React.ComponentProps<typeof MenubarPrimitive.Separator>) {
    return (
        <MenubarPrimitive.Separator
            data-slot="menubar-separator"
            className={cn("bg-border -mx-1 my-1 h-px", className)}
            {...props}
        />
    );
}

/**
 * MenubarShortcut es un componente que se utiliza para mostrar atajos de teclado asociados a las opciones del menú desplegable.
 * Proporciona a los usuarios una forma rápida de acceder a las funciones del menú utilizando combinaciones de teclas, mejorando la eficiencia y la experiencia de usuario.
 * @param className
 * @param props
 * @constructor
 */
function MenubarShortcut({
                             className,
                             ...props
                         }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="menubar-shortcut"
            className={cn(
                "text-muted-foreground ml-auto text-xs tracking-widest",
                className,
            )}
            {...props}
        />
    );
}

/**
 * MenubarSub es un componente que representa un submenú dentro del menú desplegable.
 * Permite a los usuarios acceder a opciones adicionales relacionadas con una opción principal, proporcionando una estructura jerárquica y organizada para las opciones del menú.
 * @param props
 * @constructor
 */
function MenubarSub({
                        ...props
                    }: React.ComponentProps<typeof MenubarPrimitive.Sub>) {
    return <MenubarPrimitive.Sub data-slot="menubar-sub" {...props} />;
}

/**
 * MenubarSubTrigger es un componente que se utiliza para activar un submenú dentro del menú desplegable.
 * Se muestra como una opción dentro del menú principal y, al hacer clic en ella, se despliega un submenú con opciones adicionales relacionadas con la opción principal.
 * @param className
 * @param inset
 * @param children
 * @param props
 * @constructor
 */
function MenubarSubTrigger({
                               className,
                               inset,
                               children,
                               ...props
                           }: React.ComponentProps<typeof MenubarPrimitive.SubTrigger> & {
    inset?: boolean;
}) {
    return (
        <MenubarPrimitive.SubTrigger
            data-slot="menubar-sub-trigger"
            data-inset={inset}
            className={cn(
                "focus:bg-accent focus:text-accent-foreground data-[state=open]:bg-accent data-[state=open]:text-accent-foreground flex cursor-default items-center rounded-sm px-2 py-1.5 text-sm outline-none select-none data-[inset]:pl-8",
                className,
            )}
            {...props}
        >
            {children}
            <ChevronRightIcon className="ml-auto h-4 w-4" />
        </MenubarPrimitive.SubTrigger>
    );
}

/**
 * MenubarSubContent es un componente que representa el contenido de un submenú dentro del menú desplegable.
 * Se muestra cuando se activa un MenubarSubTrigger y contiene las opciones adicionales relacionadas con la opción principal del menú.
 * @param className
 * @param props
 * @constructor
 */
function MenubarSubContent({
                               className,
                               ...props
                           }: React.ComponentProps<typeof MenubarPrimitive.SubContent>) {
    return (
        <MenubarPrimitive.SubContent
            data-slot="menubar-sub-content"
            className={cn(
                "bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 min-w-[8rem] origin-(--radix-menubar-content-transform-origin) overflow-hidden rounded-md border p-1 shadow-lg",
                className,
            )}
            {...props}
        />
    );
}

export {
    Menubar,
    MenubarPortal,
    MenubarMenu,
    MenubarTrigger,
    MenubarContent,
    MenubarGroup,
    MenubarSeparator,
    MenubarLabel,
    MenubarItem,
    MenubarShortcut,
    MenubarCheckboxItem,
    MenubarRadioGroup,
    MenubarRadioItem,
    MenubarSub,
    MenubarSubTrigger,
    MenubarSubContent,
};
