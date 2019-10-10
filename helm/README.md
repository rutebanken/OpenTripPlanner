## Introduction

This chart will setup the `otp` app and it's dependencies in a given namespace.

## Prerequisites

Download Helm version >v2.13.0 https://github.com/kubernetes/helm

## Installing the Chart

### Print template

To see the template output before deploying:
```bash
$ helm template DIRNAME --name RELEASENAME  --values VALUES.YAML --namespace NAMESPACE
```

For example:
```bash
$ helm template helm/OpenTripPlanner/ --name otp  --values helm/OpenTripPlanner/dev-c2-values.yaml --namespace dev
```

### Intall chart

To install the chart with the release name `my-release`:

```bash
$ helm install DIRNAME --name RELEASENAME --namespace NAMESPACE
```
Watch the deployment with this command

```bash
$ kubectl get pods -n NAMESPACE
```

## Uninstalling the Chart (WARNING)

To completely remove `my-release`:

```bash
$ helm delete <release-name> --purge
```

## Updating a chart
Run helm upgrade:

```bash
$ helm upgrade --install RELEASENAME DIRNAME --namespace NAMESPACE --values VALUES.YAML
```

for example:
```bash
$ helm upgrade --install otp helm/OpenTripPlanner --namespace dev --values helm/OpenTripPlanner/dev-c2-values.yaml
```

## Helm rollback
Check available revision:
```bash
$ helm history REPONAME
```

Rollback to a revision:
```bash
$ helm rollback REPONAME REVISIONNUMBER
```
