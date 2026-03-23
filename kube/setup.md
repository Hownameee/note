# Setup Kubernetes

## Install Kubernetes

### Both Nodes

```bash
# 1. Disable swap
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

# 2. Load kernel modules
sudo modprobe overlay
sudo modprobe br_netfilter

# 3. Apply sysctl params
sudo tee /etc/sysctl.d/kubernetes.conf <<EOF
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables  = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system

# 4. Install containerd
sudo apt install -y containerd

# 5. Configure containerd defaults
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml

# 6. Enable SystemdCgroup in the config
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl restart containerd

# 7. Install Kubernetes components
sudo apt install -y apt-transport-https ca-certificates curl gpg

curl -fsSL [https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key](https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key) | sudo tee /etc/apt/trusted.gpg.d/kubernetes-apt-keyring.asc > /dev/null

echo 'deb [signed-by=/etc/apt/trusted.gpg.d/kubernetes-apt-keyring.asc] [https://pkgs.k8s.io/core:/stable:/v1.29/deb/](https://pkgs.k8s.io/core:/stable:/v1.29/deb/) /' | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt update
sudo apt install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

### Master Node

```bash
# 1. Initialize the cluster
sudo kubeadm init --apiserver-advertise-address=<Master-IP> --pod-network-cidr=172.29.0.0/16

