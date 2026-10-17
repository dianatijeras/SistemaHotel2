"use client";

import * as React from "react";
import * as AlertDialogPrimitive from "@radix-ui/react-alert-dialog";

import { cn } from "./utils";
import { buttonVariants } from "./button";

/**
 * Este componente es una envoltura personalizada alrededor de los componentes de AlertDialog de Radix UI.
 * Proporciona estilos predeterminados y una estructura para crear diálogos de alerta en tu aplicación.
 * Puedes usarlo para mostrar mensajes importantes o solicitar confirmaciones a los usuarios.
 */
function AlertDialog({
                         ...props
                     }: React.ComponentProps<typeof AlertDialogPrimitive.Root>) {
    return <AlertDialogPrimitive.Root data-slot="alert-dialog" {...props} />;
}

/**
 * El componente AlertDialogTrigger es un envoltorio para el componente Trigger de AlertDialog de Radix UI.
 * @param props
 * @constructor
 */
function AlertDialogTrigger({
                                ...props
                            }: React.ComponentProps<typeof AlertDialogPrimitive.Trigger>) {
    return (
        <AlertDialogPrimitive.Trigger data-slot="alert-dialog-trigger" {...props} />
    );
}

/**
 * El componente AlertDialogPortal es un envoltorio para el componente Portal de AlertDialog de Radix UI.
 * @param props
 * @constructor
 */
function AlertDialogPortal({
                               ...props
                           }: React.ComponentProps<typeof AlertDialogPrimitive.Portal>) {
    return (
        <AlertDialogPrimitive.Portal data-slot="alert-dialog-portal" {...props} />
    );
}

/**
 * El componente AlertDialogOverlay es un envoltorio para el componente Overlay de AlertDialog de Radix UI.
 * Proporciona un fondo oscuro semitransparente detrás del diálogo de alerta para enfocar la atención del usuario en el contenido del diálogo.
 * @param props
 * @constructor
 */
function AlertDialogOverlay({
                                className,
                                ...props
                            }: React.ComponentProps<typeof AlertDialogPrimitive.Overlay>) {
    return (
        <AlertDialogPrimitive.Overlay
            data-slot="alert-dialog-overlay"
            className={cn(
                "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/50",
                className,
            )}
            {...props}
        />
    );
}

/**
 * El componente AlertDialogContent es un envoltorio para el componente Content de AlertDialog de Radix UI.
 * Proporciona el contenido principal del diálogo de alerta, incluyendo estilos predeterminados para la apariencia y animaciones.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogContent({
                                className,
                                ...props
                            }: React.ComponentProps<typeof AlertDialogPrimitive.Content>) {
    return (
        <AlertDialogPortal>
            <AlertDialogOverlay />
            <AlertDialogPrimitive.Content
                data-slot="alert-dialog-content"
                className={cn(
                    "bg-background data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] gap-4 rounded-lg border p-6 shadow-lg duration-200 sm:max-w-lg",
                    className,
                )}
                {...props}
            />
        </AlertDialogPortal>
    );
}

/**
 * El componente AlertDialogHeader es un envoltorio para el encabezado del diálogo de alerta.
 * Proporciona estilos predeterminados para el encabezado, como la disposición en columna y el centrado del texto.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogHeader({
                               className,
                               ...props
                           }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="alert-dialog-header"
            className={cn("flex flex-col gap-2 text-center sm:text-left", className)}
            {...props}
        />
    );
}


/**
 * El componente AlertDialogFooter es un envoltorio para el pie de página del diálogo de alerta.
 * Proporciona estilos predeterminados para el pie de página, como la disposición en columna-reversa y la justificación al final en pantallas pequeñas.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogFooter({
                               className,
                               ...props
                           }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="alert-dialog-footer"
            className={cn(
                "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end",
                className,
            )}
            {...props}
        />
    );
}

/**
 * El componente AlertDialogTitle es un envoltorio para el título del diálogo de alerta.
 * Proporciona estilos predeterminados para el título, como el tamaño de fuente y el peso.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogTitle({
                              className,
                              ...props
                          }: React.ComponentProps<typeof AlertDialogPrimitive.Title>) {
    return (
        <AlertDialogPrimitive.Title
            data-slot="alert-dialog-title"
            className={cn("text-lg font-semibold", className)}
            {...props}
        />
    );
}

/**
 * El componente AlertDialogDescription es un envoltorio para la descripción del diálogo de alerta.
 * Proporciona estilos predeterminados para la descripción, como el color de texto y el tamaño de fuente.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogDescription({
                                    className,
                                    ...props
                                }: React.ComponentProps<typeof AlertDialogPrimitive.Description>) {
    return (
        <AlertDialogPrimitive.Description
            data-slot="alert-dialog-description"
            className={cn("text-muted-foreground text-sm", className)}
            {...props}
        />
    );
}

/**
 * El componente AlertDialogAction es un envoltorio para la acción del diálogo de alerta.
 * Proporciona estilos predeterminados para la acción, como el uso de variantes de botón para resaltar la acción principal.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogAction({
                               className,
                               ...props
                           }: React.ComponentProps<typeof AlertDialogPrimitive.Action>) {
    return (
        <AlertDialogPrimitive.Action
            className={cn(buttonVariants(), className)}
            {...props}
        />
    );
}

/**
 * El componente AlertDialogCancel es un envoltorio para la acción de cancelación del diálogo de alerta.
 * Proporciona estilos predeterminados para la acción de cancelación, como el uso de variantes de botón con estilo de contorno para diferenciarla de la acción principal.
 * @param className
 * @param props
 * @constructor
 */
function AlertDialogCancel({
                               className,
                               ...props
                           }: React.ComponentProps<typeof AlertDialogPrimitive.Cancel>) {
    return (
        <AlertDialogPrimitive.Cancel
            className={cn(buttonVariants({ variant: "outline" }), className)}
            {...props}
        />
    );
}

export {
    AlertDialog,
    AlertDialogPortal,
    AlertDialogOverlay,
    AlertDialogTrigger,
    AlertDialogContent,
    AlertDialogHeader,
    AlertDialogFooter,
    AlertDialogTitle,
    AlertDialogDescription,
    AlertDialogAction,
    AlertDialogCancel,
};
