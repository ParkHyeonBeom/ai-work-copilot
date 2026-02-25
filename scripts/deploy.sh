#!/bin/bash
set -euo pipefail

# ============================================================
# AI Work Copilot - K3s Full Deployment Script
# Target: Windows Home Server (AMD64) via SSH
# ============================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SSH_HOST="homeserver"
KUBECONFIG_HOME="$HOME/.kube/config-home"
KUBECTL="KUBECONFIG=$KUBECONFIG_HOME kubectl"
NAMESPACE="workcopilot"
SERVICES=(gateway user-service integration-service ai-router-service briefing-service)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_step() { echo -e "\n${BLUE}==>${NC} ${GREEN}$1${NC}"; }
log_warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
log_error() { echo -e "${RED}✗ $1${NC}"; exit 1; }

# ============================================================
# Step 0: Pre-flight checks
# ============================================================
log_step "Step 0: Pre-flight checks"

command -v mvn >/dev/null 2>&1 || log_error "Maven not found"
command -v docker >/dev/null 2>&1 || log_error "Docker not found"
command -v ssh >/dev/null 2>&1 || log_error "SSH not found"
[ -f "$KUBECONFIG_HOME" ] || log_error "Kubeconfig not found at $KUBECONFIG_HOME"

# Check secrets.yaml exists
if [ ! -f "$PROJECT_ROOT/k8s/secrets.yaml" ]; then
    log_warn "k8s/secrets.yaml not found!"
    echo "  Please create it from the template:"
    echo "    cp k8s/secrets.yaml.template k8s/secrets.yaml"
    echo "    # Edit k8s/secrets.yaml with your base64-encoded values"
    echo ""
    read -p "Continue without secrets? (existing secrets on cluster will be kept) [y/N] " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]] || exit 1
    SKIP_SECRETS=true
else
    SKIP_SECRETS=false
fi

echo -e "${GREEN}✓ All pre-flight checks passed${NC}"

# ============================================================
# Step 1: Maven build
# ============================================================
log_step "Step 1: Maven clean package (skip tests)"

cd "$PROJECT_ROOT"
mvn clean package -DskipTests -q || log_error "Maven build failed"
echo -e "${GREEN}✓ Build complete${NC}"

# ============================================================
# Step 2: Docker build (linux/amd64)
# ============================================================
log_step "Step 2: Docker build (linux/amd64)"

for svc in "${SERVICES[@]}"; do
    echo -n "  Building workcopilot/$svc:latest ... "
    docker build --platform linux/amd64 -t "workcopilot/$svc:latest" "$PROJECT_ROOT/$svc" -q
    echo -e "${GREEN}done${NC}"
done

# ============================================================
# Step 3: Transfer images to K3s via SSH
# ============================================================
log_step "Step 3: Transfer images to K3s (docker save | ssh | k3s ctr import)"

for svc in "${SERVICES[@]}"; do
    echo -n "  Transferring workcopilot/$svc:latest ... "
    docker save "workcopilot/$svc:latest" | ssh "$SSH_HOST" 'sudo k3s ctr images import -'
    echo -e "${GREEN}done${NC}"
done

# ============================================================
# Step 4: kubectl apply
# ============================================================
log_step "Step 4: Apply K8s manifests"

# Namespace
echo -n "  Applying namespace ... "
eval $KUBECTL apply -f "$PROJECT_ROOT/k8s/namespace.yaml"

# Secrets (if available)
if [ "$SKIP_SECRETS" = false ]; then
    echo -n "  Applying secrets ... "
    eval $KUBECTL apply -f "$PROJECT_ROOT/k8s/secrets.yaml"
fi

# Services and Deployments
for svc in "${SERVICES[@]}"; do
    echo -n "  Applying $svc ... "
    eval $KUBECTL apply -f "$PROJECT_ROOT/k8s/$svc/"
    echo -e "${GREEN}done${NC}"
done

# ============================================================
# Step 5: Rollout status
# ============================================================
log_step "Step 5: Waiting for rollouts"

for svc in "${SERVICES[@]}"; do
    echo -n "  Waiting for $svc ... "
    eval $KUBECTL rollout status deployment/"$svc" -n "$NAMESPACE" --timeout=120s
done

# ============================================================
# Summary
# ============================================================
log_step "Deployment complete!"
echo ""
echo "  Check pods:    KUBECONFIG=$KUBECONFIG_HOME kubectl get pods -n $NAMESPACE"
echo "  Check services: KUBECONFIG=$KUBECONFIG_HOME kubectl get svc -n $NAMESPACE"
echo "  Gateway health: curl http://100.95.227.98:30080/actuator/health"
echo ""
eval $KUBECTL get pods -n "$NAMESPACE"
