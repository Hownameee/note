package com.mastery.mcpclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final List<ToolCallbackProvider> toolProviders;

    public ChatController(ChatClient.Builder chatClientBuilder, List<ToolCallbackProvider> toolProviders) {
        this.toolProviders = toolProviders;
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                Bạn là trợ lý ảo ngân hàng thông minh và chuyên nghiệp. Quy tắc BẮT BUỘC thực thi:
                1. Khi người dùng muốn xem/kiểm tra/tra cứu số dư tài khoản:
                   - Bạn PHẢI sử dụng công cụ 'getAccountBalance' ngay lập tức mà KHÔNG nói bất kỳ câu dẫn nào trước.
                   - Sau khi nhận được kết quả từ công cụ, hãy phản hồi lại cho người dùng bằng tiếng Việt thân thiện.
                2. Khi người dùng muốn thực hiện chuyển tiền:
                   - Bạn PHẢI gọi công cụ 'transferMoney' để thực hiện giao dịch chuyển tiền.
                   - Nếu hệ thống yêu cầu mã OTP, hãy phản hồi lại thông điệp đó cho người dùng để họ cung cấp OTP.
                3. Tuyệt đối không tự suy diễn số dư hoặc tự bịa ra kết quả giao dịch mà không gọi công cụ.
                """)
            .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String msg) {
        System.out.println("💬 [User]: " + msg);

        // Lấy tool callbacks từ provider tại thời điểm request
        ToolCallback[] callbacks = toolProviders.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        System.out.println("🔧 [Tools available]: " + callbacks.length + " tools found");
        for (ToolCallback cb : callbacks) {
            System.out.println("   - Tool: " + cb.getToolDefinition().name() + " (" + cb.getToolDefinition().description() + ")");
        }

        try {
            String response = chatClient.prompt()
                .user(msg)
                .toolCallbacks(callbacks)
                .call()
                .content();
            System.out.println("🤖 [AI]: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("❌ Error during chat execution:");
            e.printStackTrace();
            return "LỖI HỆ THỐNG: " + e.getMessage() + "\nNguyên nhân: " + (e.getCause() != null ? e.getCause().getMessage() : "Không có");
        }
    }
}
