package io.spec0.mockserver.standalone.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

  @Bean
  public ToolCallbackProvider mockServerTools(MockServerMcpTools mockServerMcpTools) {
    return MethodToolCallbackProvider.builder().toolObjects(mockServerMcpTools).build();
  }
}
