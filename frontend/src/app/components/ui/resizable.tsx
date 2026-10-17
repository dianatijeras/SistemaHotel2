"use client";

import * as React from "react";
import { GripVerticalIcon } from "lucide-react";
import * as ResizablePrimitive from "react-resizable-panels";

import { cn } from "./utils";

/**
 * ResizablePlanelGroup es un componente que se utiliza para crear un grupo de paneles redimensionables.
 * Este componente es parte de la biblioteca react-resizable-panels y se utiliza para organizar y manejar la disposición de los paneles dentro de una interfaz de usuario.
 * El componente acepta varias propiedades, incluyendo className para aplicar estilos personalizados y otras propiedades específicas de ResizablePrimitive.PanelGroup.
 * Al utilizar este componente, los desarrolladores pueden crear interfaces flexibles y adaptables que permiten a los usuarios ajustar el tamaño de los paneles según sus necesidades.
 * @param className
 * @param props
 * @constructor
 */
function ResizablePanelGroup({
  className,
  ...props
}: React.ComponentProps<typeof ResizablePrimitive.PanelGroup>) {
  return (
    <ResizablePrimitive.PanelGroup
      data-slot="resizable-panel-group"
      className={cn(
        "flex h-full w-full data-[panel-group-direction=vertical]:flex-col",
        className,
      )}
      {...props}
    />
  );
}

/**
 * ResizablePanel es un componente que representa un panel individual dentro de un grupo de paneles redimensionables.
 * Este componente es parte de la biblioteca react-resizable-panels y se utiliza para definir el contenido y la funcionalidad de cada panel dentro del grupo.
 * El componente acepta varias propiedades, incluyendo className para aplicar estilos personalizados y otras propiedades específicas de ResizablePrimitive.Panel.
 * Al utilizar este componente, los desarrolladores pueden crear interfaces flexibles y adaptables que permiten a los usuarios ajustar el tamaño de los paneles según sus necesidades.
 * @param props
 * @constructor
 */
function ResizablePanel({
  ...props
}: React.ComponentProps<typeof ResizablePrimitive.Panel>) {
  return <ResizablePrimitive.Panel data-slot="resizable-panel" {...props} />;
}

/**
 * ResizableHandle es un componente que representa el controlador de redimensionamiento entre los paneles en un grupo de paneles redimensionables.
 * Este componente es parte de la biblioteca react-resizable-panels y se utiliza para permitir a los usuarios ajustar el tamaño de los paneles arrastrando el controlador.
 * El componente acepta varias propiedades, incluyendo withHandle para mostrar u ocultar el controlador visual, className para aplicar estilos personalizados y otras propiedades específicas de ResizablePrimitive.PanelResizeHandle.
 * Al utilizar este componente, los desarrolladores pueden crear interfaces flexibles y adaptables que permiten a los usuarios ajustar el tamaño de los paneles según sus necesidades.
 * @param withHandle
 * @param className
 * @param props
 * @constructor
 */
function ResizableHandle({
  withHandle,
  className,
  ...props
}: React.ComponentProps<typeof ResizablePrimitive.PanelResizeHandle> & {
  withHandle?: boolean;
}) {
  return (
    <ResizablePrimitive.PanelResizeHandle
      data-slot="resizable-handle"
      className={cn(
        "bg-border focus-visible:ring-ring relative flex w-px items-center justify-center after:absolute after:inset-y-0 after:left-1/2 after:w-1 after:-translate-x-1/2 focus-visible:ring-1 focus-visible:ring-offset-1 focus-visible:outline-hidden data-[panel-group-direction=vertical]:h-px data-[panel-group-direction=vertical]:w-full data-[panel-group-direction=vertical]:after:left-0 data-[panel-group-direction=vertical]:after:h-1 data-[panel-group-direction=vertical]:after:w-full data-[panel-group-direction=vertical]:after:-translate-y-1/2 data-[panel-group-direction=vertical]:after:translate-x-0 [&[data-panel-group-direction=vertical]>div]:rotate-90",
        className,
      )}
      {...props}
    >
      {withHandle && (
        <div className="bg-border z-10 flex h-4 w-3 items-center justify-center rounded-xs border">
          <GripVerticalIcon className="size-2.5" />
        </div>
      )}
    </ResizablePrimitive.PanelResizeHandle>
  );
}

export { ResizablePanelGroup, ResizablePanel, ResizableHandle };
