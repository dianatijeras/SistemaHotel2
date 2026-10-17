import * as React from "react";

import { cn } from "./utils";

/**
 * Textarea es un componente de área de texto que se utiliza para ingresar texto de varias líneas.
 * Es útil para comentarios, descripciones o cualquier otro tipo de entrada de texto que requiera más espacio que un campo de texto normal.
 * @param className
 * @param props
 * @constructor
 */
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "resize-none border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 flex field-sizing-content min-h-16 w-full rounded-md border bg-input-background px-3 py-2 text-base transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
