import { type FormEvent } from "react";

export function SearchBar({
  value,
  onChange,
  onSubmit,
}: {
  value: string;
  onChange: (v: string) => void;
  onSubmit?: () => void;
}) {
  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit?.();
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 w-full sm:w-auto">
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Search documents…"
        className="border rounded-md px-3 py-1 text-sm flex-1 sm:w-64 dark:bg-gray-700 dark:text-white dark:border-gray-600"
      />

      <button
        type="submit"
        className="rounded-md bg-blue-600 text-white px-4 py-1 text-sm hover:bg-blue-700 transition"
      >
        Search
      </button>
    </form>
  );
}
