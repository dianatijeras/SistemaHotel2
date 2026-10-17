"use client";

import * as React from "react";
import * as AvatarPrimitive from "@radix-ui/react-avatar";

import { cn } from "./utils";

/**
 * Avatar es un componente de interfaz de usuario que se utiliza para mostrar una imagen de perfil o una representación visual de un usuario.
 * Este componente se compone de tres partes principales: Avatar, AvatarImage y AvatarFallback.
 * Avatar es el contenedor principal que envuelve la imagen del avatar, AvatarImage es el componente que muestra la imagen del avatar, y AvatarFallback es el componente que se muestra cuando la imagen del avatar no está disponible o no se puede cargar.
 * El componente Avatar utiliza estilos personalizados para garantizar que la imagen del avatar se muestre correctamente y tenga una apariencia consistente en toda la aplicación.
 * @param className
 * @param props
 * @constructor
 */
function Avatar({
                    className,
                    ...props
                }: React.ComponentProps<typeof AvatarPrimitive.Root>) {
    return (
        <AvatarPrimitive.Root
            data-slot="avatar"
            className={cn(
                "relative flex size-10 shrink-0 overflow-hidden rounded-full",
                className,
            )}
            {...props}
        />
    );
}

/**
 * AvatarImage es un componente que se utiliza para mostrar la imagen del avatar dentro del componente Avatar.
 * Este componente se encarga de mostrar la imagen del avatar y se asegura de que se ajuste correctamente al contenedor del avatar.
 * Si la imagen del avatar no está disponible o no se puede cargar, el componente AvatarFallback se mostrará en su lugar.
 * @param className
 * @param props
 * @constructor
 */
function AvatarImage({
                         className,
                         ...props
                     }: React.ComponentProps<typeof AvatarPrimitive.Image>) {
    return (
        <AvatarPrimitive.Image
            data-slot="avatar-image"
            className={cn("aspect-square size-full", className)}
            {...props}
        />
    );
}

/**
 * AvatarFallback es un componente que se muestra cuando la imagen del avatar no está disponible o no se puede cargar.
 * Este componente proporciona una representación visual alternativa para el avatar, como un icono o una letra inicial del nombre del usuario.
 */
function AvatarFallback({
                            className,
                            ...props
                        }: React.ComponentProps<typeof AvatarPrimitive.Fallback>) {
    return (
        <AvatarPrimitive.Fallback
            data-slot="avatar-fallback"
            className={cn(
                "bg-muted flex size-full items-center justify-center rounded-full",
                className,
            )}
            {...props}
        />
    );
}

export { Avatar, AvatarImage, AvatarFallback };
