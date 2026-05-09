# Service Mesh

A **service mesh** is an infrastructure layer that manages service-to-service communication in a microservices system. It handles network concerns such as security, traffic routing, retries, timeouts, observability, and policy enforcement outside the application code.

In Kubernetes, a service mesh usually works by deploying a proxy next to each application workload. The application sends traffic through the proxy, and the proxy applies the mesh configuration before forwarding the request.

## Why Service Mesh Exists

In a microservice architecture, one user request may pass through many services:

```text
frontend -> user-service -> payment-service -> notification-service
```

Without a service mesh, every service usually needs to implement the same non-business logic:

- Secure communication between services.
- Retry logic when another service is temporarily unavailable.
- Timeout and circuit breaker behavior.
- Metrics collection.
- Distributed tracing.
- Access control between services.
- Traffic splitting for deployments.
- Consistent logging and request metadata.

This creates several problems:

- The same networking logic is duplicated across services.
- Different teams may implement retries, timeouts, and metrics differently.
- Security inside the cluster is often weak because services can talk to each other freely.
- Developers spend time on infrastructure concerns instead of business logic.
- Operations teams have limited centralized control over service communication.

A service mesh solves this by moving communication logic out of the application and into a dedicated proxy layer.

## Core Idea

Instead of putting networking logic directly inside every application, the mesh adds a proxy beside each service instance.

```text
Service A container -> local proxy -> network -> local proxy -> Service B container
```

The application still calls another service normally, but the request is intercepted and handled by the proxy.

The proxy can then:

- Encrypt traffic.
- Check service identity.
- Apply routing rules.
- Retry failed requests.
- Enforce timeouts.
- Collect metrics.
- Emit traces.
- Deny traffic that violates policy.

This proxy is usually called a **sidecar proxy** because it runs next to the application container in the same Kubernetes Pod.

## Data Plane And Control Plane

A service mesh is usually split into two main parts.

### Data Plane

The **data plane** is the set of proxies that directly handle application traffic.

Responsibilities:

- Intercept inbound and outbound service traffic.
- Apply routing, retry, timeout, and security policies.
- Collect telemetry such as latency, request count, and error rate.
- Enforce mTLS and authorization rules.

Example:

```text
Pod A: app container + proxy
Pod B: app container + proxy
Pod C: app container + proxy
```

Each proxy is part of the data plane.

### Control Plane

The **control plane** manages and configures the proxies.

Responsibilities:

- Discover services and endpoints.
- Generate proxy configuration.
- Distribute traffic policies to proxies.
- Issue and rotate certificates.
- Manage service identities.
- Provide APIs through Kubernetes custom resources.

In Istio, the main control plane component is **istiod**.

## Common Service Mesh Features

### 1. Mutual TLS

Mutual TLS, or **mTLS**, encrypts traffic between services and verifies both sides of the connection.

Normal TLS usually verifies only the server:

```text
client -> verifies server
```

mTLS verifies both client and server:

```text
client <-> server
```

Benefits:

- Traffic inside the cluster is encrypted.
- Services get strong identities.
- Unauthorized services can be blocked.
- Credentials can be rotated automatically.

This is important because Kubernetes networking is often flat by default. If no network policy or mesh policy exists, many services may be able to reach each other.

### 2. Traffic Management

A service mesh can control how traffic flows between service versions.

Common patterns:

- **Canary deployment**: send a small percentage of traffic to a new version.
- **Blue-green deployment**: switch traffic from old version to new version.
- **A/B testing**: route traffic based on headers, users, or regions.
- **Traffic mirroring**: copy production traffic to another service without affecting users.
- **Failover**: route traffic to another service or region when one fails.

Example canary rollout:

```text
90% traffic -> product-service v1
10% traffic -> product-service v2
```

This allows teams to test a new version with real traffic before sending all users to it.

### 3. Resilience

Service mesh can improve reliability by applying consistent failure handling.

Examples:

- **Timeouts**: stop waiting after a configured time.
- **Retries**: retry failed requests when the failure is temporary.
- **Circuit breaking**: stop sending traffic to an unhealthy service.
- **Outlier detection**: remove unhealthy service instances from load balancing.
- **Rate limiting**: protect services from too much traffic.

