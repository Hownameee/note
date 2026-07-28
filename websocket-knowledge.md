# WebSocket Knowledge Notes

## 1. What is WebSocket?

WebSocket is a **full-duplex communication protocol** over a single TCP connection, designed to be implemented in web browsers and web servers. It enables real-time, bidirectional communication between client and server.

### Key Characteristics
- **Full-duplex**: Both client and server can send messages simultaneously.
- **Persistent connection**: Unlike HTTP, once established, the connection remains open.
- **Low latency**: Reduces overhead by avoiding repeated HTTP handshakes.
- **Frame-based**: Data is transmitted in frames rather than streams.

---

## 2. WebSocket vs HTTP

| Feature               | HTTP                          | WebSocket                      |
|-----------------------|-------------------------------|--------------------------------|
| Communication         | Half-duplex (request/response)| Full-duplex                    |
| Connection lifetime   | Short-lived (per request)     | Long-lived (persistent)        |
| Protocol              | HTTP 1.x / 2.x / 3.x         | ws:// or wss://                |
| Headers               | Heavy per request             | Lightweight after handshake    |
| Streaming             | SSE (server→client only)      | Bidirectional                  |
| Use cases             | REST APIs, web pages          | Real-time apps, gaming, chat   |

---

## 3. WebSocket Handshake

The connection begins with an **HTTP upgrade handshake**:

### Client Request (HTTP → WebSocket upgrade)
```
GET /chat HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

### Server Response
```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

- `Sec-WebSocket-Key` is a random base64-encoded key sent by the client.
- The server appends a **fixed GUID** (`258EAFA5-E914-47DA-95CA-C5AB0DC85B11`) to the key, hashes with SHA-1, and base64 encodes it to produce `Sec-WebSocket-Accept`.

---

## 4. WebSocket Frames

Data is transmitted in **frames**. WebSocket frames follow a specific binary format:

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - -+
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - -+
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data (continued)                  |
+---------------------------------------------------------------+
```

### Opcodes
| Opcode | Type              |
|--------|-------------------|
| 0x0    | Continuation frame|
| 0x1    | Text frame (UTF-8)|
| 0x2    | Binary frame      |
| 0x8    | Connection close  |
| 0x9    | Ping              |
| 0xA    | Pong              |

### Masking
- **Clients MUST mask** all frames sent to the server.
- **Servers MUST NOT mask** frames sent to the client.
- Masking prevents cache poisoning attacks.

---

## 5. WebSocket URLs

| Scheme | Meaning       | Default Port |
|--------|---------------|--------------|
| `ws://`  | Unencrypted   | 80           |
| `wss://` | Encrypted (TLS)| 443          |

---

## 6. WebSocket Lifecycle

```
CLIENT                    SERVER
  |                         |
  |--- HTTP Upgrade ------->|  (1) Opening Handshake
  |<-- 101 Switching ------|      Protocols
  |                         |
  |--- Data Frames -------->|  (2) Data Transfer
  |<-- Data Frames ---------|      (bidirectional)
  |                         |
  |--- Close Frame -------->|  (3) Closing Handshake
  |<-- Close Frame ---------|
  |                         |
```

### States (WebSocket.readyState)
| State  | Value | Description                       |
|--------|-------|-----------------------------------|
| CONNECTING | 0 | Connection not yet established |
| OPEN       | 1 | Connection established, ready to communicate |
| CLOSING    | 2 | Connection is closing |
| CLOSED     | 3 | Connection is closed or failed to open |

---

## 7. JavaScript WebSocket API (Browser)

```js
// Create connection
const socket = new WebSocket('wss://example.com/ws');

// Connection opened
socket.addEventListener('open', (event) => {
    socket.send('Hello Server!');
    
    // Sending binary data
    const buffer = new ArrayBuffer(4);
    socket.send(buffer);
    
    // Sending Blob
    const blob = new Blob(['Hello']);
    socket.send(blob);
});

// Listen for messages
socket.addEventListener('message', (event) => {
    console.log('Message from server:', event.data);
    // event.data can be String, Blob, or ArrayBuffer
    // Check socket.binaryType: 'blob' (default) or 'arraybuffer'
});

// Listen for errors
socket.addEventListener('error', (event) => {
    console.error('WebSocket error:', event);
});

// Listen for connection close
socket.addEventListener('close', (event) => {
    console.log('Connection closed:', event.code, event.reason);
    // event.code: close status code
    // event.reason: close reason (string)
    // event.wasClean: boolean
});

// Close connection
socket.close(1000, 'Closing normally');
```

---

## 8. WebSocket Close Codes

| Code  | Meaning                          |
|-------|----------------------------------|
| 1000  | Normal closure                   |
| 1001  | Going away                       |
| 1002  | Protocol error                   |
| 1003  | Unsupported data                 |
| 1005  | No status received (internal)    |
| 1006  | Abnormal closure (internal)      |
| 1007  | Invalid frame payload data       |
| 1008  | Policy violation                 |
| 1009  | Message too big                  |
| 1010  | Missing extension                |
| 1011  | Internal error on server         |
| 1015  | TLS handshake failure (internal) |

---

## 9. WebSocket with Spring Boot (Java)

