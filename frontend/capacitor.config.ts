import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.expensesagenticai.app',
  appName: 'Expense AI',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
};

export default config;
