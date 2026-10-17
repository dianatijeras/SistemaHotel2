"use client";

import * as React from "react";
import * as SwitchPrimitive from "@radix-ui/react-switch";

import { cn } from "./utils";

/**
 * Switch es un componente de interfaz de usuario que permite a los usuarios alternar entre dos estados, como encendido/apagado o activo/inactivo.
 * Es comúnmente utilizado para representar opciones binarias o configuraciones que pueden ser activadas o desactivadas.
 * El componente Switch se basa en el componente raíz de SwitchPrimitive de Radix UI, y se le aplican estilos personalizados para adaptarse al diseño de la aplicación.
 * El Switch incluye un "thumb" (el círculo que se mueve) que indica visualmente el estado actual del interruptor.
 * El componente también maneja estados como "checked" (marcado) y "unchecked" (desmarcado) para reflejar la selección del usuario.
 * @param className
 * @param props
 * @constructor
 */
function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "peer data-[state=checked]:bg-primary data-[state=unchecked]:bg-switch-background focus-visible:border-ring focus-visible:ring-ring/50 dark:data-[state=unchecked]:bg-input/80 inline-flex h-[1.15rem] w-8 shrink-0 items-center rounded-full border border-transparent transition-all outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className={cn(
          "bg-card dark:data-[state=unchecked]:bg-card-foreground dark:data-[state=checked]:bg-primary-foreground pointer-events-none block size-4 rounded-full ring-0 transition-transform data-[state=checked]:translate-x-[calc(100%-2px)] data-[state=unchecked]:translate-x-0",
        )}
      />
    </SwitchPrimitive.Root>
  );
}

export { Switch };
