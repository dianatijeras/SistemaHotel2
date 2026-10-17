"use client";

import { useTheme } from "next-themes";
import { Toaster as Sonner, ToasterProps } from "sonner";

/**
 * Toaster es un componente que muestra notificaciones en la pantalla. Utiliza la librería Sonner para renderizar las notificaciones y se adapta al tema actual (claro, oscuro o sistema) utilizando el hook useTheme de Next.js.
 * El componente también aplica estilos personalizados para el fondo, texto y borde de las notificaciones.
 * @param props
 * @constructor
 */
const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme();

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className="toaster group"
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
        } as React.CSSProperties
      }
      {...props}
    />
  );
};

export { Toaster };
