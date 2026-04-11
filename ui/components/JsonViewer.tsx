'use client';

import React, { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Copy, ChevronDown, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface JsonViewerProps {
  data: any;
  title?: string;
  collapsible?: boolean;
  defaultExpanded?: boolean;
  className?: string;
}

export function JsonViewer({ 
  data, 
  title, 
  collapsible = false, 
  defaultExpanded = true,
  className = ""
}: JsonViewerProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const [copied, setCopied] = useState(false);

  const copyToClipboard = async () => {
    try {
      const jsonString = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
      await navigator.clipboard.writeText(jsonString);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
    }
  };

  const formatJson = (data: any): string => {
    if (data === null || data === undefined) {
      return 'null';
    }
    if (typeof data === 'string') {
      try {
        // Try to parse and re-stringify to format
        return JSON.stringify(JSON.parse(data), null, 2);
      } catch {
        // If not valid JSON, return as string
        return data;
      }
    }
    return JSON.stringify(data, null, 2);
  };

  const jsonString = formatJson(data);
  const isEmpty = !data || (typeof data === 'object' && Object.keys(data).length === 0);

  return (
    <div className={`border rounded-lg bg-gray-900 ${className}`}>
      {(title || collapsible) && (
        <div className="flex items-center justify-between p-3 border-b bg-gray-800 border-gray-700">
          <div className="flex items-center gap-2">
            {collapsible && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsExpanded(!isExpanded)}
                className="h-6 w-6 p-0 text-gray-300 hover:text-white hover:bg-gray-700"
              >
                {isExpanded ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </Button>
            )}
            {title && (
              <h4 className="text-sm font-medium text-gray-200">
                {title}
              </h4>
            )}
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={copyToClipboard}
            className="h-6 w-6 p-0 text-gray-300 hover:text-white hover:bg-gray-700"
            title="Copy to clipboard"
          >
            <Copy className="h-4 w-4" />
          </Button>
        </div>
      )}
      
      {(!collapsible || isExpanded) && (
        <div className="p-0">
          {isEmpty ? (
            <div className="text-sm text-gray-400 italic p-4">
              No data available
            </div>
          ) : (
            <div className="relative">
              <SyntaxHighlighter
                language="json"
                style={vscDarkPlus}
                customStyle={{
                  margin: 0,
                  background: '#1e1e1e',
                  fontSize: '0.875rem',
                  lineHeight: '1.6',
                  padding: '1rem',
                  borderRadius: '0 0 0.5rem 0.5rem',
                  fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
                }}
                wrapLines={true}
                wrapLongLines={true}
                showLineNumbers={false}
              >
                {jsonString}
              </SyntaxHighlighter>
              {copied && (
                <div className="absolute top-2 right-2 bg-green-500 text-white px-2 py-1 rounded text-xs">
                  Copied!
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
