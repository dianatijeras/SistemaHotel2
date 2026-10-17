import * as React from "react";

const MOBILE_BREAKPOINT = 768;

/**
 * un hook para detectar si el dispositivo es móvil o no, basado en el ancho de la ventana.
 * Devuelve `true` si el ancho de la ventana es menor que el punto de quiebre móvil, y `false` en caso contrario.
 * El valor inicial es `undefined` hasta que se monta el componente y se evalúa el ancho de la ventana.
 */
export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState<boolean | undefined>(
    undefined,
  );

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    };
    mql.addEventListener("change", onChange);
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    return () => mql.removeEventListener("change", onChange);
  }, []);

  return !!isMobile;
}
