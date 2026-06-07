package com.mastery.mcpclient;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpSyncClient() {
        // Cấu hình tham số để chạy Node.js MCP server qua STDIO
        ServerParameters params = ServerParameters.builder("node")
                .arg("/home/nam/Documents/code/note/real_mcp_ai_practice/mcp-server/index.js")
                .build();

        StdioClientTransport transport = new StdioClientTransport(params);

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new io.modelcontextprotocol.spec.McpSchema.Implementation("bank-mcp-client", "1.0.0"))
                .build();

        // Bắt buộc thực hiện initialize để hoàn tất handshake với Node.js server
        client.initialize();
        System.out.println("✅ [McpConfig] MCP Client initialized and connected to Node.js via STDIO!");

        return client;
    }

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(McpSyncClient mcpSyncClient) {
        return new SyncMcpToolCallbackProvider(List.of(mcpSyncClient));
    }
}
