import { type ReactElement } from "react";
import {
  render as rtlRender,
  type RenderOptions,
} from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";

function customRender(
  ui: ReactElement,
  options?: Omit<RenderOptions, "wrapper">,
) {
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>{children}</BrowserRouter>
  );

  return rtlRender(ui, { wrapper: Wrapper, ...options });
}

// Export only the render function and test utilities
export { customRender as render };
export { screen, waitFor, within, fireEvent } from "@testing-library/react";

