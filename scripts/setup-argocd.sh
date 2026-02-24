#!/bin/bash
set -euo pipefail

echo "=== ArgoCD Installation on K3s ==="

# 1) Create argocd namespace
echo "[1/5] Creating argocd namespace..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

# 2) Install ArgoCD (|| true: ApplicationSets CRD annotation size warning is non-critical)
echo "[2/5] Installing ArgoCD..."
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml || true

# 3) Remove NetworkPolicies (not needed on single-node K3s, can block pod communication)
echo "[3/6] Removing NetworkPolicies..."
kubectl delete networkpolicy --all -n argocd 2>/dev/null || true

# 4) Wait for ArgoCD server to be ready
echo "[4/6] Waiting for ArgoCD server to be ready..."
kubectl wait --for=condition=available deployment/argocd-server -n argocd --timeout=300s

# 5) Expose ArgoCD server via NodePort (30090)
echo "[5/6] Exposing ArgoCD server on NodePort 30090..."
kubectl patch svc argocd-server -n argocd -p '{
  "spec": {
    "type": "NodePort",
    "ports": [
      {
        "name": "https",
        "port": 443,
        "targetPort": 8080,
        "nodePort": 30090,
        "protocol": "TCP"
      }
    ]
  }
}'

# 6) Print initial admin password
echo "[6/6] ArgoCD initial admin password:"
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
echo ""

echo ""
echo "=== ArgoCD Installation Complete ==="
echo "UI: https://<NODE_IP>:30090"
echo "Username: admin"
echo "Password: (printed above)"
echo ""
echo "To apply the ArgoCD Application:"
echo "  kubectl apply -f k8s/argocd/application.yaml"
