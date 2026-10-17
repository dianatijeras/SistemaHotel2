"use client";

import * as React from "react";
import { Drawer as DrawerPrimitive } from "vaul";

import { cn } from "./utils";

/**
 * Componente de Drawer que utiliza el componente raíz de Vaul.
 * Este componente se encarga de renderizar el contenedor principal del Drawer y pasarle las props necesarias.
 * @param props
 * @constructor
 */
function Drawer({
                    ...props
                }: React.ComponentProps<typeof DrawerPrimitive.Root>) {
    return <DrawerPrimitive.Root data-slot="drawer" {...props} />;
}

/**
 * Componente de Trigger para el Drawer que utiliza el componente Trigger de Vaul.
 * Este componente se encarga de renderizar el botón o elemento que activará la apertura del Drawer y pasarle las props necesarias.
 * @param props
 * @constructor
 */
function DrawerTrigger({
                           ...props
                       }: React.ComponentProps<typeof DrawerPrimitive.Trigger>) {
    return <DrawerPrimitive.Trigger data-slot="drawer-trigger" {...props} />;
}

/**
 * Componente de Portal para el Drawer que utiliza el componente Portal de Vaul.
 * Este componente se encarga de renderizar el contenido del Drawer en un contenedor separado del DOM principal, lo que permite que el Drawer se muestre por encima de otros elementos.
 * @param props
 * @constructor
 */
function DrawerPortal({
                          ...props
                      }: React.ComponentProps<typeof DrawerPrimitive.Portal>) {
    return <DrawerPrimitive.Portal data-slot="drawer-portal" {...props} />;
}

/**
 * Componente de Close para el Drawer que utiliza el componente Close de Vaul.
 * Este componente se encarga de renderizar el botón o elemento que cerrará el Drawer y pasarle las props necesarias.
 * @param props
 * @constructor
 */
function DrawerClose({
                         ...props
                     }: React.ComponentProps<typeof DrawerPrimitive.Close>) {
    return <DrawerPrimitive.Close data-slot="drawer-close" {...props} />;
}

/**
 * Componente de Overlay para el Drawer que utiliza el componente Overlay de Vaul.
 * Este componente se encarga de renderizar la capa de fondo que se muestra detrás del Drawer cuando está abierto, y pasarle las props necesarias.
 * @param className
 * @param props
 * @constructor
 */
function DrawerOverlay({
                           className,
                           ...props
                       }: React.ComponentProps<typeof DrawerPrimitive.Overlay>) {
    return (
        <DrawerPrimitive.Overlay
            data-slot="drawer-overlay"
            className={cn(
                "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/50",
                className,
            )}
            {...props}
        />
    );
}

/**
 * Componente de Content para el Drawer que utiliza el componente Content de Vaul.
 * Este componente se encarga de renderizar el contenido principal del Drawer, incluyendo la estructura y el diseño, y pasarle las props necesarias.
 * @param className
 * @param children
 * @param props
 * @constructor
 */
function DrawerContent({
                           className,
                           children,
                           ...props
                       }: React.ComponentProps<typeof DrawerPrimitive.Content>) {
    return (
        <DrawerPortal data-slot="drawer-portal">
            <DrawerOverlay />
            <DrawerPrimitive.Content
                data-slot="drawer-content"
                className={cn(
                    "group/drawer-content bg-background fixed z-50 flex h-auto flex-col",
                    "data-[vaul-drawer-direction=top]:inset-x-0 data-[vaul-drawer-direction=top]:top-0 data-[vaul-drawer-direction=top]:mb-24 data-[vaul-drawer-direction=top]:max-h-[80vh] data-[vaul-drawer-direction=top]:rounded-b-lg data-[vaul-drawer-direction=top]:border-b",
                    "data-[vaul-drawer-direction=bottom]:inset-x-0 data-[vaul-drawer-direction=bottom]:bottom-0 data-[vaul-drawer-direction=bottom]:mt-24 data-[vaul-drawer-direction=bottom]:max-h-[80vh] data-[vaul-drawer-direction=bottom]:rounded-t-lg data-[vaul-drawer-direction=bottom]:border-t",
                    "data-[vaul-drawer-direction=right]:inset-y-0 data-[vaul-drawer-direction=right]:right-0 data-[vaul-drawer-direction=right]:w-3/4 data-[vaul-drawer-direction=right]:border-l data-[vaul-drawer-direction=right]:sm:max-w-sm",
                    "data-[vaul-drawer-direction=left]:inset-y-0 data-[vaul-drawer-direction=left]:left-0 data-[vaul-drawer-direction=left]:w-3/4 data-[vaul-drawer-direction=left]:border-r data-[vaul-drawer-direction=left]:sm:max-w-sm",
                    className,
                )}
                {...props}
            >
                <div className="bg-muted mx-auto mt-4 hidden h-2 w-[100px] shrink-0 rounded-full group-data-[vaul-drawer-direction=bottom]/drawer-content:block" />
                {children}
            </DrawerPrimitive.Content>
        </DrawerPortal>
    );
}

/**
 * Componente de Header para el Drawer que utiliza un div con estilos personalizados.
 * Este componente se encarga de renderizar la sección de encabezado del Drawer, donde se pueden colocar elementos como el título o botones de acción, y pasarle las props necesarias.
 * @param className
 * @param props
 * @constructor
 */
function DrawerHeader({ className, ...props }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="drawer-header"
            className={cn("flex flex-col gap-1.5 p-4", className)}
            {...props}
        />
    );
}

/**
 * Componente de Footer para el Drawer que utiliza un div con estilos personalizados.
 * Este componente se encarga de renderizar la sección de pie de página del Drawer, donde se pueden colocar elementos como botones de acción o información adicional, y pasarle las props necesarias.
 * @param className
 * @param props
 * @constructor
 */
function DrawerFooter({ className, ...props }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="drawer-footer"
            className={cn("mt-auto flex flex-col gap-2 p-4", className)}
            {...props}
        />
    );
}

/**
 * Componente de Title para el Drawer que utiliza el componente Title de Vaul.
 * Este componente se encarga de renderizar el título del Drawer, aplicando estilos personalizados y pasando las props necesarias.
 * @param className
 * @param props
 * @constructor
 */
function DrawerTitle({
                         className,
                         ...props
                     }: React.ComponentProps<typeof DrawerPrimitive.Title>) {
    return (
        <DrawerPrimitive.Title
            data-slot="drawer-title"
            className={cn("text-foreground font-semibold", className)}
            {...props}
        />
    );
}

/**
 * Componente de Description para el Drawer que utiliza el componente Description de Vaul.
 * Este componente se encarga de renderizar la descripción o información adicional del Drawer, aplicando estilos personalizados y pasando las props necesarias.
 * @param className
 * @param props
 * @constructor
 */
function DrawerDescription({
                               className,
                               ...props
                           }: React.ComponentProps<typeof DrawerPrimitive.Description>) {
    return (
        <DrawerPrimitive.Description
            data-slot="drawer-description"
            className={cn("text-muted-foreground text-sm", className)}
            {...props}
        />
    );
}

export {
    Drawer,
    DrawerPortal,
    DrawerOverlay,
    DrawerTrigger,
    DrawerClose,
    DrawerContent,
    DrawerHeader,
    DrawerFooter,
    DrawerTitle,
    DrawerDescription,
};