Important note: retries must be configured carefully. Too many retries can increase load and make an outage worse.

### 4. Observability

Because all service traffic passes through proxies, the mesh can collect useful telemetry without changing application code.

Common telemetry:

- Request count.
- Success rate.
- Error rate.
- Latency percentiles.
- Source service and destination service.
- HTTP status codes.
- TCP connection metrics.
- Distributed tracing metadata.

This helps answer questions such as:

- Which service is slow?
- Which service is returning errors?
- Which version receives traffic?
- Which service calls another service?
- What is the request path through the system?

Popular tools used with service mesh:

- Prometheus for metrics.
- Grafana for dashboards.
- Jaeger or Zipkin for tracing.
- Kiali for Istio topology visualization.

### 5. Authorization Policy

Service mesh can enforce which services are allowed to communicate.

Example:

```text
frontend can call user-service
user-service can call payment-service
frontend cannot call database directly
```

This is useful because authorization is based on service identity, not only IP addresses.

## Kubernetes And Service Mesh

Kubernetes provides basic service discovery and load balancing through Services.

Example:

```text
user-service.default.svc.cluster.local
```

However, Kubernetes Services do not automatically provide:

- mTLS between services.
- Fine-grained traffic splitting.
- Retries and circuit breaking.
- Request-level authorization.
- Distributed tracing.
- Per-service traffic policies.

A service mesh builds these capabilities on top of Kubernetes networking.

## Istio Example

Istio is one of the most common Kubernetes service mesh implementations.

Important Istio components:

- **Envoy proxy**: the sidecar proxy that handles traffic.
- **istiod**: the control plane component.
- **Pilot**: service discovery and traffic configuration logic, now part of istiod.
- **Citadel**: certificate and identity logic, now part of istiod.
- **Galley**: configuration validation logic, now part of istiod.
- **CRDs**: Kubernetes custom resources used to configure the mesh.

Modern Istio mostly consolidates control plane responsibilities inside `istiod`.

## Istio Configuration Resources

Istio is configured through Kubernetes YAML files using custom resources.

Common resources:

- **Gateway**: configures traffic entering or leaving the mesh.
- **VirtualService**: defines routing rules.
- **DestinationRule**: defines policies for traffic after routing, such as subsets and load balancing.
- **PeerAuthentication**: configures mTLS mode.
- **AuthorizationPolicy**: controls which identities can access a workload.
- **ServiceEntry**: adds external services to the mesh registry.
- **Sidecar**: controls proxy configuration scope.
- **Telemetry**: configures metrics, access logs, and tracing behavior.
- **EnvoyFilter**: applies low-level Envoy configuration when built-in Istio APIs are not enough.
- **WasmPlugin**: extends proxy behavior using WebAssembly plugins.

## More Istio Features

### Ingress Gateway

An **ingress gateway** manages traffic entering the mesh from outside the cluster.

Example:

```text
internet/client -> load balancer -> Istio ingress gateway -> internal service
```

Ingress gateways are useful for:

- Exposing HTTP, HTTPS, TCP, or TLS services.
- Terminating TLS at the edge.
- Routing external traffic to internal services.
- Applying the same Istio routing model at the edge of the mesh.

In Istio, a `Gateway` usually defines the exposed ports and TLS settings. A `VirtualService` then defines where requests should go.

Example:

```yaml
apiVersion: networking.istio.io/v1
kind: Gateway
metadata:
  name: public-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - app.example.com
```

Then bind a route to that gateway:

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: frontend
spec:
  hosts:
    - app.example.com
  gateways:
    - public-gateway
  http:
    - route:
        - destination:
            host: frontend
            port:
              number: 8080
```

### Egress Gateway

An **egress gateway** controls traffic leaving the mesh.

Example:

```text
service -> sidecar proxy -> egress gateway -> external API
```

Egress gateways are useful when:

- External traffic must pass through a controlled point.
- Security teams need audit logs for outbound calls.
- Only specific services should call external domains.
- Outbound traffic needs fixed source IP addresses.
- TLS origination should happen at the mesh edge.

Example external dependencies:

```text
payment-service -> api.stripe.com
notification-service -> smtp.example.com
```

Without egress control, any service may be able to call external systems directly. With egress policies, the platform can explicitly allow or deny outbound destinations.

### ServiceEntry

`ServiceEntry` adds a service that is not part of the Kubernetes service registry into the mesh.

It is commonly used for external services.

Example:

```yaml
apiVersion: networking.istio.io/v1
kind: ServiceEntry
metadata:
  name: external-payment-api
