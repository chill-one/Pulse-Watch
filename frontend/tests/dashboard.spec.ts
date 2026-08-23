import { test, expect } from "@playwright/test";

test("dashboard loads", async ({ page }) => {
  await page.goto("/");

  await expect(
    page.getByRole("heading", {
      name: "PulseWatch",
    })
  ).toBeVisible();

  await expect(
    page.getByRole("heading", {
      name: "Monitors",
    })
  ).toBeVisible();

  await expect(
    page.getByRole("link", {
      name: "Add Monitor",
    })
  ).toBeVisible();
});