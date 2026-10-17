"use client";

import * as React from "react";
import { Command as CommandPrimitive } from "cmdk";
import { SearchIcon } from "lucide-react";

import { cn } from "./utils";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "./dialog";

/**
 * Componente comando que se utiliza para crear una paleta de comandos.
 * Se puede utilizar dentro de un diálogo para mostrar la paleta de comandos.
 * @param className
 * @param props
 * @constructor
 */
function Command({
                     className,
                     ...props
                 }: React.ComponentProps<typeof CommandPrimitive>) {
    return (
        <CommandPrimitive
            data-slot="command"
            className={cn(
                "bg-popover text-popover-foreground flex h-full w-full flex-col overflow-hidden rounded-md",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente de diálogo que se utiliza para mostrar la paleta de comandos.
 * @param title
 * @param description
 * @param children
 * @param props
 * @constructor
 */
function CommandDialog({
                           title = "Command Palette",
                           description = "Search for a command to run...",
                           children,
                           ...props
                       }: React.ComponentProps<typeof Dialog> & {
    title?: string;
    description?: string;
}) {
    return (
        <Dialog {...props}>
            <DialogHeader className="sr-only">
                <DialogTitle>{title}</DialogTitle>
                <DialogDescription>{description}</DialogDescription>
            </DialogHeader>
            <DialogContent className="overflow-hidden p-0">
                <Command className="[&_[cmdk-group-heading]]:text-muted-foreground **:data-[slot=command-input-wrapper]:h-12 [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:font-medium [&_[cmdk-group]]:px-2 [&_[cmdk-group]:not([hidden])_~[cmdk-group]]:pt-0 [&_[cmdk-input-wrapper]_svg]:h-5 [&_[cmdk-input-wrapper]_svg]:w-5 [&_[cmdk-input]]:h-12 [&_[cmdk-item]]:px-2 [&_[cmdk-item]]:py-3 [&_[cmdk-item]_svg]:h-5 [&_[cmdk-item]_svg]:w-5">
                    {children}
                </Command>
            </DialogContent>
        </Dialog>
    );
}

/**
 * Componente de entrada que se utiliza para escribir el comando que se desea ejecutar.
 * @param className
 * @param props
 * @constructor
 */
function CommandInput({
                          className,
                          ...props
                      }: React.ComponentProps<typeof CommandPrimitive.Input>) {
    return (
        <div
            data-slot="command-input-wrapper"
            className="flex h-9 items-center gap-2 border-b px-3"
        >
            <SearchIcon className="size-4 shrink-0 opacity-50" />
            <CommandPrimitive.Input
                data-slot="command-input"
                className={cn(
                    "placeholder:text-muted-foreground flex h-10 w-full rounded-md bg-transparent py-3 text-sm outline-hidden disabled:cursor-not-allowed disabled:opacity-50",
                    className,
                )}
                {...props}
            />
        </div>
    );
}

/**
 * Componente de lista que se utiliza para mostrar los comandos disponibles.
 * @param className
 * @param props
 * @constructor
 */
function CommandList({
                         className,
                         ...props
                     }: React.ComponentProps<typeof CommandPrimitive.List>) {
    return (
        <CommandPrimitive.List
            data-slot="command-list"
            className={cn(
                "max-h-[300px] scroll-py-1 overflow-x-hidden overflow-y-auto",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que se muestra cuando no hay comandos disponibles.
 * @param props
 * @constructor
 */
function CommandEmpty({
                          ...props
                      }: React.ComponentProps<typeof CommandPrimitive.Empty>) {
    return (
        <CommandPrimitive.Empty
            data-slot="command-empty"
            className="py-6 text-center text-sm"
            {...props}
        />
    );
}

/**
 * Componente de grupo que se utiliza para agrupar los comandos disponibles.
 * @param className
 * @param props
 * @constructor
 */
function CommandGroup({
                          className,
                          ...props
                      }: React.ComponentProps<typeof CommandPrimitive.Group>) {
    return (
        <CommandPrimitive.Group
            data-slot="command-group"
            className={cn(
                "text-foreground [&_[cmdk-group-heading]]:text-muted-foreground overflow-hidden p-1 [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1.5 [&_[cmdk-group-heading]]:text-xs [&_[cmdk-group-heading]]:font-medium",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente de separador que se utiliza para separar los comandos disponibles.
 * @param className
 * @param props
 * @constructor
 */
function CommandSeparator({
                              className,
                              ...props
                          }: React.ComponentProps<typeof CommandPrimitive.Separator>) {
    return (
        <CommandPrimitive.Separator
            data-slot="command-separator"
            className={cn("bg-border -mx-1 h-px", className)}
            {...props}
        />
    );
}

/**
 * Componente de ítem que se utiliza para mostrar un comando disponible.
 * @param className
 * @param props
 * @constructor
 */
function CommandItem({
                         className,
                         ...props
                     }: React.ComponentProps<typeof CommandPrimitive.Item>) {
    return (
        <CommandPrimitive.Item
            data-slot="command-item"
            className={cn(
                "data-[selected=true]:bg-accent data-[selected=true]:text-accent-foreground [&_svg:not([class*='text-'])]:text-muted-foreground relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-hidden select-none data-[disabled=true]:pointer-events-none data-[disabled=true]:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente de atajo que se utiliza para mostrar el atajo de teclado de un comando disponible.
 * @param className
 * @param props
 * @constructor
 */
function CommandShortcut({
                             className,
                             ...props
                         }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="command-shortcut"
            className={cn(
                "text-muted-foreground ml-auto text-xs tracking-widest",
                className,
            )}
            {...props}
        />
    );
}

export {
    Command,
    CommandDialog,
    CommandInput,
    CommandList,
    CommandEmpty,
    CommandGroup,
    CommandItem,
    CommandShortcut,
    CommandSeparator,
};
