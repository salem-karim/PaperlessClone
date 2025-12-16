import {
  FaTag,
  FaFolder,
  FaBriefcase,
  FaHome,
  FaFileAlt,
  FaBook,
  FaHeart,
  FaStar,
  FaEnvelope,
  FaShoppingCart,
  FaCog,
  FaUser,
  FaUsers,
  FaCalendar,
  FaClock,
  FaCamera,
  FaMusic,
  FaGamepad,
  FaGraduationCap,
  FaMedkit,
  FaPlane,
  FaCar,
  FaUtensils,
  FaShieldAlt,
  FaMoneyBill,
} from "react-icons/fa";
import { IconType } from "react-icons";

export type IconName =
  | "tag"
  | "folder"
  | "briefcase"
  | "home"
  | "file"
  | "book"
  | "heart"
  | "star"
  | "envelope"
  | "shopping"
  | "settings"
  | "user"
  | "users"
  | "calendar"
  | "clock"
  | "camera"
  | "music"
  | "game"
  | "education"
  | "medical"
  | "travel"
  | "car"
  | "food"
  | "security"
  | "money";

export const iconOptions: { name: IconName; icon: IconType; label: string }[] =
  [
    { name: "tag", icon: FaTag, label: "Tag" },
    { name: "folder", icon: FaFolder, label: "Folder" },
    { name: "briefcase", icon: FaBriefcase, label: "Work" },
    { name: "home", icon: FaHome, label: "Home" },
    { name: "file", icon: FaFileAlt, label: "Document" },
    { name: "book", icon: FaBook, label: "Book" },
    { name: "heart", icon: FaHeart, label: "Favorite" },
    { name: "star", icon: FaStar, label: "Important" },
    { name: "envelope", icon: FaEnvelope, label: "Mail" },
    { name: "shopping", icon: FaShoppingCart, label: "Shopping" },
    { name: "settings", icon: FaCog, label: "Settings" },
    { name: "user", icon: FaUser, label: "Personal" },
    { name: "users", icon: FaUsers, label: "Team" },
    { name: "calendar", icon: FaCalendar, label: "Events" },
    { name: "clock", icon: FaClock, label: "Time" },
    { name: "camera", icon: FaCamera, label: "Photos" },
    { name: "music", icon: FaMusic, label: "Music" },
    { name: "game", icon: FaGamepad, label: "Gaming" },
    { name: "education", icon: FaGraduationCap, label: "Education" },
    { name: "medical", icon: FaMedkit, label: "Medical" },
    { name: "travel", icon: FaPlane, label: "Travel" },
    { name: "car", icon: FaCar, label: "Vehicle" },
    { name: "food", icon: FaUtensils, label: "Food" },
    { name: "security", icon: FaShieldAlt, label: "Security" },
    { name: "money", icon: FaMoneyBill, label: "Finance" },
  ];

export const iconMap: Record<IconName, IconType> = {
  tag: FaTag,
  folder: FaFolder,
  briefcase: FaBriefcase,
  home: FaHome,
  file: FaFileAlt,
  book: FaBook,
  heart: FaHeart,
  star: FaStar,
  envelope: FaEnvelope,
  shopping: FaShoppingCart,
  settings: FaCog,
  user: FaUser,
  users: FaUsers,
  calendar: FaCalendar,
  clock: FaClock,
  camera: FaCamera,
  music: FaMusic,
  game: FaGamepad,
  education: FaGraduationCap,
  medical: FaMedkit,
  travel: FaPlane,
  car: FaCar,
  food: FaUtensils,
  security: FaShieldAlt,
  money: FaMoneyBill,
};

export function getIcon(iconName: string): IconType {
  return iconMap[iconName as IconName] || FaTag;
}
