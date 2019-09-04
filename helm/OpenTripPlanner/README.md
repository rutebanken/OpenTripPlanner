## Introduction

This chart will setup the `otp` app and it's dependencies in a given namespace.

## Prerequisites

Download Helm version >v2.12.0 https://github.com/kubernetes/helm

```
$ helm init --client-only
$ helm repo add entur https://entur-helm-charts.storage.googleapis.com
$ helm dependency update
```

## Installing the Chart

To see the template output before deploying:
```bash
$ helm template ./ --name ukur-demo-dev  --values values.yaml --namespace dev
```

To install the chart with the release name `my-release`:

```bash
$ helm install ./ --name <release-name> --namespace my-namespace
```
Watch the deployment with this command

```bash
$ kubectl get svc <release-name> --namespace my-namespace -w
```

## Uninstalling the Chart

To completely remove `my-release`:

```bash
$ helm delete <release-name> --purge
```

## Updating a chart

Use `--reuse-values` if you want to keep values set on earlier install/upgrade. Values can be checked using this command:
```bash
$ helm get values <release-name>
```

#### From remote repository
```bash
$ helm update
$ helm upgrade <release-name> entur/ukur-demo
```

#### From local chart
```bash
$ helm upgrade --set imageTag=<image-tag e.g. master-v40> ./ukur-demo
```

## Configuration

The following tables lists the configurable parameters of the order chart and their default values.