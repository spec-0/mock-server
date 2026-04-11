/**
 * Team Resources Management Types
 * 
 * This file contains all types related to team resource management,
 * including markdown documents, links, embedded views, and other resources.
 */

// Base Resource Types
export interface BaseResource {
  id: string;
  teamId: string;
  title: string;
  description?: string;
  type: ResourceType;
  tags: string[];
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export type ResourceType = 
  | 'markdown'
  | 'link'
  | 'embedded'
  | 'file'
  | 'code_snippet'
  | 'image'
  | 'video'
  | 'documentation'
  | 'dashboard'
  | 'api_endpoint';

// Markdown Resource
export interface MarkdownResource extends BaseResource {
  type: 'markdown';
  content: string;
  metadata: {
    wordCount?: number;
    readingTime?: number;
    lastModified?: string;
  };
}

// Link Resource
export interface LinkResource extends BaseResource {
  type: 'link';
  url: string;
  metadata: {
    title?: string;
    description?: string;
    favicon?: string;
    domain?: string;
    isSecure?: boolean;
    lastChecked?: string;
  };
}

// Embedded Resource (for dashboards, etc.)
export interface EmbeddedResource extends BaseResource {
  type: 'embedded';
  embedUrl: string;
  embedType: 'iframe' | 'grafana' | 'kibana' | 'datadog' | 'custom';
  metadata: {
    width?: number;
    height?: number;
    allowFullscreen?: boolean;
    sandbox?: string;
    refreshInterval?: number;
  };
}

// File Resource
export interface FileResource extends BaseResource {
  type: 'file';
  fileName: string;
  fileSize: number;
  mimeType: string;
  downloadUrl: string;
  metadata: {
    extension?: string;
    lastModified?: string;
    checksum?: string;
  };
}

// Code Snippet Resource
export interface CodeSnippetResource extends BaseResource {
  type: 'code_snippet';
  language: string;
  code: string;
  metadata: {
    lineCount?: number;
    syntaxHighlighted?: boolean;
    executable?: boolean;
  };
}

// Image Resource
export interface ImageResource extends BaseResource {
  type: 'image';
  imageUrl: string;
  thumbnailUrl?: string;
  metadata: {
    width?: number;
    height?: number;
    alt?: string;
    format?: string;
  };
}

// Video Resource
export interface VideoResource extends BaseResource {
  type: 'video';
  videoUrl: string;
  thumbnailUrl?: string;
  metadata: {
    duration?: number;
    format?: string;
    platform?: 'youtube' | 'vimeo' | 'custom';
    videoId?: string;
  };
}

// Documentation Resource
export interface DocumentationResource extends BaseResource {
  type: 'documentation';
  content: string;
  format: 'markdown' | 'html' | 'text';
  metadata: {
    version?: string;
    author?: string;
    lastReviewed?: string;
    reviewStatus?: 'draft' | 'review' | 'approved';
  };
}

// Dashboard Resource
export interface DashboardResource extends BaseResource {
  type: 'dashboard';
  dashboardUrl: string;
  dashboardType: 'grafana' | 'kibana' | 'datadog' | 'custom';
  metadata: {
    refreshInterval?: number;
    autoRefresh?: boolean;
    timeRange?: string;
    variables?: Record<string, any>;
  };
}

// API Endpoint Resource
export interface APIEndpointResource extends BaseResource {
  type: 'api_endpoint';
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  metadata: {
    baseUrl?: string;
    headers?: Record<string, string>;
    authentication?: 'none' | 'bearer' | 'basic' | 'api_key';
    rateLimit?: number;
    documentation?: string;
  };
}

// Union type for all resources
export type TeamResource = 
  | MarkdownResource
  | LinkResource
  | EmbeddedResource
  | FileResource
  | CodeSnippetResource
  | ImageResource
  | VideoResource
  | DocumentationResource
  | DashboardResource
  | APIEndpointResource;

// Resource Categories
export interface ResourceCategory {
  id: string;
  name: string;
  description?: string;
  color?: string;
  icon?: string;
  resourceCount: number;
}

// Resource Collection
export interface ResourceCollection {
  id: string;
  teamId: string;
  name: string;
  description?: string;
  resources: string[]; // Resource IDs
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

// Resource Search and Filter
export interface ResourceFilter {
  type?: ResourceType[];
  tags?: string[];
  isPublic?: boolean;
  createdBy?: string;
  dateRange?: {
    start: string;
    end: string;
  };
  search?: string;
}

export interface ResourceSearchResult {
  resources: TeamResource[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

// Form Types for Creating/Editing Resources
export interface CreateResourceRequest {
  title: string;
  description?: string;
  type: ResourceType;
  tags: string[];
  isPublic: boolean;
  // Type-specific data
  content?: string; // For markdown, code_snippet, documentation
  url?: string; // For link, embedded
  embedType?: EmbeddedResource['embedType'];
  fileName?: string; // For file
  language?: string; // For code_snippet
  imageUrl?: string; // For image
  videoUrl?: string; // For video
  dashboardType?: DashboardResource['dashboardType'];
  endpoint?: string; // For api_endpoint
  method?: APIEndpointResource['method'];
  metadata?: Record<string, any>;
}

export interface UpdateResourceRequest extends Partial<CreateResourceRequest> {
  id: string;
}

// Resource Analytics
export interface ResourceAnalytics {
  totalResources: number;
  resourcesByType: Record<ResourceType, number>;
  resourcesByUser: Record<string, number>;
  popularTags: Array<{ tag: string; count: number }>;
  recentActivity: Array<{
    resourceId: string;
    action: 'created' | 'updated' | 'viewed';
    timestamp: string;
    userId: string;
  }>;
}

// Mock Data Types for Development
export interface MockTeamResourceData {
  resources: TeamResource[];
  categories: ResourceCategory[];
  collections: ResourceCollection[];
  analytics: ResourceAnalytics;
}

// Resource Preview Types
export interface ResourcePreview {
  id: string;
  title: string;
  description?: string;
  type: ResourceType;
  thumbnail?: string;
  metadata?: Record<string, any>;
  createdAt: string;
  createdBy: string;
}

// Resource Sharing
export interface ResourceShare {
  id: string;
  resourceId: string;
  sharedWith: string[]; // User IDs or team IDs
  permissions: 'view' | 'edit' | 'admin';
  expiresAt?: string;
  createdAt: string;
  createdBy: string;
}
