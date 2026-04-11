// Startup script to perform health checks and configuration validation
// This runs during the app initialization process

import { performStartupHealthCheck } from '@/lib/healthCheck';

// Configuration validation
function validateConfiguration(): void {
  console.log('🔧 Validating application configuration...');
  
  const requiredEnvVars = [
    'NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY',
    'CLERK_SECRET_KEY',
    'NEXT_PUBLIC_API_BASE_URL'
  ];
  
  const missingVars: string[] = [];
  
  for (const envVar of requiredEnvVars) {
    if (!process.env[envVar]) {
      missingVars.push(envVar);
    }
  }
  
  if (missingVars.length > 0) {
    console.log('❌ Missing required environment variables:');
    missingVars.forEach(varName => {
      console.log(`   - ${varName}`);
    });
    console.log('⚠️ Application may not function correctly');
  } else {
    console.log('✅ All required environment variables are set');
  }
  
  // Log configuration details (masked for security)
  console.log('📋 Configuration Summary:');
  console.log('==========================');
  console.log(`   NODE_ENV: ${process.env.NODE_ENV || 'not set'}`);
  console.log(`   NEXT_PUBLIC_API_BASE_URL: ${process.env.NEXT_PUBLIC_API_BASE_URL || 'not set'}`);
  console.log(`   NEXT_PUBLIC_DOCS_URL: ${process.env.NEXT_PUBLIC_DOCS_URL || 'not set'}`);
  console.log(`   NEXT_PUBLIC_DASHBOARD_URL: ${process.env.NEXT_PUBLIC_DASHBOARD_URL || 'not set'}`);
  console.log(`   NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY: ${process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY ? 
    `${process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY.substring(0, 20)}...` : 'not set'}`);
  console.log(`   CLERK_SECRET_KEY: ${process.env.CLERK_SECRET_KEY ? 
    `${process.env.CLERK_SECRET_KEY.substring(0, 20)}...` : 'not set'}`);
  console.log('==========================');
}

// Main startup function
export async function performStartupChecks(): Promise<void> {
  console.log('🚀 Starting API Management UI initialization...');
  console.log('===============================================');
  
  // Validate configuration
  validateConfiguration();
  
  // Perform health check
  await performStartupHealthCheck();
  
  console.log('✅ Startup checks completed');
  console.log('===============================================');
}

// Auto-run if this is the main module (for testing)
if (require.main === module) {
  performStartupChecks().catch(console.error);
}