spec:
  hosts:
    - api.payment.example.com
  ports:
    - number: 443
      name: https
      protocol: HTTPS
  resolution: DNS
  location: MESH_EXTERNAL
```

After this, Istio can apply traffic policy, telemetry, and egress controls to calls to `api.payment.example.com`.

### TLS Origination

**TLS origination** means the application sends plain HTTP to its local proxy, and the proxy upgrades the outbound connection to HTTPS.

Example:

```text
application -- HTTP --> local proxy -- HTTPS --> external service
```

This can simplify application code because the mesh owns external TLS settings. It can also centralize certificate and TLS policy.

### Fault Injection

Fault injection intentionally adds failures or delays to test how services behave.

Common fault types:

- Delay: make a request slower.
- Abort: return an error response.

Example use cases:

- Test whether the frontend handles a slow backend.
- Verify that retries and timeouts are configured correctly.
- Practice failure scenarios before production incidents happen.

Example:

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: user-service
spec:
  hosts:
    - user-service
  http:
    - fault:
        delay:
          percentage:
            value: 10
          fixedDelay: 2s
      route:
        - destination:
            host: user-service
```

This delays 10% of requests by 2 seconds.

### Request Matching And Header-Based Routing

Istio can route traffic based on HTTP request properties.

Examples:

- Path.
- Headers.
- Method.
- Query parameters.
- Authority or host.

Example: route beta users to `v2`.

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: product-service
spec:
  hosts:
    - product-service
  http:
    - match:
        - headers:
            x-user-group:
              exact: beta
      route:
        - destination:
            host: product-service
            subset: v2
    - route:
        - destination:
            host: product-service
            subset: v1
```

This is useful for beta testing, internal users, region-based experiments, or mobile app version migrations.

### Traffic Mirroring

Traffic mirroring copies live traffic to another service version without affecting the real user response.

Example:

```text
real request -> service v1
copied request -> service v2
```

Use cases:

- Test a new service version with real traffic.
- Compare behavior between old and new implementations.
- Warm up a new service before release.
- Validate migration from one backend to another.

Important: mirrored traffic should not perform unsafe side effects such as duplicate payments, emails, or database writes unless the receiving service is isolated.

### Circuit Breaking And Outlier Detection

Circuit breaking protects a service from overload by limiting connections or requests.

Outlier detection removes unhealthy instances from load balancing when they return too many errors.

Example:

```yaml
apiVersion: networking.istio.io/v1
kind: DestinationRule
metadata:
  name: payment-service
spec:
  host: payment-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 1m
```

This helps prevent one unhealthy service instance from receiving traffic forever.

### Locality Load Balancing

Locality load balancing prefers nearby service instances.

Example:

```text
service in zone-a -> prefer service pods in zone-a
service in zone-b -> prefer service pods in zone-b
```

Benefits:

- Lower latency.
- Lower cross-zone network cost.
- Better fault isolation.

This matters more in large clusters, multi-zone clusters, and multi-region service mesh setups.

### Multi-Cluster Mesh

Istio can connect services across multiple Kubernetes clusters.

Example:

```text
cluster-a/frontend -> cluster-b/payment-service
```

Common reasons to use multi-cluster mesh:

- High availability across clusters.
- Gradual migration between clusters.
- Regional traffic routing.
- Shared services across environments.
- Disaster recovery.

Multi-cluster service mesh is powerful, but it adds operational complexity around networking, DNS, certificates, trust domains, and failure handling.

### Gateway API Support

Istio supports the Kubernetes **Gateway API** in addition to Istio's own `Gateway` and `VirtualService` APIs.

Gateway API resources include:

- `GatewayClass`
- `Gateway`
- `HTTPRoute`
- `TCPRoute`
- `TLSRoute`
- `GRPCRoute`

The Gateway API is intended to provide a more standard Kubernetes-native way to configure traffic routing.

Example shape:

```text
GatewayClass -> Gateway -> HTTPRoute -> Service
```

This can make routing configuration more portable across different Kubernetes networking implementations.

### Telemetry API

Istio's `Telemetry` resource controls how metrics, logs, and traces are generated.

Telemetry can be configured at different levels:

- Mesh-wide.
- Namespace-wide.
- Workload-specific.

Example:

```yaml
apiVersion: telemetry.istio.io/v1
kind: Telemetry
metadata:
  name: namespace-telemetry
  namespace: production
