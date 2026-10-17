"use client";

import * as React from "react";
import * as RadioGroupPrimitive from "@radix-ui/react-radio-group";
import { CircleIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * RadioGroup es un componente de React que utiliza la biblioteca Radix UI para crear un grupo de botones de opción (radio buttons).
 * Este componente permite a los usuarios seleccionar una opción de un conjunto de opciones mutuamente exclusivas.
 * El componente se compone de dos partes principales: RadioGroup, que es el contenedor del grupo de botones, y RadioGroupItem, que representa cada botón individual dentro del grupo.
 * El diseño y estilo de los botones se personalizan utilizando clases CSS y el icono CircleIcon para indicar la selección.
 * @param className
 * @param props
 * @constructor
 */
function RadioGroup({
  className,
  ...props
}: React.ComponentProps<typeof RadioGroupPrimitive.Root>) {
  return (
    <RadioGroupPrimitive.Root
      data-slot="radio-group"
      className={cn("grid gap-3", className)}
      {...props}
    />
  );
}

/**
 * RadioGroupItem es un componente que representa un botón de opción individual dentro del grupo de botones de opción (RadioGroup).
 * Este componente utiliza la biblioteca Radix UI para crear un botón de opción estilizado.
 * El botón se muestra como un círculo que puede ser seleccionado por el usuario.
 * El diseño del botón se personaliza utilizando clases CSS, y el icono CircleIcon se utiliza para indicar visualmente cuándo un botón está seleccionado.
 * @param className
 * @param props
 * @constructor
 */
function RadioGroupItem({
  className,
  ...props
}: React.ComponentProps<typeof RadioGroupPrimitive.Item>) {
  return (
    <RadioGroupPrimitive.Item
      data-slot="radio-group-item"
      className={cn(
        "border-input text-primary focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 aspect-square size-4 shrink-0 rounded-full border shadow-xs transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...props}
    >
      <RadioGroupPrimitive.Indicator
        data-slot="radio-group-indicator"
        className="relative flex items-center justify-center"
      >
        <CircleIcon className="fill-primary absolute top-1/2 left-1/2 size-2 -translate-x-1/2 -translate-y-1/2" />
      </RadioGroupPrimitive.Indicator>
    </RadioGroupPrimitive.Item>
  );
}

export { RadioGroup, RadioGroupItem };
