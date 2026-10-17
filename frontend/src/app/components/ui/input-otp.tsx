"use client";

import * as React from "react";
import { OTPInput, OTPInputContext } from "input-otp";
import { MinusIcon } from "lucide-react";

import { cn } from "./utils";

/**
 * InputOTP es un componente de entrada de código OTP (One-Time Password) que utiliza la biblioteca "input-otp".
 * Permite a los usuarios ingresar códigos OTP de manera fácil y estilizada.
 * El componente se compone de varios subcomponentes, como InputOTPGroup, InputOTPSlot e InputOTPSeparator, que se utilizan para organizar y mostrar los campos de entrada de OTP.
 * El componente también maneja el estado de los campos de entrada, como el carácter ingresado, si el campo está activo y si tiene un cursor falso para indicar la posición actual del usuario.
 * @param className
 * @param containerClassName
 * @param props
 * @constructor
 */
function InputOTP({
                      className,
                      containerClassName,
                      ...props
                  }: React.ComponentProps<typeof OTPInput> & {
    containerClassName?: string;
}) {
    return (
        <OTPInput
            data-slot="input-otp"
            containerClassName={cn(
                "flex items-center gap-2 has-disabled:opacity-50",
                containerClassName,
            )}
            className={cn("disabled:cursor-not-allowed", className)}
            {...props}
        />
    );
}

/**
 * InputOTPGroup es un componente que se utiliza para agrupar los campos de entrada de OTP.
 * Proporciona un contenedor flexible para organizar los campos de entrada de OTP y aplicar estilos comunes a todos ellos.
 * @param className
 * @param props
 * @constructor
 */
function InputOTPGroup({ className, ...props }: React.ComponentProps<"div">) {
    return (
        <div
            data-slot="input-otp-group"
            className={cn("flex items-center gap-1", className)}
            {...props}
        />
    );
}

/**
 * InputOTPSlot es un componente que representa un campo de entrada individual para el código OTP.
 * Cada slot muestra el carácter ingresado por el usuario, si el campo está activo y si tiene un cursor falso para indicar la posición actual del usuario.
 * El componente utiliza el contexto de OTPInput para obtener la información relevante sobre el estado del campo de entrada.
 * @param index
 * @param className
 * @param props
 * @constructor
 */
function InputOTPSlot({
                          index,
                          className,
                          ...props
                      }: React.ComponentProps<"div"> & {
    index: number;
}) {
    const inputOTPContext = React.useContext(OTPInputContext);
    const { char, hasFakeCaret, isActive } = inputOTPContext?.slots[index] ?? {};

    return (
        <div
            data-slot="input-otp-slot"
            data-active={isActive}
            className={cn(
                "data-[active=true]:border-ring data-[active=true]:ring-ring/50 data-[active=true]:aria-invalid:ring-destructive/20 dark:data-[active=true]:aria-invalid:ring-destructive/40 aria-invalid:border-destructive data-[active=true]:aria-invalid:border-destructive dark:bg-input/30 border-input relative flex h-9 w-9 items-center justify-center border-y border-r text-sm bg-input-background transition-all outline-none first:rounded-l-md first:border-l last:rounded-r-md data-[active=true]:z-10 data-[active=true]:ring-[3px]",
                className,
            )}
            {...props}
        >
            {char}
            {hasFakeCaret && (
                <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                    <div className="animate-caret-blink bg-foreground h-4 w-px duration-1000" />
                </div>
            )}
        </div>
    );
}

/**
 * InputOTPSeparator es un componente que se utiliza para mostrar un separador entre los campos de entrada de OTP.
 * En este caso, se utiliza un ícono de guion (MinusIcon) para indicar visualmente la separación entre los campos de entrada de OTP.
 * @param props
 * @constructor
 */
function InputOTPSeparator({ ...props }: React.ComponentProps<"div">) {
    return (
        <div data-slot="input-otp-separator" role="separator" {...props}>
            <MinusIcon />
        </div>
    );
}

export { InputOTP, InputOTPGroup, InputOTPSlot, InputOTPSeparator };
