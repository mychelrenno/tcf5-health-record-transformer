Deployment guide - build image and deploy to Kubernetes

Steps (local development)

1) Build Docker image locally

   # from project root
   docker build -t tcf5-health-record-transformer:local .

2) (Optional) Tag and push to registry

   docker tag tcf5-health-record-transformer:local myregistry.example.com/tcf5-health-record-transformer:local
   docker push myregistry.example.com/tcf5-health-record-transformer:local

3) Prepare Kubernetes manifests and Secret

   # Create Secret with DB_PASS (base64 encoding)
   kubectl create secret generic tcf5-health-record-transformer-secret --from-literal=DB_PASS='sus_password_dev'

   # Apply k8s resources via kustomize (directory k8s/ contains kustomization.yaml)
   kubectl apply -k k8s/

4) If you need to change the image after build

   # Example: set a different image tag
   kubectl set image deployment/tcf5-health-record-transformer app=myregistry.example.com/tcf5-health-record-transformer:local

5) Verify deployment

   kubectl rollout status deployment/tcf5-health-record-transformer
   kubectl get pods -l app=tcf5-health-record-transformer
   kubectl logs -l app=tcf5-health-record-transformer

Notes
- The deployment manifest contains a placeholder image `tcf5-health-record-transformer:latest`. Use `kubectl set image` to update if you push images into a registry.
- ConfigMap provides non-secret configuration values; Secret stores the DB password. In production, replace with sealed secrets/vault.
- The application reads configuration from environment variables and application.yaml placeholders.
