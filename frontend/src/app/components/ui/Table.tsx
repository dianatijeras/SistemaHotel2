import { ReactNode } from "react";
import { Search } from "lucide-react";

/**
 * Una tabla genérica que puede ser utilizada para mostrar cualquier tipo de datos.
 */
interface Column<T> {
    key: string;
    label: string;
    render?: (item: T) => ReactNode;
}

/**
 * Props para el componente Table.
 * @template T El tipo de datos que se mostrará en la tabla. Debe tener una propiedad `id` de tipo number o string.
 */
interface TableProps<T> {
    data: T[];
    columns: Column<T>[];
    onSearch?: (query: string) => void;
    searchPlaceholder?: string;
    actions?: (item: T) => ReactNode;
}

/**
 * Componente de tabla genérica que muestra datos en formato tabular, con soporte para búsqueda y acciones personalizadas.
 * @template T El tipo de datos que se mostrará en la tabla. Debe tener una propiedad `id` de tipo number o string.
 * @param data
 * @param columns
 * @param onSearch
 * @param searchPlaceholder
 * @param actions
 * @constructor
 */
export function Table<T extends { id: number | string }>({
                                                             data,
                                                             columns,
                                                             onSearch,
                                                             searchPlaceholder = "Buscar...",
                                                             actions,
                                                         }: TableProps<T>) {
    return (
        <div className="space-y-4">
            {onSearch && (
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder={searchPlaceholder}
                        onChange={(e) => onSearch(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                </div>
            )}

            {/* `overflow-x-auto` en lugar de `overflow-hidden`: con `hidden` la columna de acciones
                quedaba recortada y sin forma de desplazarse en pantallas estrechas, de modo que
                confirmar, cancelar, editar o eliminar era imposible desde la interfaz. */}
            <div className="bg-white border border-gray-200 rounded-lg overflow-x-auto">
                <table className="w-full">
                    <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                        {columns.map((column) => (
                            <th
                                key={column.key}
                                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                            >
                                {column.label}
                            </th>
                        ))}
                        {actions && (
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Acciones
                            </th>
                        )}
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                    {data.length === 0 ? (
                        <tr>
                            <td
                                colSpan={columns.length + (actions ? 1 : 0)}
                                className="px-6 py-8 text-center text-gray-500"
                            >
                                No hay datos para mostrar
                            </td>
                        </tr>
                    ) : (
                        data.map((item) => (
                            <tr key={item.id} className="hover:bg-gray-50">
                                {columns.map((column) => (
                                    <td key={column.key} className="px-6 py-4 text-sm text-gray-900">
                                        {column.render
                                            ? column.render(item)
                                            : String(item[column.key as keyof T])}
                                    </td>
                                ))}
                                {actions && (
                                    <td className="px-6 py-4 text-sm">
                                        <div className="flex gap-2">{actions(item)}</div>
                                    </td>
                                )}
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