spec:
  accessLogging:
    - providers:
        - name: envoy
```

Telemetry configuration is useful when:

- Some namespaces need access logs and others do not.
- Trace sampling should be different per workload.
- Metrics labels need to be customized.
- Noisy workloads need reduced telemetry volume.

### EnvoyFilter

`EnvoyFilter` allows advanced low-level customization of Envoy proxy configuration.

Use cases:

- Add custom HTTP filters.
- Modify listener behavior.
- Add advanced proxy behavior not exposed by normal Istio APIs.

This is powerful but risky.

Prefer standard Istio resources first:

- `VirtualService`
- `DestinationRule`
- `Gateway`
- `AuthorizationPolicy`
- `Telemetry`

Use `EnvoyFilter` only when the standard APIs cannot express the required behavior.

### WebAssembly Plugins

Istio can extend Envoy behavior with WebAssembly through `WasmPlugin`.

Possible use cases:

- Custom authentication logic.
- Request or response modification.
- Custom telemetry generation.
- Policy checks.

This allows platform teams to add proxy behavior without changing application code.

### Ambient Mesh

Traditional Istio uses sidecar proxies. Each application Pod gets its own Envoy proxy.

Ambient mesh is a newer Istio data plane mode that can add mesh features without injecting a sidecar into every Pod.

Ambient mode uses two layers:

- **ztunnel**: a per-node proxy that handles L4 features such as mTLS, identity, L4 authorization, and L4 telemetry.
- **waypoint proxy**: an optional Envoy proxy that handles L7 features such as HTTP routing, retries, rich authorization, tracing, and access logs.

Basic shape:

```text
workload -> node ztunnel -> node ztunnel -> workload
```

With L7 features:

```text
workload -> ztunnel -> waypoint proxy -> destination workload
```

Sidecar mode:

```text
one Envoy proxy per application Pod
```

Ambient mode:

```text
one ztunnel per node, optional waypoint proxies for L7 policy
```

Benefits of ambient mode:

- No sidecar container inside every application Pod.
- Lower per-Pod resource overhead.
- Easier onboarding for existing workloads.
- L4 security can be adopted first.
- L7 features can be added only where needed.

Tradeoffs:

- Different operational model from sidecar mode.
- Some features require waypoint proxies.
- Teams must understand where policy is enforced: ztunnel for L4, waypoint for L7.

Example namespace label:

```bash
kubectl label namespace production istio.io/dataplane-mode=ambient
```

Example waypoint enrollment:

```bash
istioctl waypoint apply -n production --enroll-namespace
```

### Sidecar Mode vs Ambient Mode

| Area | Sidecar Mode | Ambient Mode |
| --- | --- | --- |
| Proxy placement | Envoy inside each application Pod | ztunnel per node, optional waypoint proxies |
| Pod modification | Requires sidecar injection | Does not require sidecar injection |
| L4 security | Envoy sidecar | ztunnel |
| L7 routing and policy | Envoy sidecar | waypoint proxy |
| Resource model | Per workload Pod proxy | Shared node proxy plus optional L7 proxy |
| Adoption style | Workloads are injected | Namespace or workload enrollment |

### Policy Layers

Istio policies can be thought of in layers.

Authentication:

- Who is the caller?
- Is traffic using mTLS?
- Which identity does this workload have?

Authorization:

- Is this caller allowed to call this destination?
- Is this request path allowed?
- Is this HTTP method allowed?

Traffic management:

- Which version receives the request?
- Should requests be retried?
- How long should the proxy wait?
- Should traffic be mirrored?

Telemetry:

- Which metrics are emitted?
- Are access logs enabled?
- What percentage of requests are traced?

Thinking in layers makes Istio easier to debug because each layer answers a different question.

## Example: Traffic Splitting

Assume there are two versions of a service:

```yaml
version: v1
version: v2
```

Traffic can be split like this:

```yaml
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: product-service
spec:
  hosts:
    - product-service
  http:
    - route:
        - destination:
            host: product-service
            subset: v1
          weight: 90
        - destination:
            host: product-service
            subset: v2
          weight: 10