### WebSocket Configuration
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/chat")
                .setAllowedOrigins("*")
                .withSockJS(); // Fallback for older browsers
    }
}
```

### WebSocket Handler
```java
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final Set<WebSocketSession> sessions = 
        new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("Connected: " + session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, 
                                      TextMessage message) {
        // Broadcast to all sessions
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(
                    "Echo: " + message.getPayload()));
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, 
                                       CloseStatus status) {
        sessions.remove(session);
        System.out.println("Disconnected: " + session.getId());
    }
}
```

### Using STOMP over WebSocket (Spring + SockJS)

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker
        config.enableSimpleBroker("/topic", "/queue");
        // Application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

```java
@Controller
public class GreetingController {
    
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) {
        return new Greeting("Hello, " + message.getName() + "!");
    }
}
```

**Client-Side (STOMP over SockJS):**
```js
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    // Subscribe to a topic
    stompClient.subscribe('/topic/greetings', (greeting) => {
        console.log(JSON.parse(greeting.body).content);
    });
    
    // Send a message
    stompClient.send('/app/hello', {}, JSON.stringify({
        name: 'World'
    }));
});
```

---

## 10. WebSocket with Node.js (ws library)

```js
import { WebSocketServer } from 'ws';

const wss = new WebSocketServer({ port: 8080 });

wss.on('connection', (ws, req) => {
    console.log('Client connected');
    
    // Send message to client
    ws.send('Welcome!');
    
    // Receive message from client
    ws.on('message', (data, isBinary) => {
        console.log('Received:', data.toString());
        
        // Echo back
        ws.send(`Echo: ${data}`);
        
        // Broadcast to all clients
        wss.clients.forEach((client) => {
            if (client.readyState === WebSocket.OPEN) {
                client.send(data, { binary: isBinary });
            }
        });
    });
    
    // Handle close
    ws.on('close', () => {
        console.log('Client disconnected');
    });
    
    // Handle errors
    ws.on('error', (error) => {
        console.error('WebSocket error:', error);
    });
    
    // Ping/pong for keep-alive
    ws.isAlive = true;
    ws.on('pong', () => {
        ws.isAlive = true;
    });
});

// Heartbeat interval
const interval = setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.isAlive === false) return ws.terminate();
        ws.isAlive = false;
        ws.ping();
    });
}, 30000);

wss.on('close', () => clearInterval(interval));
```

---

## 11. WebSocket vs SSE (Server-Sent Events)

| Feature         | WebSocket                     | SSE                           |
|-----------------|-------------------------------|-------------------------------|
| Direction       | Bidirectional                 | Server → Client only          |
| Protocol        | ws:// / wss://                | HTTP                           |
| Auto-reconnect  | Must implement manually       | Built-in (EventSource)        |
| Binary data     | Yes                           | No (text only)                 |
| Browser support | All modern browsers           | All modern browsers            |
| Complexity      | Higher                        | Lower                          |
| Firewall        | May be blocked                | Uses standard HTTP             |
| Best for        | Real-time games, chat, collaboration | Live feeds, notifications, stock tickers |

---

## 12. Best Practices & Considerations

### Security
- **Always use `wss://`** in production to encrypt traffic.
- **Validate the `Origin` header** on the server to prevent CSWSH (Cross-Site WebSocket Hijacking).
- **Authenticate during the initial HTTP handshake** (cookies/tokens) — WebSocket has no built-in auth.
- **Implement rate limiting** to prevent DoS attacks.
- **Sanitize all incoming data** — treat it like any untrusted user input.

### Performance
- **Use binary frames** for large data payloads (smaller overhead).
- **Implement compression** (permessage-deflate extension) for text-heavy messages.
- **Use backpressure mechanisms** — don't let slow consumers block the server.
- **Consider connection pooling** and horizontal scaling strategies.

### Scalability
- WebSocket connections are **stateful** — sticky sessions or a shared pub/sub (Redis, Kafka) are needed for horizontal scaling.
- **Use Redis Pub/Sub or a message broker** to broadcast messages across multiple server instances.

### Connection Management
- **Implement heartbeat/ping-pong** to detect and clean up dead connections.
- **Set a maximum message size** to prevent memory exhaustion.
- **Gracefully handle reconnection** on the client side with exponential backoff.

---

## 13. Common Patterns

### Echo Server
Simple bidirectional reflection — useful for testing.

### Pub/Sub with Channels
```
Client A subscribes to "chat:room1"
Client B subscribes to "chat:room1"
Server broadcasts message from A to all subscribers in "chat:room1"
```

### Request-Response over WebSocket
Use correlation IDs to match responses to requests:
```json
// Request
{ "id": "req-1", "action": "getUser", "params": { "userId": 123 } }
// Response
{ "id": "req-1", "action": "getUser", "result": { ... } }
```

---

## 14. Debugging Tools

- **Chrome DevTools** → Network tab → WS filter — inspect frame-by-frame.
- **wscat** — Simple command-line WebSocket client:
  ```bash
  npx wscat -c wss://example.com/ws
  ```
- **Postman** — Supports WebSocket connections for manual testing.
- **WebSocket King** — Browser extension for WebSocket testing.
- **Wireshark** — Deep packet inspection of WebSocket frames.

---

## 15. Key Extensions (RFC 6455)

| Extension                | Purpose                                      |
|--------------------------|----------------------------------------------|
| `permessage-deflate`       | Compress WebSocket message payloads          |
| `x-webkit-deflate-frame`   | Deflate compression per frame (legacy)       |
| `permessage-bzip2`         | Bzip2 compression per message (rare)         |

---

## Summary

WebSocket is the **foundation of real-time web communication** — enabling efficient, bidirectional, low-latency data transfer. Understanding the handshake, frame structure, masking, and lifecycle is essential for building robust real-time applications. Always combine with proper security practices (WSS, origin validation, authentication) and consider scaling via pub/sub systems for production deployments.

---

*Last updated: April 2025*
