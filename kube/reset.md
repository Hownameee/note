# Reset Kubernetes

## Server clean

sudo systemctl stop kubelet
sudo systemctl stop containerd
sudo rm -f /etc/kubernetes/admin.conf
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/manifests/*
sudo rm -rf /var/lib/etcd/*
sudo rm -rf /etc/kubernetes/*.conf
sudo rm -rf /etc/cni/net.d
sudo rm -rf ~/.kube
sudo systemctl start containerd
sudo systemctl start kubelet
sudo kubeadm init --apiserver-advertise-address=192.168.56.10 --pod-network-cidr=10.244.0.0/16

## Worker clean

sudo kubeadm reset -f
sudo systemctl stop kubelet
sudo rm -rf /etc/cni/net.d
sudo rm -rf /var/lib/kubelet/*
sudo systemctl start kubelet
kubeadm join 192.168.56.10:6443 --token p50sgl.fgpkd8jknks1yjod \
 --discovery-token-ca-cert-hash sha256:b432757de3e62c23f852cb0e39ac0507427bcb8f8c4d6d2a39ac3dd2c6560b30
