import { defineConfig, devices } from '@playwright/test';

const GATEWAY_URL = process.env.GATEWAY_URL || 'https://localhost:9527';
const IS_CI = !!process.env.CI;

export default defineConfig({
  testDir: './tests',
  fullyParallel: !IS_CI,
  forbidOnly: IS_CI,
  retries: IS_CI ? 2 : 0,
  workers: IS_CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],
  use: {
    baseURL: GATEWAY_URL,
    trace: 'on-first-retry',
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 30000,
    navigationTimeout: 60000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  outputDir: 'test-results',
});
