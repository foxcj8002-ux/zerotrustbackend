import { test as base, Page, BrowserContext } from '@playwright/test';
import { generateTestJwt, TEST_USERS } from './fixtures';

export interface AuthenticatedPage {
  page: Page;
  validJwt: string;
  context: BrowserContext;
}

export async function createAuthenticatedContext(
  baseContext: BrowserContext,
  userKey: keyof typeof TEST_USERS = 'user001'
): Promise<{ context: BrowserContext; jwt: string }> {
  const user = TEST_USERS[userKey];
  const jwt = await generateTestJwt({
    sub: user.userId,
    resourceId: user.resourceId,
  });

  const context = await baseContext.newContext();
  await context.addCookies([{
    name: 'access_token',
    value: jwt,
    domain: 'localhost',
    path: '/',
    secure: true,
    httpOnly: false,
  }]);

  return { context, jwt };
}

export const test = base.extend<{ authenticatedPage: AuthenticatedPage }>({
  authenticatedPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
    });

    const user = TEST_USERS.user001;
    const jwt = await generateTestJwt({
      sub: user.userId,
      resourceId: user.resourceId,
    });

    await context.addCookies([{
      name: 'access_token',
      value: jwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    const page = await context.newPage();

    await use({ page, validJwt: jwt, context });

    await context.close();
  },
});

export { expect } from '@playwright/test';
