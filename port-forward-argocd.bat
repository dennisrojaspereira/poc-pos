@echo off
kubectl port-forward svc/argocd-server -n argocd 8089:443
