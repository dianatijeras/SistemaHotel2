"use client";

import * as React from "react";
import * as SheetPrimitive from "@radix-ui/react-dialog";
import { XIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * Un componente de hoja deslizante (sheet) que se puede usar para mostrar contenido adicional sin salir de la página actual.
 * Utiliza el componente Dialog de Radix UI como base y lo personaliza para crear una experiencia de hoja deslizante.
 * @param props
 * @constructor
 */
function Sheet({ ...props }: React.ComponentProps<typeof SheetPrimitive.Root>) {
  return <SheetPrimitive.Root data-slot="sheet" {...props} />;
}

/**
 * Un componente de activador para la hoja deslizante.
 * Este componente se utiliza para abrir la hoja cuando se hace clic en él.
 * @param props
 * @constructor
 */
function SheetTrigger({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Trigger>) {
  return <SheetPrimitive.Trigger data-slot="sheet-trigger" {...props} />;
}

/**
 * Un componente de cierre para la hoja deslizante.
 * Este componente se utiliza para cerrar la hoja cuando se hace clic en él.
 */
function SheetClose({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Close>) {
  return <SheetPrimitive.Close data-slot="sheet-close" {...props} />;
}

/**
 * Un componente de portal para la hoja deslizante.
 * Este componente se utiliza para renderizar la hoja en un portal, lo que permite que se muestre por encima de otros elementos en la página.
 * @param props
 * @constructor
 */
function SheetPortal({
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Portal>) {
  return <SheetPrimitive.Portal data-slot="sheet-portal" {...props} />;
}

/**
 * Un componente de superposición para la hoja deslizante.
 * Este componente se utiliza para mostrar una superposición semitransparente detrás de la hoja cuando está abierta, lo que ayuda a enfocar la atención del usuario en la hoja.
 * @param className
 * @param props
 * @constructor
 */
function SheetOverlay({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Overlay>) {
  return (
    <SheetPrimitive.Overlay
      data-slot="sheet-overlay"
      className={cn(
        "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/50",
        className,
      )}
      {...props}
    />
  );
}

/**
 * Un componente de contenido para la hoja deslizante.
 * Este componente se utiliza para mostrar el contenido de la hoja.
 * Se puede configurar para que se deslice desde diferentes lados de la pantalla (arriba, derecha, abajo, izquierda) utilizando la propiedad "side".
 * @param className
 * @param children
 * @param side
 * @param props
 * @constructor
 */
function SheetContent({
  className,
  children,
  side = "right",
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Content> & {
  side?: "top" | "right" | "bottom" | "left";
}) {
  return (
    <SheetPortal>
      <SheetOverlay />
      <SheetPrimitive.Content
        data-slot="sheet-content"
        className={cn(
          "bg-background data-[state=open]:animate-in data-[state=closed]:animate-out fixed z-50 flex flex-col gap-4 shadow-lg transition ease-in-out data-[state=closed]:duration-300 data-[state=open]:duration-500",
          side === "right" &&
            "data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right inset-y-0 right-0 h-full w-3/4 border-l sm:max-w-sm",
          side === "left" &&
            "data-[state=closed]:slide-out-to-left data-[state=open]:slide-in-from-left inset-y-0 left-0 h-full w-3/4 border-r sm:max-w-sm",
          side === "top" &&
            "data-[state=closed]:slide-out-to-top data-[state=open]:slide-in-from-top inset-x-0 top-0 h-auto border-b",
          side === "bottom" &&
            "data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom inset-x-0 bottom-0 h-auto border-t",
          className,
        )}
        {...props}
      >
        {children}
        <SheetPrimitive.Close className="ring-offset-background focus:ring-ring data-[state=open]:bg-secondary absolute top-4 right-4 rounded-xs opacity-70 transition-opacity hover:opacity-100 focus:ring-2 focus:ring-offset-2 focus:outline-hidden disabled:pointer-events-none">
          <XIcon className="size-4" />
          <span className="sr-only">Close</span>
        </SheetPrimitive.Close>
      </SheetPrimitive.Content>
    </SheetPortal>
  );
}

/**
 * Un componente de encabezado para la hoja deslizante.
 * Este componente se utiliza para mostrar un encabezado en la hoja, que puede incluir un título y una descripción.
 * @param className
 * @param props
 * @constructor
 */
function SheetHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-header"
      className={cn("flex flex-col gap-1.5 p-4", className)}
      {...props}
    />
  );
}

/**
 * Un componente de pie de página para la hoja deslizante.
 * Este componente se utiliza para mostrar un pie de página en la hoja, que puede incluir acciones o información adicional.
 * @param className
 * @param props
 * @constructor
 */
function SheetFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-footer"
      className={cn("mt-auto flex flex-col gap-2 p-4", className)}
      {...props}
    />
  );
}

/**
 * Un componente de título para la hoja deslizante.
 * Este componente se utiliza para mostrar un título en el encabezado de la hoja.
 * @param className
 * @param props
 * @constructor
 */
function SheetTitle({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Title>) {
  return (
    <SheetPrimitive.Title
      data-slot="sheet-title"
      className={cn("text-foreground font-semibold", className)}
      {...props}
    />
  );
}

/**
 * Un componente de descripción para la hoja deslizante.
 * Este componente se utiliza para mostrar una descripción en el encabezado de la hoja, debajo del título.
 * @param className
 * @param props
 * @constructor
 */
function SheetDescription({
  className,
  ...props
}: React.ComponentProps<typeof SheetPrimitive.Description>) {
  return (
    <SheetPrimitive.Description
      data-slot="sheet-description"
      className={cn("text-muted-foreground text-sm", className)}
      {...props}
    />
  );
}

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
};
