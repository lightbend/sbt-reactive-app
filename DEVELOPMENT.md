### Minikube

on a fresh terminal..

```
minikube delete
minikube start
eval $(minikube docker-env)
kubectl config current-context
sbt
> scripted bootstrap-demo/kubernetes-api
```

### OpenShift

```
oc new-project reactivelibtest1
export OC_TOKEN=$(oc serviceaccounts get-token default)
echo "$OC_TOKEN" | docker login -u unused --password-stdin docker-registry-default.centralpark.lightbend.com
sbt -Ddeckhand.openshift

> scripted bootstrap-demo/kubernetes-api
```
