import { describe, it, expect, vi } from "vitest";
import { render, screen } from "../test/test-utils";
import userEvent from "@testing-library/user-event";
import { DeleteModal } from "./DeleteModal";

describe("DeleteModal", () => {
  it("renders confirmation message and buttons", () => {
    render(<DeleteModal onConfirm={() => {}} onCancel={() => {}} />);

    expect(
      screen.getByText("Are you sure you want to delete this document?"),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("calls onConfirm when Delete button is clicked", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();

    render(<DeleteModal onConfirm={onConfirm} onCancel={() => {}} />);

    await user.click(screen.getByRole("button", { name: "Delete" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("calls onCancel when Cancel button is clicked", async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();

    render(<DeleteModal onConfirm={() => {}} onCancel={onCancel} />);

    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

