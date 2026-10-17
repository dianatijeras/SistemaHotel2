"use client";

import * as React from "react";
import * as TabsPrimitive from "@radix-ui/react-tabs";

import { cn } from "./utils";

/**
 * Tabs es un componente de interfaz de usuario que permite organizar contenido en secciones o pestañas.
 * Cada pestaña representa una sección diferente del contenido, y al hacer clic en una pestaña, se muestra el contenido asociado a esa pestaña.
 * Este componente es útil para mejorar la navegación y la organización de la información en una aplicación o sitio web.
 * @param className
 * @param props
 * @constructor
 */
function Tabs({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Root>) {
  return (
    <TabsPrimitive.Root
      data-slot="tabs"
      className={cn("flex flex-col gap-2", className)}
      {...props}
    />
  );
}

/**
 * TabsList es un componente que se utiliza dentro de Tabs para contener las pestañas o triggers.
 * Es el contenedor que agrupa las pestañas y les proporciona un estilo y una estructura visual.
 * Generalmente, se utiliza para organizar las pestañas en una fila o columna, dependiendo del diseño de la interfaz de usuario.
 * @param className
 * @param props
 * @constructor
 */
function TabsList({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.List>) {
  return (
    <TabsPrimitive.List
      data-slot="tabs-list"
      className={cn(
        "bg-muted text-muted-foreground inline-flex h-9 w-fit items-center justify-center rounded-xl p-[3px] flex",
        className,
      )}
      {...props}
    />
  );
}

/**
 * TabsTrigger es un componente que representa una pestaña o trigger dentro de TabsList.
 * Es el elemento interactivo que el usuario puede hacer clic para cambiar entre las diferentes secciones de contenido.
 * Cada TabsTrigger está asociado a un contenido específico, y al hacer clic en él, se muestra el contenido correspondiente a esa pestaña.
 * @param className
 * @param props
 * @constructor
 */
function TabsTrigger({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return (
    <TabsPrimitive.Trigger
      data-slot="tabs-trigger"
      className={cn(
        "data-[state=active]:bg-card dark:data-[state=active]:text-foreground focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:outline-ring dark:data-[state=active]:border-input dark:data-[state=active]:bg-input/30 text-foreground dark:text-muted-foreground inline-flex h-[calc(100%-1px)] flex-1 items-center justify-center gap-1.5 rounded-xl border border-transparent px-2 py-1 text-sm font-medium whitespace-nowrap transition-[color,box-shadow] focus-visible:ring-[3px] focus-visible:outline-1 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
        className,
      )}
      {...props}
    />
  );
}

/**
 * TabsContent es un componente que representa el contenido asociado a una pestaña o trigger específico dentro de Tabs.
 * Es el área donde se muestra la información o el contenido relacionado con la pestaña seleccionada.
 * Cada TabsContent está vinculado a un TabsTrigger mediante un identificador, y solo se muestra el contenido correspondiente a la pestaña activa.
 * @param className
 * @param props
 * @constructor
 */
function TabsContent({
  className,
  ...props
}: React.ComponentProps<typeof TabsPrimitive.Content>) {
  return (
    <TabsPrimitive.Content
      data-slot="tabs-content"
      className={cn("flex-1 outline-none", className)}
      {...props}
    />
  );
}

export { Tabs, TabsList, TabsTrigger, TabsContent };
