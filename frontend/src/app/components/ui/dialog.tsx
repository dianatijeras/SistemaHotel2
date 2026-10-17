"use client";

import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { XIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * Componentes de diálogo basados en Radix UI.
 * Estos componentes proporcionan una estructura y estilos predefinidos para crear diálogos modales en la aplicación.
 * Se pueden personalizar mediante props y clases CSS.
 * @param props
 * @constructor
 */
function Dialog({
                    ...props
                }: React.ComponentProps<typeof DialogPrimitive.Root>) {
    return <DialogPrimitive.Root data-slot="dialog" {...props} />;
}

/**
 * Componente que actúa como disparador para abrir el diálogo.
 * Se puede usar cualquier elemento como trigger, y al hacer clic en él, se abrirá el diálogo asociado.
 * @param props
 * @constructor
 */
function DialogTrigger({
                           ...props
                       }: React.ComponentProps<typeof DialogPrimitive.Trigger>) {
    return <DialogPrimitive.Trigger data-slot="dialog-trigger" {...props} />;
}

/**
 * Componente que se encarga de renderizar el contenido del diálogo en un portal, lo que permite que el diálogo se muestre por encima de otros elementos en la página.
 * @param props
 * @constructor
 */
function DialogPortal({
                          ...props
                      }: React.ComponentProps<typeof DialogPrimitive.Portal>) {
    return <DialogPrimitive.Portal data-slot="dialog-portal" {...props} />;
}

/**
 * Componente que representa el botón de cierre del diálogo.
 * Al hacer clic en este botón, el diálogo se cerrará automáticamente.
 * @param props
 * @constructor
 */
function DialogClose({
                         ...props
                     }: React.ComponentProps<typeof DialogPrimitive.Close>) {
    return <DialogPrimitive.Close data-slot="dialog-close" {...props} />;
}

/**
 * Componente que representa la superposición del diálogo, que se muestra detrás del contenido del diálogo para oscurecer el fondo y enfocar la atención en el diálogo.
 * @param className
 * @param props
 * @constructor
 */
function DialogOverlay({
                           className,
                           ...props
                       }: React.ComponentProps<typeof DialogPrimitive.Overlay>) {
    return (
        <DialogPrimitive.Overlay
            data-slot="dialog-overlay"
            className={cn(
                "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/50",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que representa el contenido principal del diálogo.
 * Este componente se renderiza dentro del portal y contiene el contenido que se desea mostrar en el diálogo, así como un botón de cierre para cerrar el diálogo.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function DialogContent({
                           className,
                           children,
                           ...props
                       }: React.ComponentProps<typeof DialogPrimitive.Content>) {
    return (
        <DialogPortal data-slot="dialog-portal">
            <DialogOverlay />
            <DialogPrimitive.Content
                data-slot="dialog-content"
                className={cn(
                    "bg-background data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] gap-4 rounded-lg border p-6 shadow-lg duration-200 sm:max-w-lg",
                    className,
                )}
                {...props}
            >
                {children}
                <DialogPrimitive.Close className="ring-offset-background focus:ring-ring data-[state=open]:bg-accent data-[state=open]:text-muted-foreground absolute top-4 right-4 rounded-xs opacity-70 transition-opacity hover:opacity-100 focus:ring-2 focus:ring-offset-2 focus:outline-hidden disabled:pointer-events-none [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4">
                    <XIcon />
                    <span className="sr-only">Close</span>
                </DialogPrimitive.Close>
            </DialogPrimitive.Content>
        </DialogPortal>
    );
}

/**
 * Componente que representa el encabezado del diálogo, que generalmente contiene el título y la descripción del diálogo.
 * @param className
 * @param props
 * @constructor
 */
function DialogHeader({ className, ...props }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="dialog-header"
            className={cn("flex flex-col gap-2 text-center sm:text-left", className)}
            {...props}
        />
    );
}

/**
 * Componente que representa el pie de página del diálogo, que generalmente contiene los botones de acción para cerrar el diálogo o realizar otras acciones relacionadas con el diálogo.
 * @param className
 * @param props
 * @constructor
 */
function DialogFooter({ className, ...props }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="dialog-footer"
            className={cn(
                "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente que representa el título del diálogo, que generalmente se muestra en el encabezado del diálogo y proporciona una breve descripción del propósito del diálogo.
 * @param className
 * @param props
 * @constructor
 */
function DialogTitle({
                         className,
                         ...props
                     }: React.ComponentProps<typeof DialogPrimitive.Title>) {
    return (
        <DialogPrimitive.Title
            data-slot="dialog-title"
            className={cn("text-lg leading-none font-semibold", className)}
            {...props}
        />
    );
}

/**
 * Componente que representa la descripción del diálogo, que generalmente se muestra debajo del título en el encabezado del diálogo y proporciona información adicional sobre el propósito o contenido del diálogo.
 * @param className
 * @param props
 * @constructor
 */
function DialogDescription({
                               className,
                               ...props
                           }: React.ComponentProps<typeof DialogPrimitive.Description>) {
    return (
        <DialogPrimitive.Description
            data-slot="dialog-description"
            className={cn("text-muted-foreground text-sm", className)}
            {...props}
        />
    );
}

export {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogOverlay,
    DialogPortal,
    DialogTitle,
    DialogTrigger,
};
