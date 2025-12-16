import { getIcon } from "./iconConstants";

export function CategoryIcon({
  iconName,
  className,
}: {
  iconName: string;
  className?: string;
}) {
  const Icon = getIcon(iconName);
  return <Icon className={className} />;
}
