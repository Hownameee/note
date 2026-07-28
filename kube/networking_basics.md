# Computer Network

## OSI Network Layers for DevOps

[Image of OSI model layers]

### Data Link (Layer 2)

* **Think of this as:** The physical street addresses of houses on the same block.
* Layer 2 is concerned purely with moving data across a **local physical link**. It does not know what the internet is, and it does not know what an IP address is.
* **The Address:** It uses **MAC addresses** (Media Access Control), which are hardcoded into physical network cards (e.g., `00:1A:2B:3C:4D:5E`).
* **The Devices:** **Switches and Bridges** operate here. A switch literally just looks at a frame of data, sees the destination MAC address, and forwards it to the correct cable port.
* **In Kubernetes:** The virtual network bridge we talked about earlier (like `cbr0` or `docker0`) is a Layer 2 device. It blindly passes traffic between Pods on the exact same server using their MAC addresses.

### Network Layer (Layer 3)

* **Think of this as:** The Zip Codes and City names that allow a letter to travel from New York to Tokyo.
* Layer 3 introduces the concept of **logical routing** across multiple, different networks.
* **The Address:** It uses **IP addresses** (e.g., `192.168.1.5` or `10.96.0.1`).
* **The Devices:** **Routers** operate here. A router looks at an IP address and says, "This isn't for my local neighborhood; I need to send this to the next city router."
* **In Linux:** `iptables` (the Linux firewall) is primarily a Layer 3 tool. It looks at the Source IP and Destination IP of a packet to decide whether to block it, allow it, or translate it (NAT).

### Transport (Layer 4)

* **Think of this as:** The name of the specific person inside the house who the letter is for.
* Once Layer 3 gets the data to the correct computer, Layer 4 figures out **which application** on that computer should receive it.
* **The Concept:** It uses **TCP / UDP Protocols** and **Ports**.
* **Example:** A web server listens on Port 80, while an SSH server listens on Port 22. Layer 4 ensures the web traffic goes to the web server and not the SSH server.

---

## Bridge

Historically, a hardware bridge was used to connect two separate network segments (like two different physical wires of computers) to make them act like one big network.

**How it works:**

* Imagine you have two separate neighborhoods (Network Segment A and Network Segment B) connected by a single literal bridge.
* The Bridge has only two ports: Port 1 goes to Segment A, Port 2 goes to Segment B.
* The Bridge "listens" to all the traffic on both sides and builds a table in its memory of which MAC addresses live on which side.
* **Filtering:** If Computer 1 (on Segment A) sends a message to Computer 2 (also on Segment A), the Bridge sees the traffic, realizes both computers are on the same side, and **blocks** the traffic from crossing the bridge. This keeps Segment B from being flooded with irrelevant traffic.
* **Forwarding:** If Computer 1 (Segment A) sends a message to Computer 3 (Segment B), the Bridge **allows** the frame to cross over.

**The catch:** Hardware bridges were mostly processed via software (the CPU of the bridge), making them relatively slow.

---

## Why Do DevOps Engineers Care? (The Virtual Bridge)

Because in the world of Linux, Docker, and Kubernetes, we use **Virtual Software Bridges**.

When you install Docker on a Linux server, Docker creates a virtual network interface called `docker0`.

* `docker0` is a **Linux Bridge**.
* It acts exactly like a physical hardware switch, but it exists entirely in the server's RAM and CPU.
* When you spin up 5 Docker containers, Linux creates a virtual network cable (a `veth` pair) for each container and "plugs" them all into the `docker0` bridge.
* This is exactly how your containers on the same host can ping each other using their local IP/MAC addresses without the traffic ever leaving the physical server!
