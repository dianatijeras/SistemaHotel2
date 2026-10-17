import { ReactNode } from "react";

interface CardProps {
    children: ReactNode;
    className?: string;
}

/**
 * Componete de tarjeta reutilizable que se puede usar para envolver contenido con un estilo consistente.
 * @param children
 * @param className
 * @constructor
 */
export function Card({ children, className = '' }: CardProps) {
    return (
        <div className={`bg-white border border-gray-200 rounded-lg p-6 ${className}`}>
            {children}
        </div>
    );
}