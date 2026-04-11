# MCP Integration

The mock server exposes a built-in **MCP (Model Context Protocol) server**, letting AI assistants like Claude and Cursor manage your mock server directly — creating variants, inspecting logs, changing strategies, and more — without leaving the conversation.

## Contents

- [Enabling MCP](#enabling-mcp)
- [Connecting an AI assistant](#connecting-an-ai-assistant)
  - [Claude Desktop](#claude-desktop)
  - [Cursor](#cursor)
  - [Claude Code CLI](#claude-code-cli)
- [Available tools](#available-tools)
- [Example prompts](#example-prompts)

---

## Enabling MCP

MCP is disabled by default and enabled per mock server.

**From the UI:** Go to the **Settings** tab → toggle **Enable MCP**.

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

> [!NOTE]
> The SSE endpoint does not require authentication. If you run the mock server on a non-default port, update the URL accordingly.

---

## Connecting an AI assistant

Add the server to your AI assistant's MCP config, then restart the client.

### Claude Desktop

**Config file location:**
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

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

### Cursor

Create or edit `.cursor/mcp.json` in your project root:

```json
{
  "mcpServers": {
    "spec0-mock-server": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Restart Cursor. The tools are available in AI chat and Composer.

### Claude Code CLI

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

| Tool | Description |
|------|-------------|
| `list_mock_servers` | List all mock servers with their IDs, names, and base URLs |
| `list_operations` | List operations from the OpenAPI spec for a given server |
| `list_variants` | List response variants, optionally filtered by `operationId` |
| `create_variant` | Create a static response variant (name, status code, body, default flag) |
| `create_cel_variant` | Create a CEL-evaluated dynamic variant (supports natural language description) |
| `delete_variant` | Delete a variant by ID |
| `set_strategy` | Set the response selection strategy (`RANDOM`, `DEFAULT_ONLY`, `SEQUENTIAL`, `ROUND_ROBIN`) |
| `get_logs` | Retrieve recent request logs (default 20, max 200) |
| `reset_to_defaults` | Delete all user-created variants and reset the strategy to `RANDOM` |

---

## Example prompts

Once the MCP server is connected, you can ask your AI assistant in plain language:

**Exploring your servers**
> "List all mock servers and show me the operations for the Users service."

**Creating variants**
> "Create a variant for `getUser` that returns a 404 with `{"error": "user not found"}` when the user ID is 99."

> "Create a CEL variant for `createOrder` that generates a random UUID for the order ID and includes the current timestamp."

**Controlling strategy**
> "Set the strategy for this mock server to SEQUENTIAL so my tests get predictable responses."

**Debugging**
> "Show me the last 10 request logs for this mock server."

**Resetting state**
> "Reset all variants on the Payments mock server back to defaults."

---

## See also

- [Variants & Response Strategies](./variants-and-strategies.md) — strategies that `set_strategy` controls
- [CEL Expressions](./cel-expressions.md) — what `create_cel_variant` can express
- [Request Logs](./request-logs.md) — what `get_logs` returns
- [← Documentation index](./README.md)
