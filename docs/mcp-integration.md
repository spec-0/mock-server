# MCP Integration

The mock server exposes a built-in **MCP (Model Context Protocol) server**, letting AI assistants like Claude and Cursor manage your mock server directly — creating variants, inspecting logs, changing strategies, and more — without leaving the conversation.

---

## Enabling MCP

MCP is disabled by default. Enable it per mock server:

**From the UI:** Go to **Settings** tab → toggle **Enable MCP**.

**From the API:**
```bash
curl -X PATCH http://localhost:8080/mock-server/servers/{mockServerId}/mcp-config \
  -H 'Content-Type: application/json' \
  -d '{"mcpEnabled": true}'
```

Once enabled, the SSE endpoint is available at:

```
http://localhost:8080/mcp/sse
```

---

## Connecting Claude Desktop

Add the following to your `claude_desktop_config.json`:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "spec0-mock-server": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Restart Claude Desktop. The mock server tools will appear in the tool picker.

---

## Connecting Cursor

Create or edit `.cursor/mcp.json` in your project root (or your global Cursor config):

```json
{
  "mcpServers": {
    "spec0-mock-server": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Restart Cursor. The tools are now available in Cursor's AI chat and Composer.

---

## Connecting Claude Code (CLI)

Add to your project's `CLAUDE.md` or to `~/.claude/CLAUDE.md`:

```json
{
  "mcpServers": {
    "spec0-mock-server": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Or pass it at startup:

```bash
claude --mcp-server spec0-mock-server=http://localhost:8080/mcp/sse
```

---

## Available tools

Once connected, your AI assistant can call the following tools:

| Tool | What it does |
|------|-------------|
| `list_mock_servers` | List all mock servers with their IDs, names, and base URLs |
| `list_operations` | List operations (endpoints) from the OpenAPI spec for a given server |
| `list_variants` | List response variants, optionally filtered by `operationId` |
| `create_variant` | Create a static response variant with name, status code, body, and default flag |
| `create_cel_variant` | Create a CEL-evaluated dynamic variant (supports natural language description) |
| `delete_variant` | Delete a variant by ID |
| `set_strategy` | Set the response selection strategy (`RANDOM`, `DEFAULT_ONLY`, `SEQUENTIAL`, `ROUND_ROBIN`) |
| `get_logs` | Retrieve recent request logs (default 20, max 200) |
| `reset_to_defaults` | Delete all user-created variants and reset the strategy to `RANDOM` |

---

## Example prompts

Once the MCP server is connected, you can ask your AI assistant things like:

> "List all mock servers and show me the operations for the Users service."

> "Create a variant for `getUser` that returns a 404 with body `{"error": "user not found"}` when the user ID is 99."

> "Set the strategy for this mock server to SEQUENTIAL so my tests get predictable responses."

> "Show me the last 10 request logs for this mock server."

> "Create a CEL variant for `createOrder` that generates a random UUID for the order ID and includes the current timestamp."

> "Reset all variants on the Payments mock server back to defaults."

---

## Notes

- MCP operates on all mock servers on the same instance — the `list_mock_servers` tool returns all of them.
- The SSE endpoint does not require authentication in the current version.
- If you run the mock server on a non-default port, update the URL in your MCP config accordingly.
