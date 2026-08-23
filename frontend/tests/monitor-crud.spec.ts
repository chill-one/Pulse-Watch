import { expect, test } from "@playwright/test";

test("can create, edit, and delete a monitor", async ({ page }) => {

  const uniqueId = Date.now();

  const originalName = `Playwright Monitor ${uniqueId}`;
  const editedName =
    `Playwright Monitor Updated ${uniqueId}`;

  let detailUrl: string | undefined;
  let monitorDeleted = false;

  try {
    await page.goto("/");

    await page
      .getByRole("link", {
        name: "Add Monitor",
        exact: true,
      })
      .click();

    await expect(page).toHaveURL(
      /\/monitors\/new$/
    );

    await page
      .getByLabel("Name")
      .fill(originalName);

    await page
      .getByLabel("URL")
      .fill("https://example.com");

    await page
      .getByLabel("Check interval (seconds)")
      .fill("30");

    await page
      .getByLabel("Timeout (seconds)")
      .fill("5");

    // Wait until React hydration has completed
    // and the form is safe to submit.
    const createButton = page.getByRole("button", {
      name: "Create Monitor",
      exact: true,
    });

    await expect(createButton).toBeEnabled();

    await createButton.click();

    // Successful creation redirects to dashboard.
    await expect(page).toHaveURL(/\/$/);

    const createdMonitorLink = page.getByRole("link", {
      name: originalName,
      exact: true,
    });

    await expect(
      createdMonitorLink
    ).toBeVisible();

    // Save this URL so the finally block can clean up
    // the disposable monitor if the test fails later.
    detailUrl =
      (await createdMonitorLink.getAttribute("href")) ??
      undefined;

    await createdMonitorLink.click();

    await expect(page).toHaveURL(
      /\/monitors\/[^/]+$/
    );

    await expect(
      page.getByRole("heading", {
        name: originalName,
        exact: true,
      })
    ).toBeVisible();

    await page
      .getByRole("link", {
        name: "Edit Monitor",
        exact: true,
      })
      .click();

    await expect(page).toHaveURL(
      /\/monitors\/[^/]+\/edit$/
    );

    // Verify edit form is populated with
    // the existing monitor configuration.
    await expect(
      page.getByLabel("Name")
    ).toHaveValue(originalName);

    await expect(
      page.getByLabel("URL")
    ).toHaveValue("https://example.com");

    await expect(
      page.getByLabel("Check interval (seconds)")
    ).toHaveValue("30");

    await expect(
      page.getByLabel("Timeout (seconds)")
    ).toHaveValue("5");

    // Change only the name.
    await page
      .getByLabel("Name")
      .fill(editedName);

    // Wait for edit form hydration before submitting.
    const saveButton = page.getByRole("button", {
      name: "Save Changes",
      exact: true,
    });

    await expect(saveButton).toBeEnabled();

    await saveButton.click();

    await expect(page).toHaveURL(
      /\/monitors\/[^/]+$/
    );

    await expect(
      page.getByRole("heading", {
        name: editedName,
        exact: true,
      })
    ).toBeVisible();

    await page
      .getByRole("link", {
        name: /Back to dashboard/,
      })
      .click();

    await expect(page).toHaveURL(/\/$/);

    const editedMonitorLink = page.getByRole("link", {
      name: editedName,
      exact: true,
    });

    await expect(
      editedMonitorLink
    ).toBeVisible();

    // Open the monitor again before deleting it.
    await editedMonitorLink.click();

    await expect(page).toHaveURL(
      /\/monitors\/[^/]+$/
    );

    await expect(
      page.getByRole("heading", {
        name: editedName,
        exact: true,
      })
    ).toBeVisible();

    // The application uses window.confirm().
    // Register this BEFORE clicking Delete Monitor.
    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");

      await dialog.accept();
    });

    await page
      .getByRole("button", {
        name: "Delete Monitor",
        exact: true,
      })
      .click();

    await expect(page).toHaveURL(/\/$/);

    monitorDeleted = true;

    // Make sure the deleted monitor no longer appears.
    await expect(
      page.getByRole("link", {
        name: editedName,
        exact: true,
      })
    ).not.toBeVisible();
  } finally {
    /*
     * If the test failed after monitor creation but
     * before normal deletion, try to remove the
     * disposable monitor so repeated test runs don't
     * leave garbage data behind.
     */
    if (!monitorDeleted && detailUrl) {
      try {
        await page.goto(detailUrl);

        const deleteButton = page.getByRole("button", {
          name: "Delete Monitor",
          exact: true,
        });

        if (await deleteButton.isVisible()) {
          page.once("dialog", async (dialog) => {
            if (dialog.type() === "confirm") {
              await dialog.accept();
            } else {
              await dialog.dismiss();
            }
          });

          await deleteButton.click();

          await expect(page).toHaveURL(/\/$/);
        }
      } catch {
        // Cleanup is best effort and must not
        // hide the original test failure.
      }
    }
  }
});