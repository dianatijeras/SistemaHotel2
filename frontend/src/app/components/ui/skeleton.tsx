import { cn } from "./utils";

/**
 * Skeleton es un componente de carga que muestra un bloque animado mientras se cargan los datos reales.
 * Es útil para mejorar la experiencia del usuario al indicar que el contenido está en proceso de carga.
 * @param className
 * @param props
 * @constructor
 */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("bg-accent animate-pulse rounded-md", className)}
      {...props}
    />
  );
}

export { Skeleton };
