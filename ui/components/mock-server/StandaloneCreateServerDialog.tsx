'use client';

import React, { useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Upload } from 'lucide-react';
import { useStandaloneClient } from '@/context/StandaloneServerContext';
import type { MockServer } from '@/services/mockServerAdminClient';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: (server: MockServer) => void;
}

export default function StandaloneCreateServerDialog({ open, onOpenChange, onCreated }: Props) {
  const client = useStandaloneClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [serverName, setServerName] = useState('');
  const [specContent, setSpecContent] = useState('');
  const [specName, setSpecName] = useState('');
  const [strategy, setStrategy] = useState<'RANDOM' | 'ROUND_ROBIN' | 'SEQUENTIAL' | 'DEFAULT_ONLY'>('RANDOM');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target?.result as string;
      setSpecContent(text ?? '');
      if (!specName) setSpecName(file.name.replace(/\.(yaml|yml|json)$/i, ''));
      if (!serverName) setServerName(file.name.replace(/\.(yaml|yml|json)$/i, '') + '-mock');
    };
    reader.readAsText(file);
  };

  const handleSubmit = async () => {
    if (!serverName.trim() || !specContent.trim() || !specName.trim()) {
      setError('Server name, spec name, and spec content are all required.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const spec = await client.registerSpec({ specName: specName.trim(), specContent: specContent.trim() });
      const server = await client.createMockServer({
        specId: spec.specId,
        name: serverName.trim(),
        defaultStrategy: strategy,
      });
      onCreated(server);
      onOpenChange(false);
      // Reset form
      setServerName('');
      setSpecContent('');
      setSpecName('');
      setStrategy('RANDOM');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create mock server.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Mock Server</DialogTitle>
          <DialogDescription>
            Upload or paste an OpenAPI spec (YAML or JSON) and configure your mock server.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Spec upload */}
          <div className="space-y-2">
            <Label>OpenAPI Spec</Label>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="h-4 w-4 mr-2" />
                Upload file
              </Button>
              <span className="text-xs text-muted-foreground self-center">or paste below</span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".yaml,.yml,.json"
              className="hidden"
              onChange={handleFileChange}
            />
            <Textarea
              placeholder="openapi: 3.0.0&#10;info:&#10;  title: My API&#10;  version: 1.0.0&#10;paths: ..."
              value={specContent}
              onChange={(e) => setSpecContent(e.target.value)}
              className="font-mono text-xs min-h-[180px]"
            />
          </div>

          {/* Spec name */}
          <div className="space-y-2">
            <Label htmlFor="specName">Spec name</Label>
            <Input
              id="specName"
              placeholder="e.g. petstore"
              value={specName}
              onChange={(e) => setSpecName(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Used to identify the spec — re-registering the same name + content is idempotent.
            </p>
          </div>

          {/* Server name */}
          <div className="space-y-2">
            <Label htmlFor="serverName">Mock server name</Label>
            <Input
              id="serverName"
              placeholder="e.g. petstore-mock"
              value={serverName}
              onChange={(e) => setServerName(e.target.value)}
            />
          </div>

          {/* Strategy */}
          <div className="space-y-2">
            <Label>Default response strategy</Label>
            <Select value={strategy} onValueChange={(v) => setStrategy(v as typeof strategy)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="RANDOM">Random</SelectItem>
                <SelectItem value="SEQUENTIAL">Sequential</SelectItem>
                <SelectItem value="ROUND_ROBIN">Round Robin</SelectItem>
                <SelectItem value="DEFAULT_ONLY">Default Only</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? 'Creating...' : 'Create Mock Server'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
