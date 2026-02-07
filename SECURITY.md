# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly:

1. **Do NOT** create a public GitHub issue for security vulnerabilities
2. Email security concerns to the project maintainers
3. Include a description of the vulnerability and steps to reproduce

## Security Practices

### API Key Management
- API keys are stored using Android `EncryptedSharedPreferences`
- Keys are never hardcoded in source code
- Build-time configuration uses `local.properties` (excluded from version control)
- CI/CD uses encrypted GitHub Secrets

### Network Security
- All API communication uses HTTPS with TLS 1.2+
- Certificate pinning for the OPL API server
- VPN service operates locally (no remote proxy)
- DNS queries are analyzed locally, not forwarded to third parties

### Data Privacy
- No user accounts or personal data collected
- GPS data is only transmitted when user explicitly submits a snapshot
- Location monitoring happens entirely on-device
- No analytics or tracking SDKs

### Code Security
- Dependencies are audited via GitHub Dependabot
- CodeQL security scanning in CI/CD pipeline
- No secrets in repository history (verified via git-secrets scanning)

## Security Audit Checklist

- [x] No API keys or secrets in source code
- [x] No secrets in git history
- [x] HTTPS-only API communication
- [x] Encrypted local storage for sensitive data
- [x] VPN traffic analysis is local-only
- [x] GPS data requires explicit user action to transmit
- [x] Dependencies audited for known vulnerabilities
- [x] CI/CD pipeline includes security scanning
