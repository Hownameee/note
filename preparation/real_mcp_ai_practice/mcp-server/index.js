import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

// Khởi tạo MCP Server thông tin định danh
const server = new Server(
  {
    name: "bank-mcp-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Mock data cho tài khoản ngân hàng
const accounts = {
  "123": { balance: 5000000, name: "Nguyen Van A" },
  "456": { balance: 1000000, name: "Tran Thi B" },
};

// Định nghĩa danh sách tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "getAccountBalance",
        description: "Tra cứu số dư hiện tại của tài khoản ngân hàng.",
        inputSchema: {
          type: "object",
          properties: {
            accountId: {
              type: "string",
              description: "Mã số tài khoản ngân hàng cần tra cứu (ví dụ: '123').",
            },
          },
          required: ["accountId"],
        },
      },
      {
        name: "transferMoney",
        description:
          "Thực hiện giao dịch chuyển tiền giữa hai tài khoản. Nếu chưa có mã OTP, giao dịch yêu cầu cung cấp OTP trước.",
        inputSchema: {
          type: "object",
          properties: {
            fromAccountId: {
              type: "string",
              description: "Mã số tài khoản chuyển đi.",
            },
            toAccountId: {
              type: "string",
              description: "Mã số tài khoản nhận.",
            },
            amount: {
              type: "number",
              description: "Số tiền muốn chuyển (VND).",
            },
            otp: {
              type: "string",
              description: "Mã xác thực OTP (6 chữ số). Bỏ trống nếu chưa có.",
            },
          },
          required: ["fromAccountId", "toAccountId", "amount"],
        },
      },
    ],
  };
});

// Xử lý thực thi tool
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  console.error(`[Server Log] Calling tool ${name} with args:`, JSON.stringify(args));

  if (name === "getAccountBalance") {
    const { accountId } = args;
    const account = accounts[accountId];
    if (!account) {
      return {
        content: [
          {
            type: "text",
            text: `Không tìm thấy tài khoản ngân hàng số ${accountId}.`,
          },
        ],
      };
    }
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify({
            status: "SUCCESS",
            accountId: accountId,
            accountName: account.name,
            balance: account.balance,
            currency: "VND",
          }),
        },
      ],
    };
  }

  if (name === "transferMoney") {
    const { fromAccountId, toAccountId, amount, otp } = args;

    const fromAccount = accounts[fromAccountId];
    const toAccount = accounts[toAccountId];

    if (!fromAccount) {
      return {
        content: [
          {
            type: "text",
            text: `Lỗi: Không tìm thấy tài khoản nguồn ${fromAccountId}.`,
          },
        ],
      };
    }

    if (!toAccount) {
      return {
        content: [
          {
            type: "text",
            text: `Lỗi: Không tìm thấy tài khoản đích ${toAccountId}.`,
          },
        ],
      };
    }

    if (fromAccount.balance < amount) {
      return {
        content: [
          {
            type: "text",
            text: `Lỗi: Số dư tài khoản ${fromAccountId} không đủ để thực hiện chuyển ${amount} VND.`,
          },
        ],
      };
    }

    // Giai đoạn 1: Chưa nhập OTP
    if (!otp) {
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify({
              status: "REQUIRE_OTP",
              message: `Yêu cầu xác nhận: Chuyển ${amount} VND từ tài khoản ${fromAccountId} (${fromAccount.name}) đến tài khoản ${toAccountId} (${toAccount.name}). Mã OTP đã được gửi đến số điện thoại đăng ký. Vui lòng cung cấp mã OTP để xác nhận giao dịch.`,
            }),
          },
        ],
      };
    }

    // Giai đoạn 2: Đã nhập OTP
    if (otp !== "999888") {
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify({
              status: "INVALID_OTP",
              message: "Mã OTP cung cấp không chính xác hoặc đã hết hạn. Vui lòng thử lại với OTP hợp lệ (ví dụ: '123456').",
            }),
          },
        ],
      };
    }

    // Thực hiện trừ tiền
    fromAccount.balance -= amount;
    toAccount.balance += amount;

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify({
            status: "SUCCESS",
            message: `Giao dịch thành công! Đã chuyển ${amount} VND từ tài khoản ${fromAccountId} sang ${toAccountId}. Số dư mới của tài khoản ${fromAccountId} là ${fromAccount.balance} VND.`,
            transactionId: "TXN_" + Math.random().toString(36).substr(2, 9).toUpperCase(),
          }),
        },
      ],
    };
  }

  throw new Error(`Tool không hợp lệ: ${name}`);
});

// Chạy server thông qua STDIO transport
async function run() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("MCP Server is running on STDIO");
}

run().catch((error) => {
  console.error("Fatal error running server:", error);
  process.exit(1);
});
