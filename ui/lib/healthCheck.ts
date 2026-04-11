// Health check utility for backend connectivity
// This helps verify NEXT_PUBLIC_API_BASE_URL is configured correctly

export interface HealthCheckResult {
  success: boolean;
  url: string;
  status?: number;
  error?: string;
  responseTime?: number;
}

export async function checkBackendHealth(): Promise<HealthCheckResult> {
  const startTime = Date.now();
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  
  if (!baseUrl) {
    return {
      success: false,
      url: 'N/A',
      error: 'NEXT_PUBLIC_API_BASE_URL environment variable is not set'
    };
  }

  const healthUrl = `${baseUrl}/actuator/health`;
  
  try {
    console.log(`🏥 Checking backend health at: ${healthUrl}`);
    
    const response = await fetch(healthUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'API-Management-UI-HealthCheck/1.0'
      },
      // Add timeout to prevent hanging
      signal: AbortSignal.timeout(10000) // 10 second timeout
    });
    
    const responseTime = Date.now() - startTime;
    
    if (response.ok) {
      const healthData = await response.json().catch(() => ({}));
      console.log(`✅ Backend health check successful (${response.status}) - Response time: ${responseTime}ms`);
      console.log(`📊 Health data:`, JSON.stringify(healthData, null, 2));
      
      return {
        success: true,
        url: healthUrl,
        status: response.status,
        responseTime
      };
    } else {
      console.log(`⚠️ Backend health check returned ${response.status} - Response time: ${responseTime}ms`);
      return {
        success: false,
        url: healthUrl,
        status: response.status,
        error: `HTTP ${response.status}: ${response.statusText}`,
        responseTime
      };
    }
  } catch (error) {
    const responseTime = Date.now() - startTime;
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    
    console.log(`❌ Backend health check failed - Response time: ${responseTime}ms`);
    console.log(`🚨 Error details:`, errorMessage);
    
    return {
      success: false,
      url: healthUrl,
      error: errorMessage,
      responseTime
    };
  }
}

// Function to perform health check and log results
export async function performStartupHealthCheck(): Promise<void> {
  console.log('🔍 Performing startup health check...');
  console.log('=====================================');
  
  const result = await checkBackendHealth();
  
  console.log('=====================================');
  if (result.success) {
    console.log(`✅ Startup health check PASSED`);
    console.log(`🌐 Backend URL: ${result.url}`);
    console.log(`⚡ Response time: ${result.responseTime}ms`);
  } else {
    console.log(`❌ Startup health check FAILED`);
    console.log(`🌐 Attempted URL: ${result.url}`);
    console.log(`🚨 Error: ${result.error}`);
    console.log(`⚡ Response time: ${result.responseTime}ms`);
    
    // Don't exit the process, just log the error
    // This allows the app to start even if backend is temporarily unavailable
    console.log(`⚠️ Continuing startup despite health check failure...`);
  }
  console.log('=====================================');
}
