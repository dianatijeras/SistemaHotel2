import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { ChevronRight, MoreHorizontal } from "lucide-react";

import { cn } from "./utils";

/**
 * breadcrumb es un componente de navegación que muestra la ubicación actual del usuario dentro de una jerarquía de páginas.
 * Se compone de varios subcomponentes, como BreadcrumbList, BreadcrumbItem, BreadcrumbLink, BreadcrumbPage, BreadcrumbSeparator y BreadcrumbEllipsis, que se utilizan para construir la estructura del breadcrumb.
 * @param props
 * @constructor
 */
function Breadcrumb({ ...props }: React.ComponentProps<"nav">) {
    return <nav aria-label="breadcrumb" data-slot="breadcrumb" {...props} />;
}

/**
 * BreadcrumbList es un componente que representa la lista de elementos del breadcrumb.
 * Se utiliza para envolver los elementos individuales del breadcrumb, como BreadcrumbItem, BreadcrumbLink y BreadcrumbPage.
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbList({ className, ...props }: React.ComponentProps<"ol">) {
    return (
        <ol
            data-slot="breadcrumb-list"
            className={cn(
                "text-muted-foreground flex flex-wrap items-center gap-1.5 text-sm break-words sm:gap-2.5",
                className,
            )}
            {...props}
        />
    );
}

/**
 * BreadcrumbItem es un componente que representa un elemento individual del breadcrumb.
 * Se utiliza para envolver un enlace o una página dentro del breadcrumb.
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbItem({ className, ...props }: React.ComponentProps<"li">) {
    return (
        <li
            data-slot="breadcrumb-item"
            className={cn("inline-flex items-center gap-1.5", className)}
            {...props}
        />
    );
}

/**
 * BreadcrumbLink es un componente que representa un enlace dentro del breadcrumb.
 * Se utiliza para navegar a una página anterior en la jerarquía del breadcrumb.
 * @param asChild
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbLink({
                            asChild,
                            className,
                            ...props
                        }: React.ComponentProps<"a"> & {
    asChild?: boolean;
}) {
    const Comp = asChild ? Slot : "a";

    return (
        <Comp
            data-slot="breadcrumb-link"
            className={cn("hover:text-foreground transition-colors", className)}
            {...props}
        />
    );
}

/**
 * BreadcrumbPage es un componente que representa la página actual dentro del breadcrumb.
 * Se utiliza para indicar la ubicación actual del usuario dentro de la jerarquía del breadcrumb.
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbPage({ className, ...props }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="breadcrumb-page"
            role="link"
            aria-disabled="true"
            aria-current="page"
            className={cn("text-foreground font-normal", className)}
            {...props}
        />
    );
}

/**
 * BreadcrumbSeparator es un componente que representa el separador entre los elementos del breadcrumb.
 * Se utiliza para separar visualmente los elementos del breadcrumb, como los enlaces y la página actual.
 * @param children
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbSeparator({
                                 children,
                                 className,
                                 ...props
                             }: React.ComponentProps<"li">) {
    return (
        <li
            data-slot="breadcrumb-separator"
            role="presentation"
            aria-hidden="true"
            className={cn("[&>svg]:size-3.5", className)}
            {...props}
        >
            {children ?? <ChevronRight />}
        </li>
    );
}

/**
 * BreadcrumbEllipsis es un componente que representa una elipsis dentro del breadcrumb.
 * Se utiliza para indicar que hay más elementos en la jerarquía del breadcrumb que no se muestran.
 * @param className
 * @param props
 * @constructor
 */
function BreadcrumbEllipsis({
                                className,
                                ...props
                            }: React.ComponentProps<"span">) {
    return (
        <span
            data-slot="breadcrumb-ellipsis"
            role="presentation"
            aria-hidden="true"
            className={cn("flex size-9 items-center justify-center", className)}
            {...props}
        >
      <MoreHorizontal className="size-4" />
      <span className="sr-only">More</span>
    </span>
    );
}

export {
    Breadcrumb,
    BreadcrumbList,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbPage,
    BreadcrumbSeparator,
    BreadcrumbEllipsis,
};