```

The subsets are usually defined in a `DestinationRule`:

```yaml
apiVersion: networking.istio.io/v1
kind: DestinationRule
metadata:
  name: product-service
spec:
  host: product-service
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```

## Example: Enforce mTLS

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT
```

This requires workloads in the namespace to communicate through mTLS.

## Example: Authorization Policy

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-frontend-to-user-service
  namespace: production
spec:
  selector:
    matchLabels:
      app: user-service
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/production/sa/frontend
```

This allows only the `frontend` service account in the `production` namespace to call workloads labeled `app: user-service`.

## Sidecar Injection

In Istio, the Envoy sidecar can be added automatically when a namespace is labeled for injection.

Example:

```bash
kubectl label namespace production istio-injection=enabled
```

After that, new Pods created in the namespace receive an Envoy sidecar automatically.

The sidecar usually contains:

- Application container.
- Envoy proxy container.
- Init container or CNI logic to redirect traffic through Envoy.

## Request Flow In Istio

Example request from `frontend` to `user-service`:

```text
frontend app
  -> frontend Envoy sidecar
  -> user-service Envoy sidecar
  -> user-service app
```

During this flow:

1. The frontend application sends a request to `user-service`.
2. Local networking rules redirect the request to the frontend Envoy proxy.
3. Envoy checks routing rules and security configuration.
4. Envoy opens an mTLS connection to the destination Envoy proxy.
5. The destination Envoy validates the caller identity.
6. Authorization policy is enforced.
7. The request is forwarded to the user-service application container.
8. Metrics and traces are emitted by both proxies.

## Benefits

- Centralized control over service communication.
- Less duplicate networking code in services.
- Better security with mTLS and identity-based policy.
- Better observability across services.
- Safer deployments through traffic splitting.
- Consistent retries, timeouts, and load balancing.
- Platform teams can manage network behavior without changing application code.

## Tradeoffs

Service mesh also adds complexity.

Common tradeoffs:

- More components to install and operate.
- More resource usage because each workload has a proxy.
- More configuration to understand.
- Harder debugging when traffic behavior is controlled by mesh rules.
- Possible latency overhead from proxy hops.
- Incorrect retry or timeout settings can cause production issues.

Because of this, service mesh is most useful when a system has many services, strong security needs, complex traffic management, or mature platform operations.

For small applications, Kubernetes Services, Ingress, NetworkPolicy, and application-level metrics may be enough.

## Service Mesh vs API Gateway

An API gateway and a service mesh are related but solve different problems.

| Area | API Gateway | Service Mesh |
| --- | --- | --- |
| Main traffic | External client to internal services | Service to service |
| Scope | North-south traffic | East-west traffic |
| Users | External clients, mobile apps, browsers | Internal services |
| Features | Authentication, routing, rate limits, request transformation | mTLS, service identity, retries, observability, traffic policy |
| Location | Edge of the system | Inside the cluster |

Many systems use both:

```text
client -> API gateway -> service mesh -> internal services
```

## When To Use Service Mesh

Use service mesh when:

- There are many microservices communicating with each other.
- Internal service traffic must be encrypted.
- Teams need consistent retries, timeouts, and observability.
- Deployments need canary traffic splitting.
- Security requires identity-based service access control.
- Platform teams need centralized traffic policy.

Avoid or delay service mesh when:

- The system has only a few services.
- The team is not ready to operate the added complexity.
- Basic Kubernetes networking is enough.
- Most traffic control can be handled at the application or gateway layer.

## Summary

Service mesh extracts non-business networking logic from applications and places it into a managed proxy layer. In Kubernetes, this usually means each workload gets a sidecar proxy, while a control plane configures those proxies.

Istio uses Envoy as the data plane and `istiod` as the control plane. It is commonly configured through Kubernetes CRDs such as `VirtualService`, `DestinationRule`, `PeerAuthentication`, and `AuthorizationPolicy`.

The main value of a service mesh is secure, observable, and controllable communication between services.
