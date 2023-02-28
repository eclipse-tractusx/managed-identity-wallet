{{/*
Expand the name of the chart.
*/}}
{{- define "managed-identity-wallets.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "managed-identity-wallets.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "managed-identity-wallets.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "managed-identity-wallets.labels" -}}
helm.sh/chart: {{ include "managed-identity-wallets.chart" . }}
{{ include "managed-identity-wallets.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "managed-identity-wallets.selectorLabels" -}}
app.kubernetes.io/name: {{ include "managed-identity-wallets.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Invoke include on given definition with postgresql dependency context
Usage: include "acapyPostgresContext" (list $ "your_include_function_here")
*/}}
{{- define "acapyPostgresContext" -}}
{{- $ := index . 0 }}
{{- $function := index . 1 }}
{{- include $function (dict "Values" $.Values.acapypostgresql "Chart" (dict "Name" "acapypostgresql") "Release" $.Release) }}
{{- end }}

{{/*
Invoke include on given definition with postgresql dependency context
Usage: include "postgresContext" (list $ "your_include_function_here")
*/}}
{{- define "postgresContext" -}}
{{- $ := index . 0 }}
{{- $function := index . 1 }}
{{- include $function (dict "Values" $.Values.postgresql "Chart" (dict "Name" "postgresql") "Release" $.Release) }}
{{- end }}

{{/*
Create the default JDBC url
*/}}
{{- define "managed-identity-wallets.jdbcUrl" -}}
{{- printf "jdbc:postgresql://%s-postgresql:5432/postgres?user=%s&password=%s" .Release.Name .Values.postgresql.secret.user .Values.postgresql.secret.postgrespassword }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "managed-identity-wallets.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "managed-identity-wallets.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
