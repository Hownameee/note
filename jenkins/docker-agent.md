# Troubleshooting: Node Deadlock with Docker Agents

## The Problem: Executor Starvation

By default, the number of executors is typically set to match the number of CPU cores. While this prevents system resource exhaustion (RAM/CPU), it creates a logic bottleneck when using multi-stage or Docker-based pipelines.

## Why the Deadlock Occurs

When a pipeline uses a Docker agent, it often requires **two executor slots** to proceed:

1. **Slot 1 (Flyweight/Heavyweight):** Used by the main pipeline script to coordinate the build.
2. **Slot 2 (Docker Agent):** Requested when the script enters the `agent { docker ... }` block.

**Scenario:** If a node has **4 executors** and **4 jobs** start at the same time:

* Each job claims **1 executor** to start the controller script.
* The node is now at 100% capacity (4/4 executors used).
* Each job then attempts to spin up its Docker container and waits for a **free executor**.
* **Result:** A "circular wait" deadlock occurs. No job can finish because no executors are free, and no executors will become free until a job finishes.

---

## Recommended Solutions

### 1. The Executor Buffer (Immediate Fix)

Increase the executor count to **CPU Cores + 1 (or 2)**.

* **Benefit:** Provides a "buffer" slot that allows at least one job to acquire its second required executor, complete its task, and break the deadlock cycle.
* **Risk:** Monitor RAM usage closely, as this allows more concurrent processes than the CPU core count originally intended.

### 2. Throttling & Resource Locking

* **Throttle Concurrent Builds:** Limit the specific job to only 2 or 3 concurrent instances to ensure there is always a spare slot on the node.
* **Lockable Resources Plugin:** Wrap the Docker agent block in a `lock` to manage the queue programmatically.

### 3. Shift to Dynamic Provisioning

Use the **Jenkins Docker or Kubernetes Cloud** plugins. Instead of relying on a fixed number of local executors, the system provisions an external container for the agent, bypassing the local executor limit entirely.
