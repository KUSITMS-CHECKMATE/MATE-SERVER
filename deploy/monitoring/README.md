# Monitoring (Grafana / Prometheus / Loki)

`kube-prometheus-stack`, `loki-stack`은 클러스터에 Helm으로 설치합니다.  
이 디렉터리의 Kustomize 리소스(Ingress, ServiceMonitor)는 ArgoCD `monitoring` 앱으로 Sync됩니다.

## Grafana 대시보드 영속화 (PVC)

Grafana UI에서 만든 대시보드는 기본적으로 Pod ephemeral storage에만 저장됩니다.  
Pod가 CrashLoopBackOff 후 새 ReplicaSet으로 교체되면 **대시보드가 사라집니다.**

`helm/kube-prometheus-stack-values.yaml`에서 PVC를 활성화합니다.

```bash
helm upgrade monitoring prometheus-community/kube-prometheus-stack \
  -n monitoring \
  -f deploy/monitoring/helm/kube-prometheus-stack-values.yaml \
  --reuse-values

kubectl rollout status deployment -n monitoring -l app.kubernetes.io/name=grafana
kubectl get pvc -n monitoring
```

### Loki datasource default 충돌 방지

Prometheus(kube-prometheus-stack)와 Loki(loki-stack)가 둘 다 `isDefault: true`이면 Grafana가 기동하지 않습니다.

```bash
helm upgrade loki grafana/loki-stack -n monitoring \
  -f deploy/monitoring/helm/loki-stack-values.yaml
```

`loki.isDefault: false`만 설정합니다. (`--set isDefault=false`는 동작하지 않음)
