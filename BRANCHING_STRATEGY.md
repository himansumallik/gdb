- **main**: Production stable code. Trigger point for production environments.
- **dev**: Active integration branch for testing microservices compatibility.
- **feature/<service-name>-<topic>**: Isolated development branches.

## Pull Request Rules
- All features must merge into `dev` via a Pull Request (PR).
- Requires at least 1 peer approval.
- All automated CI build checks must pass.