# 2. Setup kubeconfig for kubectl
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# 3. Apply Calico network plugin
kubectl apply -f [https://raw.githubusercontent.com/projectcalico/calico/v3.26.1/manifests/calico.yaml](https://raw.githubusercontent.com/projectcalico/calico/v3.26.1/manifests/calico.yaml)

# 4. Verify nodes
kubectl get nodes
```

### Worker Node

```bash
# 1. Join the cluster
sudo kubeadm join <Master-IP>:6443 --token <token> --discovery-token-ca-cert-hash sha256:<hash>
```

---

## Meaning of Each Step

### Disable Swap

Kubernetes assigns "Quality of Service" (QoS) classes to Pods to ensure they get the exact RAM they requested. If Linux starts moving memory pages to a hard drive swap file, K8s loses its ability to accurately measure and enforce memory limits. It literally refuses to start if swap is on. 

This means when you deploy a Pod, you can specify how much RAM it **Requests** (what it needs to start) and its **Limit** (the absolute maximum it is allowed to use). Based on these numbers, Kubernetes assigns the Pod one of three Quality of Service (QoS) classes:

* **Guaranteed:** The Pod requested exactly 1GB of RAM and has a limit of exactly 1GB. It is a VIP. K8s guarantees this RAM will always be available to it.
* **Burstable:** The Pod requested 512MB but has a limit of 1GB. It's guaranteed 512MB, but can "burst" higher if the node has spare RAM.
* **BestEffort:** The Pod didn't specify any limits. It gets whatever is left over. If the server runs out of memory, these are the first Pods to be killed.

**The Swap Problem:**
* If Swap is enabled, the Linux kernel essentially "lies" to Kubernetes (when memory is full).
* K8s might think a Guaranteed Pod is happily using its 1GB of RAM.
* But under the hood, Linux might have swapped 500MB of that Pod's memory to the hard drive.
* Suddenly, your blazing-fast microservice is running at a crawl because it's reading from a disk instead of RAM.

Furthermore, Kubernetes relies on the Linux OOM (Out of Memory) Killer to terminate misbehaving Pods that exceed their limits. If swap is enabled, the OOM killer might not trigger when K8s expects it to, because the OS just keeps shoving data onto the hard drive.

To ensure performance is predictable and that limits are strictly enforced, the K8s creators simply hardcoded `kubelet` to crash on startup if it detects swap is enabled. *(Note: recent versions of K8s are starting to experiment with swap support for edge cases, but disabling it remains the absolute standard).*

### Overlay Module

Just like a Docker base image where changes are stacked on top of each other, the `overlay` module handles this layered filesystem capability for your containers.

### br_netfilter Module

* **The Disconnect:** By default in the Linux operating system, when a frame of data travels across a Layer 2 bridge, the kernel processes it extremely fast. Layer 3 completely ignores it. It assumes, "This is local neighborhood traffic, the postal system (Layer 3) doesn't need to inspect this."
* **The Kubernetes Problem:** K8s creates "Services" with fake Virtual IPs (VIPs). K8s programs iptables (Layer 3) to intercept traffic going to those fake VIPs and rewrite them to the real Pod IPs.
* **The Collision:** A Pod tries to talk to a Service VIP. It sends the traffic out onto the local Layer 2 virtual bridge. Because it's on a bridge, Linux skips Layer 3. Because Linux skips Layer 3, iptables never sees the packet. The VIP is never translated, and the connection drops.
* **The Solution:** When you load the `br_netfilter` kernel module, it literally forces the Linux kernel to change its default behavior. It says: "Even though this traffic is flowing across a Layer 2 bridge, you MUST pause and hand the packet up to Layer 3 iptables for inspection before you let it finish crossing the bridge."
* Because `br_netfilter` forces that Layer 2 traffic up to Layer 3, iptables catches the fake Service VIP, performs the Network Address Translation (NAT) to the real Pod IP, and hands it back down to the bridge for delivery.

### Sysctl Parameters

#### `net.bridge.bridge-nf-call-iptables = 1` (and `ip6tables`)

To understand these two lines, remember the discussion about Layer 2 (Bridges) and Layer 3 (Firewalls/iptables).

* **The Default Linux Behavior:** Normally, when data crosses a local network bridge (like the virtual bridge connecting your containers), Linux processes it incredibly fast at Layer 2. It completely bypasses the iptables firewall (Layer 3) because it assumes local traffic doesn't need to be inspected or routed.
* **What this setting does:** Setting this to `1` (True) forces the Linux kernel to change that default behavior. It says: "Every time an IPv4 (or IPv6) packet crosses a local bridge, you MUST pause and hand that packet up to the iptables firewall for inspection before letting it continue."
* **Why Kubernetes absolutely needs this:** Kubernetes Services rely on "fake" Virtual IPs (VIPs). The `kube-proxy` agent writes rules into iptables to say, "If you see traffic going to Fake IP X, rewrite the destination to Real Pod IP Y." If a Pod sends traffic to a Service VIP, that traffic goes out onto the virtual bridge. If you don't have these settings set to 1, the traffic crosses the bridge, iptables never sees it, the destination IP is never rewritten, and your Pod's request just times out. These lines ensure the firewall catches the packet and performs the necessary Network Address Translation (NAT).

#### `net.ipv4.ip_forward = 1`

This single line is what transforms a standard Linux server into a Router.

* **The Default Linux Behavior:** By default (`0`), a Linux computer is selfish. If it receives a network packet, it looks at the destination IP address. If that IP address does not belong to its own network card, the kernel says, "This isn't for me," and instantly drops (destroys) the packet.
* **What this setting does:** Setting this to `1` (True) enables IP Forwarding. It tells the kernel: "If you receive a packet that isn't for you, don't drop it. Instead, look at your routing tables and forward the packet out the correct port to get it closer to its destination."
* **Why Kubernetes absolutely needs this:** In a Kubernetes cluster, your physical Node (e.g., IP `192.168.1.10`) is going to constantly receive traffic that is destined for a Pod (e.g., IP `10.244.1.5`). Because the Pod's IP is different from the Node's IP, a default Linux kernel would just drop the traffic. By turning on `ip_forward`, you allow the Node to accept the traffic, realize it is meant for a Pod living inside it (or on another Node), and forward it appropriately.

### Systemd Cgroup Configuration

To understand why we change this setting, you need to understand how Linux limits CPU and RAM for containers using a feature called **cgroups** (Control Groups).

When you tell Kubernetes, "This Pod is only allowed to use 1GB of RAM," Kubernetes doesn't actually enforce that. It tells `containerd` to do it, and `containerd` uses Linux cgroups to build a microscopic "fence" around the container.

**Here is where the problem starts:**
* **The Linux OS Manager (`systemd`):** Almost all modern Linux distributions (like Ubuntu, CentOS) use a program called `systemd` to boot the system and act as the master manager of these cgroups.
* **The Container Manager (`cgroupfs`):** By default, `containerd` comes out of the box using a completely different, older cgroup manager called `cgroupfs`.

**The Collision:**
If you don't run that `sed` command, you will have two different managers (`systemd` and `cgroupfs`) fighting over the exact same CPU and RAM limits on your server. When the server comes under heavy load, these two managers will get out of sync. Kubernetes will get confused about how much RAM is actually available, processes will crash, and your Kubernetes node will suddenly flip to a `NotReady` state and drop all your Pods.

**The Solution:**
By changing `SystemdCgroup = true`, you are forcing `containerd` to surrender its resource management to `systemd`. This ensures that the Linux kernel, the Kubernetes `kubelet`, and `containerd` are all using the exact same "boss" to manage CPU and RAM.