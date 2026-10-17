"use client";

import * as AspectRatioPrimitive from "@radix-ui/react-aspect-ratio";

/**
 * Componente AspectRatio para mantener una relación de aspecto específica en su contenido.
 * @param props
 * @constructor
 */
function AspectRatio({
                         ...props
                     }: React.ComponentProps<typeof AspectRatioPrimitive.Root>) {
    return <AspectRatioPrimitive.Root data-slot="aspect-ratio" {...props} />;
}

export { AspectRatio };