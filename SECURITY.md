# Security Policy

## Supported versions

There are no versioned releases yet. Security fixes are prepared for the current `main` branch; other branches and snapshots are not guaranteed to receive fixes.

| Version | Supported |
| --- | --- |
| `main` (current) | Yes |
| Other branches or snapshots | No |

## Reporting a vulnerability

Please report suspected vulnerabilities privately through GitHub's **Security** tab using **Report a vulnerability** (private vulnerability reporting is enabled for this repository). Do not open a public issue or pull request for an undisclosed vulnerability.

Include, when possible:

- A concise description of the affected feature and potential impact.
- The affected commit or application version and deployment configuration.
- Steps to reproduce or a minimal proof of concept.

Do not include real passwords, client secrets, access tokens, private keys, or production data. Redact sensitive values from logs and examples.

If GitHub's private reporting option is unavailable, contact the repository owner privately through GitHub rather than disclosing the issue publicly.

## Response and disclosure

Reports are reviewed by the maintainers. We will acknowledge and follow up as soon as reasonably possible, but there is no guaranteed response-time SLA. If a report is accepted, we will coordinate remediation and disclosure with the reporter. If it is declined, we will explain the reason where possible. Please keep the report and details private until coordinated disclosure.

## Deployment notes

- The checked-in `application.properties` contains fixed credentials and fallback secrets. Treat them as public, override them in every environment, and rotate any values used outside disposable local development.
- Change the initial `admin` / `admin` administrator password immediately on first run.
- Use HTTPS for the administration UI and API endpoints, and protect HTTP Basic client credentials, Bearer API tokens, and OAuth2 client credentials/access tokens.
- Keep the encryption secret stable or re-encrypt stored database passwords and client secrets before changing it; existing values cannot be decrypted with a different secret.
- Keep organization, named-client, credential-user and API activation state aligned with the access you intend to grant. Inactive users or parent organizations/clients cannot authenticate or access APIs.
- Organization deletion is available only at the organization level and is blocked until every named client, credential user and assigned API beneath it is inactive. Deletion removes the organization and its nested named clients and credential users; use deactivation rather than deletion to suspend access.
