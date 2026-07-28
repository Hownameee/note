# 📝 Knowledge & Experience Notes

> **Just a place to take note about knowledge and experience!**

A personal knowledge base covering a wide range of software engineering topics — from authentication and container orchestration to state management, system design, and interview preparation.

---

## 📂 Table of Contents

- [Introduction](#introduction)
- [Repository Structure](#repository-structure)
- [Topic Breakdown](#topic-breakdown)
  - [🔐 Authentication & Identity](#-authentication--identity)
  - [☸️ Kubernetes & Container Orchestration](#️-kubernetes--container-orchestration)
  - [☕ Spring Framework](#-spring-framework)
  - [⚛️ Frontend & State Management](#️-frontend--state-management)
  - [🛠️ DevOps & CI/CD](#️-devops--cicd)
  - [🗄️ API & Gateway](#-api--gateway)
  - [🐳 Docker & Containerization](#-docker--containerization)
  - [📱 Mobile Development](#-mobile-development)
  - [📊 System Monitoring](#-system-monitoring)
  - [🎓 Interview Preparation](#-interview-preparation)
  - [🧠 AI & MCP Practice](#-ai--mcp-practice)
  - [📈 Traceability & Transparency](#-traceability--transparency)
  - [💻 GitHub API & Integrations](#-github-api--integrations)
- [Quick Start](#quick-start)
- [How to Use This Repository](#how-to-use-this-repository)
- [Contributing](#contributing)
- [License](#license)

---

## Introduction

This repository serves as a **centralized knowledge base** — a collection of notes, guides, cheat sheets, and practical exercises accumulated while exploring various technologies and solving real-world problems. It is designed to be both a quick reference for daily work and a deep-dive resource for learning new concepts.

Whether you're setting up **Keycloak SSO**, debugging **Kubernetes networking**, writing a **Spring Boot** backend, or preparing for a **senior backend interview**, you'll likely find something useful here.

---

## Repository Structure

```
.
├── keycloak/           # Keycloak identity & access management
├── kube/               # Kubernetes architecture, setup, and operations
├── spring/             # Spring Framework (IoC, Security, JPA, WebSocket)
├── docker/             # Docker internals and problem-solving
├── github-api/         # GitHub REST & OAuth integration notes
├── auth-github-jwt-demo/ # Full-stack demo: GitHub OAuth2 + JWT Auth
├── unihub-gateway/     # API Gateway project (Spring Cloud Gateway)
├── jenkins/            # Jenkins CI/CD pipeline notes
├── redux/              # Redux state management (core, toolkit, RTK Query)
├── tanstack-query/     # TanStack Query (React Query) core guides
├── mobile/             # Mobile development patterns (ViewModels)
├── sysstat/            # System statistics and monitoring
├── TR/                 # Traceability topics (IR-based, BERT)
├── preparation/        # Interview prep & advanced backend practice
│   ├── advanced_backend_mastery/  # Deep-dive backend concepts
│   ├── advanced_backend_practice/ # Hands-on practice projects
│   └── real_mcp_ai_practice/      # MCP AI integration practice
└── README.md           # This file
```

---

## Topic Breakdown

### 🔐 Authentication & Identity

**Location:** [`keycloak/`](./keycloak/)

Keycloak is an open-source identity and access management solution. This section covers:

- **[Keycloak.md](./keycloak/Keycloak.md)** — Comprehensive guide to setting up and configuring Keycloak.
- **[Standards.md](./keycloak/Standards.md)** — OAuth2, OpenID Connect (OIDC), and SAML standards explained.
- **[realm-export.json](./keycloak/realm-export.json)** — Sample realm configuration for quick import.
- **[compose.yaml](./keycloak/compose.yaml)** — Docker Compose setup to run Keycloak locally.
- **client/** — OAuth2 client configuration examples.

**Key Topics:** OAuth2, OIDC, SSO, Realm Configuration, User Federation, Token Exchange

---

### ☸️ Kubernetes & Container Orchestration

**Location:** [`kube/`](./kube/)

Everything from basic concepts to deep internals:

| File | Description |
|------|-------------|
| **[k8s_introduction.md](./kube/k8s_introduction.md)** | What is Kubernetes and why use it |
| **[k8s_architecture_overview.md](./kube/k8s_architecture_overview.md)** | Control plane, nodes, and core components |
| **[k8s_internals_and_networking.md](./kube/k8s_internals_and_networking.md)** | Networking models, CNI, service mesh |
| **[k8s_setup_and_theory.md](./kube/k8s_setup_and_theory.md)** | Cluster setup, kubeadm, and theory |
| **[kubectl_cheat_sheet.md](./kube/kubectl_cheat_sheet.md)** | Quick reference for daily kubectl commands |
| **[k8s_reset_commands.md](./kube/k8s_reset_commands.md)** | How to reset/clean up a cluster |
| **[networking_basics.md](./kube/networking_basics.md)** | Pod networking, Services, Ingress |
| **[docker_swarm_overview.md](./kube/docker_swarm_overview.md)** | Comparison with Kubernetes |
| **[nginx_pod_example.yaml](./kube/nginx_pod_example.yaml)** | Example Pod manifest |
| **[pod_advanced_options_template.yaml](./kube/pod_advanced_options_template.yaml)** | Advanced Pod spec template |
| **config/** | Kubernetes config samples |
| **service-mesh/** | Istio/Linkerd service mesh notes |

**Visual Diagrams:** Architecture diagrams for container runtime, deployment objects, and general K8s architecture are included as PNG files.

**Key Topics:** Pods, Deployments, Services, Ingress, CNI, Service Mesh, RBAC, Helm

---

### ☕ Spring Framework

**Location:** [`spring/`](./spring/)

In-depth Spring Boot and Spring Framework notes:

```
spring/
├── security/       # Spring Security, OAuth2 resource server, method security
├── data-access/    # JPA, Hibernate, transactions, N+1 problem
├── ioc/            # Dependency injection, IoC container, bean lifecycle
├── websocket/      # WebSocket, STOMP, real-time messaging
└── integer-mapping-demo/  # Demo project for integer persistence mapping
```

**Key Topics:** IoC/AOP, Spring Security, JPA/Hibernate, WebSocket, Transaction Management

---

### ⚛️ Frontend & State Management

#### Redux

**Location:** [`redux/`](./redux/)

Structured learning path from core concepts to advanced patterns:

| File | Description |
|------|-------------|
| **[1-core-redux.md](./redux/1-core-redux.md)** | Redux fundamentals: store, actions, reducers, middleware |
| **[2-redux-toolkit.md](./redux/2-redux-toolkit.md)** | Modern Redux with Redux Toolkit (configureStore, createSlice) |
| **[3-react-redux.md](./redux/3-react-redux.md)** | Connecting Redux to React (useSelector, useDispatch) |
| **[4-rtk-query.md](./redux/4-rtk-query.md)** | Data fetching and caching with RTK Query |
| **[5-practice-guide.md](./redux/5-practice-guide.md)** | Guided practice exercises |
| **[6-exercises.md](./redux/6-exercises.md)** | Additional exercises for mastery |
| **demo/** | Example Redux application code |

#### TanStack Query (React Query)

**Location:** [`tanstack-query/`](./tanstack-query/)

- **[query-core-guides.md](./tanstack-query/query-core-guides.md)** — Core concepts: queries, mutations, caching, infinite queries, and devtools.

**Key Topics:** Redux Core, Redux Toolkit, RTK Query, React Query, Caching Strategies

---

### 🛠️ DevOps & CI/CD

**Location:** [`jenkins/`](./jenkins/)

- **[docker-agent.md](./jenkins/docker-agent.md)** — Running Jenkins agents inside Docker containers.
- **save/** — Saved Jenkins pipeline configurations.

**Key Topics:** Pipeline as Code, Docker Agents, Shared Libraries

---

### 🗄️ API & Gateway

**Location:** [`unihub-gateway/`](./unihub-gateway/)

A practical API Gateway implementation using Spring Cloud Gateway:

```
unihub-gateway/
├── gateway/              # Gateway service (routing, filtering)
├── backend/              # Backend services behind the gateway
└── docker-compose.yaml   # Multi-service orchestration
```

**Key Topics:** Spring Cloud Gateway, Route Configuration, Filter Chains, Load Balancing

---

### 🐳 Docker & Containerization

**Location:** [`docker/`](./docker/)

- **[copyon-write-probelm.md](./docker/copyon-write-probelm.md)** — Deep dive into Docker's copy-on-write (CoW) filesystem layer mechanism and common pitfalls.

**Key Topics:** Union Filesystem, Layer Caching, CoW Strategy, Image Optimization

---

### 📱 Mobile Development

**Location:** [`mobile/`](./mobile/)

- **[viewmodel.md](./mobile/viewmodel.md)** — ViewModel pattern in mobile development (Android). Covers lifecycle awareness, state management, and best practices.

**Key Topics:** MVVM, Android Architecture Components, StateFlow, LiveData

---

### 📊 System Monitoring

**Location:** [`sysstat/`](./sysstat/)

- **[sysstat.md](./sysstat/sysstat.md)** — Guide to using `sysstat` utilities (`sar`, `iostat`, `mpstat`, `pidstat`) for Linux performance monitoring and troubleshooting.

**Key Topics:** CPU/Memory/Disk/Network Monitoring, Performance Analysis, Bottleneck Detection

---

### 🎓 Interview Preparation

**Location:** [`preparation/`](./preparation/)

#### Advanced Backend Mastery

**Location:** [`preparation/advanced_backend_mastery/`](./preparation/advanced_backend_mastery/)

A structured roadmap and deep-dive into backend engineering topics:

| File | Topic |
|------|-------|
| **[00_Roadmap.md](./preparation/advanced_backend_mastery/00_Roadmap.md)** | Learning roadmap for senior backend engineering |
| **[01_Java_Memory_PassByValue_Mutability.md](./preparation/advanced_backend_mastery/01_Java_Memory_PassByValue_Mutability.md)** | Java memory model, pass-by-value, immutability |
| **[02_Functional_Interfaces_Streams.md](./preparation/advanced_backend_mastery/02_Functional_Interfaces_Streams.md)** | Functional programming in Java |
| **[03_Multithreading_VirtualThreads.md](./preparation/advanced_backend_mastery/03_Multithreading_VirtualThreads.md)** | Concurrency, virtual threads (Project Loom) |
| **[04_Authentication_JWT_SSO_Keycloak.md](./preparation/advanced_backend_mastery/04_Authentication_JWT_SSO_Keycloak.md)** | Auth patterns: JWT, SSO, Keycloak |
| **[05_SpringBoot_IoC_AOP.md](./preparation/advanced_backend_mastery/05_SpringBoot_IoC_AOP.md)** | Spring IoC container and AOP |
| **[06_AI_Integration_MCP.md](./preparation/advanced_backend_mastery/06_AI_Integration_MCP.md)** | AI integration with MCP (Model Context Protocol) |
| **[07_JPA_NPlus1_Query.md](./preparation/advanced_backend_mastery/07_JPA_NPlus1_Query.md)** | JPA N+1 problem and query optimization |
| **[08_SpringBoot_Autowired_DeepDive.md](./preparation/advanced_backend_mastery/08_SpringBoot_Autowired_DeepDive.md)** | Deep dive into `@Autowired` |
| **[09_JPA_ReadOnly_Transactional.md](./preparation/advanced_backend_mastery/09_JPA_ReadOnly_Transactional.md)** | Read-only transactions in JPA |
| **[10_DTO_Mapping_Backend_Frontend.md](./preparation/advanced_backend_mastery/10_DTO_Mapping_Backend_Frontend.md)** | DTO design and mapping strategies |

#### Advanced Backend Practice

**Location:** [`preparation/advanced_backend_practice/`](./preparation/advanced_backend_practice/)

Hands-on practice projects:

- **Spring Boot + JWT + OAuth2** backend project with PostgreSQL
- Docker Compose setup for local development
- Realm export for Keycloak
- **[PRACTICE_GUIDE.md](./preparation/advanced_backend_practice/PRACTICE_GUIDE.md)** — Step-by-step instructions

---

### 🧠 AI & MCP Practice

**Location:** [`preparation/real_mcp_ai_practice/`](./preparation/real_mcp_ai_practice/)

Practical exploration of the Model Context Protocol (MCP):

```
real_mcp_ai_practice/
├── mcp-server/              # MCP server implementation
├── mcp-chatbot-client/      # Chatbot client using MCP
└── PRACTICE_GUIDE.md        # Setup and usage guide
```

**Key Topics:** MCP Protocol, AI Integration, Tool Calling, Context Management

---

### 📈 Traceability & Transparency

**Location:** [`TR/`](./TR/)

Research and notes on traceability in software engineering:

| File | Description |
|------|-------------|
| **[ir_based_traceability_summary.md](./TR/ir_based_traceability_summary.md)** | Information Retrieval (IR) based traceability between artifacts |
| **[bert_traceability_summary.md](./TR/bert_traceability_summary.md)** | Using BERT for deep-learning-based traceability |
| **[transarc_summary.md](./TR/transarc_summary.md)** | TransArC architecture for traceability |

**Key Topics:** Requirements Traceability, IR Models, Deep Learning, BERT

---

### 💻 GitHub API & Integrations

**Location:** [`github-api/`](./github-api/)

Notes on using GitHub's REST API and OAuth flows:

| File | Description |
|------|-------------|
| **[github_http_structure.md](./github-api/github_http_structure.md)** | GitHub REST API request/response structure |
| **[github_api_user_repos.md](./github-api/github_api_user_repos.md)** | Fetching user repositories |
| **[github_oauth_token_guide.md](./github-api/github_oauth_token_guide.md)** | OAuth token flow for app integration |
| **token-app/** | Sample app for token-based GitHub API access |

#### Full-Stack Demo: GitHub OAuth2 + JWT Auth

**Location:** [`auth-github-jwt-demo/`](./auth-github-jwt-demo/)

A complete end-to-end authentication demo:

1. **React** frontend shows a \"Continue with GitHub\" button
2. Backend (**Spring Boot**) orchestrates the GitHub OAuth2 flow
3. On success, the backend issues a **JWT** stored in an HttpOnly cookie
4. Subsequent API requests are authenticated via the JWT cookie

**Tech Stack:** React, Spring Boot, Spring Security OAuth2, H2 Database, JWT

---

## Quick Start

```bash
# Clone the repository
git clone <repo-url>
cd note

# Explore a topic — for example, Redux:
cat redux/1-core-redux.md

# Or run the Keycloak demo:
cd keycloak
docker compose up -d

# Or run the auth demo (requires GitHub OAuth App):
cd auth-github-jwt-demo/backend
cp .env.example .env
# Fill in your GitHub credentials
./gradlew bootRun
```

---

## How to Use This Repository

1. **Browse by Topic** — Each major topic has its own directory with a clear structure.
2. **Follow the Roadmap** — The `preparation/advanced_backend_mastery/00_Roadmap.md` provides a structured learning path.
3. **Run Demos** — Several directories contain runnable projects (Docker Compose, Spring Boot, React).
4. **Reference Daily** — Cheat sheets like `kubectl_cheat_sheet.md` are designed for quick terminal lookups.
5. **Search** — Use `grep -r \"keyword\" .` to find specific topics across all notes.

---

## Contributing

This is a personal knowledge base, but suggestions, corrections, and additions are welcome! Feel free to open issues or submit pull requests.

---

## License

This project is for personal knowledge sharing and educational purposes.
