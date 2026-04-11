'use client';

import React, { useState, useEffect } from 'react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface PathVariableEditorProps {
  path: string;
  pathParams?: Record<string, string>;
  onPathParamsChange: (pathParams: Record<string, string>) => void;
  className?: string;
  showVariablesSection?: boolean;
}

interface PathSegment {
  type: 'static' | 'variable';
  content: string;
  variableName?: string;
}

export default function PathVariableEditor({ 
  path, 
  pathParams = {}, 
  onPathParamsChange,
  className = '',
  showVariablesSection = true
}: PathVariableEditorProps) {
  const [segments, setSegments] = useState<PathSegment[]>([]);
  const [localPathParams, setLocalPathParams] = useState<Record<string, string>>(pathParams);

  // Parse the path into segments
  useEffect(() => {
    const parsedSegments: PathSegment[] = [];
    const pathParts = path.split('/');
    
    for (const part of pathParts) {
      if (part.startsWith('{') && part.endsWith('}')) {
        // This is a path variable
        const variableName = part.slice(1, -1); // Remove { and }
        parsedSegments.push({
          type: 'variable',
          content: part,
          variableName
        });
      } else if (part) {
        // This is a static segment
        parsedSegments.push({
          type: 'static',
          content: part
        });
      }
    }
    
    setSegments(parsedSegments);
  }, [path]);

  // Initialize path params with default values
  useEffect(() => {
    const defaultParams: Record<string, string> = {};
    segments.forEach(segment => {
      if (segment.type === 'variable' && segment.variableName) {
        if (!localPathParams[segment.variableName]) {
          // Generate a default value based on the variable name
          defaultParams[segment.variableName] = generateDefaultValue(segment.variableName);
        }
      }
    });
    
    if (Object.keys(defaultParams).length > 0) {
      const newParams = { ...localPathParams, ...defaultParams };
      setLocalPathParams(newParams);
      onPathParamsChange(newParams);
    }
  }, [segments]);

  const generateDefaultValue = (variableName: string): string => {
    const lowerName = variableName.toLowerCase();
    
    // Common patterns for generating default values
    if (lowerName.includes('id')) {
      return '123';
    } else if (lowerName.includes('user')) {
      return 'user-123';
    } else if (lowerName.includes('team')) {
      return 'team-456';
    } else if (lowerName.includes('org')) {
      return 'org-789';
    } else if (lowerName.includes('uuid')) {
      return '550e8400-e29b-41d4-a716-446655440000';
    } else if (lowerName.includes('slug')) {
      return 'example-slug';
    } else {
      return 'example-value';
    }
  };

  const handleVariableChange = (variableName: string, value: string) => {
    const newParams = { ...localPathParams, [variableName]: value };
    setLocalPathParams(newParams);
    onPathParamsChange(newParams);
  };

  const buildFinalPath = (): string => {
    return segments.map(segment => {
      if (segment.type === 'variable' && segment.variableName) {
        return localPathParams[segment.variableName] || segment.content;
      }
      return segment.content;
    }).join('/');
  };

  if (segments.length === 0) {
    return (
      <div className={`font-mono text-sm ${className}`}>
        {path}
      </div>
    );
  }

  return (
    <div className={`space-y-2 ${className}`}>
      {/* Display the path with editable variables */}
      <div className="flex flex-wrap items-center gap-1 font-mono text-sm">
        {segments.map((segment, index) => (
          <React.Fragment key={index}>
            {segment.type === 'static' ? (
              <span className="text-gray-700">/{segment.content}</span>
            ) : (
              <div className="flex items-center">
                <span className="text-gray-700">/</span>
                <Input
                  value={localPathParams[segment.variableName!] || ''}
                  onChange={(e) => handleVariableChange(segment.variableName!, e.target.value)}
                  className="w-32 h-6 text-xs font-mono bg-green-100 border-green-300 text-green-800 placeholder-green-600 focus:bg-green-50 focus:border-green-400"
                  placeholder={segment.variableName}
                />
              </div>
            )}
          </React.Fragment>
        ))}
      </div>
      
      
      {/* Show individual variable controls - only if showVariablesSection is true */}
      {showVariablesSection && segments.some(s => s.type === 'variable') && (
        <div className="mt-3 space-y-2">
          <Label className="text-xs font-medium text-gray-700">Path Variables:</Label>
          <div className="grid grid-cols-1 gap-2">
            {segments
              .filter(s => s.type === 'variable')
              .map((segment, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <Label className="text-xs text-gray-600 w-20 truncate">
                    {segment.variableName}:
                  </Label>
                  <Input
                    value={localPathParams[segment.variableName!] || ''}
                    onChange={(e) => handleVariableChange(segment.variableName!, e.target.value)}
                    className="flex-1 h-7 text-xs font-mono bg-green-100 border-green-300 text-green-800 placeholder-green-600 focus:bg-green-50 focus:border-green-400"
                    placeholder={`Enter ${segment.variableName}`}
                  />
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
