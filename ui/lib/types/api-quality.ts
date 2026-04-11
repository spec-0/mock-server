/**
 * API Quality Management Types
 * 
 * This file contains all types related to SLOs, SLAs, load testing, and linting
 * for API quality management and monitoring.
 */

// SLO (Service Level Objective) Types
export interface SLO {
  id: string;
  apiId: string;
  name: string;
  description: string;
  metric: SLOMetric;
  target: number; // Target percentage (e.g., 99.9 for 99.9%)
  measurementWindow: '1h' | '24h' | '7d' | '30d';
  status: 'healthy' | 'warning' | 'critical' | 'unknown';
  currentValue?: number;
  lastUpdated: string;
  createdAt: string;
  createdBy: string;
}

export type SLOMetric = 
  | 'availability' 
  | 'response_time_p95' 
  | 'response_time_p99' 
  | 'error_rate' 
  | 'throughput'
  | 'latency_p50'
  | 'latency_p95'
  | 'latency_p99';

// SLA (Service Level Agreement) Types
export interface SLA {
  id: string;
  apiId: string;
  name: string;
  description: string;
  terms: SLATerm[];
  status: 'active' | 'draft' | 'expired';
  effectiveDate: string;
  expiryDate?: string;
  createdAt: string;
  createdBy: string;
}

export interface SLATerm {
  id: string;
  metric: SLOMetric;
  target: number;
  measurementWindow: '1h' | '24h' | '7d' | '30d';
  penalty?: string; // Description of penalty if SLA is breached
  reward?: string; // Description of reward if SLA is met
}

// Load Test Results Types
export interface LoadTestResult {
  id: string;
  apiId: string;
  testName: string;
  description: string;
  testConfiguration: LoadTestConfiguration;
  results: LoadTestMetrics;
  status: 'running' | 'completed' | 'failed' | 'cancelled';
  startedAt: string;
  completedAt?: string;
  duration?: number; // in seconds
  createdAt: string;
  createdBy: string;
}

export interface LoadTestConfiguration {
  virtualUsers: number;
  duration: number; // in seconds
  rampUpTime: number; // in seconds
  targetEndpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  body?: string;
  expectedResponseTime?: number; // in milliseconds
  expectedThroughput?: number; // requests per second
}

export interface LoadTestMetrics {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageResponseTime: number; // in milliseconds
  minResponseTime: number;
  maxResponseTime: number;
  p50ResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  throughput: number; // requests per second
  errorRate: number; // percentage
  errors: LoadTestError[];
}

export interface LoadTestError {
  type: string;
  message: string;
  count: number;
  percentage: number;
}

// API Linting Types
export interface LintResult {
  id: string;
  apiId: string;
  lintedAt: string;
  status: 'passed' | 'failed' | 'warning';
  score: number; // 0-100
  rules: LintRule[];
  summary: LintSummary;
  createdAt: string;
  createdBy: string;
}

export interface LintRule {
  id: string;
  name: string;
  description: string;
  category: LintCategory;
  severity: 'error' | 'warning' | 'info';
  status: 'passed' | 'failed' | 'skipped';
  message?: string;
  suggestion?: string;
  line?: number;
  column?: number;
}

export type LintCategory = 
  | 'security'
  | 'performance'
  | 'design'
  | 'documentation'
  | 'naming'
  | 'structure'
  | 'best_practices';

export interface LintSummary {
  totalRules: number;
  passed: number;
  failed: number;
  warnings: number;
  errors: number;
  skipped: number;
  categories: Record<LintCategory, number>;
}

// Lint Configuration Types
export interface LintConfiguration {
  id: string;
  apiId: string;
  name: string;
  description: string;
  rules: LintRuleConfig[];
  isActive: boolean;
  createdAt: string;
  createdBy: string;
}

export interface LintRuleConfig {
  ruleId: string;
  enabled: boolean;
  severity: 'error' | 'warning' | 'info';
  customMessage?: string;
  parameters?: Record<string, any>;
}

// API Quality Summary Types
export interface APIQualitySummary {
  apiId: string;
  overallScore: number; // 0-100
  sloStatus: 'healthy' | 'warning' | 'critical';
  slaCompliance: number; // percentage
  lastLoadTest?: LoadTestResult;
  lastLintResult?: LintResult;
  recommendations: QualityRecommendation[];
  lastUpdated: string;
}

export interface QualityRecommendation {
  id: string;
  type: 'slo' | 'sla' | 'performance' | 'security' | 'design';
  priority: 'high' | 'medium' | 'low';
  title: string;
  description: string;
  action: string;
  impact: string;
}

// Form Types for UI
export interface SLOCreateRequest {
  name: string;
  description: string;
  metric: SLOMetric;
  target: number;
  measurementWindow: '1h' | '24h' | '7d' | '30d';
}

export interface SLACreateRequest {
  name: string;
  description: string;
  terms: Omit<SLATerm, 'id'>[];
  effectiveDate: string;
  expiryDate?: string;
}

export interface LoadTestCreateRequest {
  testName: string;
  description: string;
  configuration: LoadTestConfiguration;
}

export interface LintConfigurationCreateRequest {
  name: string;
  description: string;
  rules: Omit<LintRuleConfig, 'ruleId'>[];
}

// Mock Data Types for Development
export interface MockAPIQualityData {
  slos: SLO[];
  slas: SLA[];
  loadTestResults: LoadTestResult[];
  lintResults: LintResult[];
  lintConfigurations: LintConfiguration[];
  qualitySummary: APIQualitySummary;
}
