# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: monitor-crud.spec.ts >> can create, edit, and delete a monitor
- Location: tests/monitor-crud.spec.ts:3:1

# Error details

```
Error: expect(page).toHaveURL(expected) failed

Expected pattern: /\/$/
Received string:  "http://127.0.0.1:3000/monitors/new?"
Timeout: 5000ms

Call log:
  - Expect "toHaveURL" with timeout 5000ms
    14 × locator resolved to <html lang="en">…</html>
       - unexpected value "http://127.0.0.1:3000/monitors/new?"

```

```yaml
- main:
  - link "← Back to dashboard":
    - /url: /
  - heading "Add Monitor" [level=1]
  - paragraph: Add a website or service for PulseWatch to monitor.
  - text: Name
  - textbox "Name":
    - /placeholder: GitHub Production
  - text: URL
  - textbox "URL Include http:// or https://":
    - /placeholder: https://example.com
  - text: Include http:// or https:// Check interval (seconds)
  - spinbutton "Check interval (seconds)": "60"
  - text: Timeout (seconds)
  - spinbutton "Timeout (seconds)": "5"
  - button "Create Monitor"
```

# Test source

```ts
  1  | import { expect, test } from "@playwright/test";
  2  | 
  3  | test("can create, edit, and delete a monitor", async ({ page }) => {
  4  |   const uniqueId = Date.now();
  5  |   const originalName = `Playwright Monitor ${uniqueId}`;
  6  |   const editedName = `Playwright Monitor Updated ${uniqueId}`;
  7  | 
  8  |   let detailUrl: string | undefined;
  9  |   let deleted = false;
  10 | 
  11 |   try {
  12 |     await page.goto("/");
  13 | 
  14 |     await page.getByRole("link", { name: "Add Monitor", exact: true }).click();
  15 |     await expect(page).toHaveURL(/\/monitors\/new$/);
  16 | 
  17 |     await page.getByLabel("Name").fill(originalName);
  18 |     await page.getByLabel("URL").fill("https://example.com");
  19 |     await page.getByLabel("Check interval (seconds)").fill("30");
  20 |     await page.getByLabel("Timeout (seconds)").fill("5");
  21 | 
  22 |     await page.getByRole("button", { name: "Create Monitor", exact: true }).click();
> 23 |     await expect(page).toHaveURL(/\/$/);
     |                        ^ Error: expect(page).toHaveURL(expected) failed
  24 | 
  25 |     const createdMonitorLink = page.getByRole("link", {
  26 |       name: originalName,
  27 |       exact: true,
  28 |     });
  29 |     await expect(createdMonitorLink).toBeVisible();
  30 | 
  31 |     detailUrl = (await createdMonitorLink.getAttribute("href")) ?? undefined;
  32 |     await createdMonitorLink.click();
  33 |     await expect(page).toHaveURL(/\/monitors\/[^/]+$/);
  34 |     await expect(
  35 |       page.getByRole("heading", { name: originalName, exact: true })
  36 |     ).toBeVisible();
  37 | 
  38 |     await page.getByRole("link", { name: "Edit Monitor", exact: true }).click();
  39 |     await expect(page).toHaveURL(/\/monitors\/[^/]+\/edit$/);
  40 |     await expect(page.getByLabel("Name")).toHaveValue(originalName);
  41 |     await expect(page.getByLabel("URL")).toHaveValue("https://example.com");
  42 |     await expect(page.getByLabel("Check interval (seconds)")).toHaveValue("30");
  43 |     await expect(page.getByLabel("Timeout (seconds)")).toHaveValue("5");
  44 | 
  45 |     await page.getByLabel("Name").fill(editedName);
  46 |     await page.getByRole("button", { name: "Save Changes", exact: true }).click();
  47 |     await expect(page).toHaveURL(/\/monitors\/[^/]+$/);
  48 |     await expect(
  49 |       page.getByRole("heading", { name: editedName, exact: true })
  50 |     ).toBeVisible();
  51 | 
  52 |     await page.getByRole("link", { name: /Back to dashboard/ }).click();
  53 |     await expect(page).toHaveURL(/\/$/);
  54 | 
  55 |     const editedMonitorLink = page.getByRole("link", {
  56 |       name: editedName,
  57 |       exact: true,
  58 |     });
  59 |     await expect(editedMonitorLink).toBeVisible();
  60 |     await editedMonitorLink.click();
  61 |     await expect(page).toHaveURL(/\/monitors\/[^/]+$/);
  62 | 
  63 |     page.once("dialog", (dialog) => dialog.accept());
  64 |     await page.getByRole("button", { name: "Delete Monitor", exact: true }).click();
  65 |     await expect(page).toHaveURL(/\/$/);
  66 |     deleted = true;
  67 | 
  68 |     await expect(
  69 |       page.getByRole("link", { name: editedName, exact: true })
  70 |     ).not.toBeVisible();
  71 |   } finally {
  72 |     if (!deleted && detailUrl) {
  73 |       try {
  74 |         await page.goto(detailUrl);
  75 |         const deleteButton = page.getByRole("button", {
  76 |           name: "Delete Monitor",
  77 |           exact: true,
  78 |         });
  79 | 
  80 |         if (await deleteButton.isVisible()) {
  81 |           page.once("dialog", (dialog) => dialog.accept());
  82 |           await deleteButton.click();
  83 |           await expect(page).toHaveURL(/\/$/);
  84 |         }
  85 |       } catch {
  86 |         // Cleanup is best effort so the original assertion failure is preserved.
  87 |       }
  88 |     }
  89 |   }
  90 | });
  91 | 
```