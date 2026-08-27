# Family sideload keystore

This keystore is for installing **Watt Home** on household phones only.
It is **not** a Play Store signing key.

The `.jks` file is **not** stored in git. Keep a private copy for local
`assembleRelease` builds (see `keystore.properties.example`).

GitHub Actions signs with these repo secrets (Settings → Secrets and variables → Actions):

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Use the same keystore that signed earlier family APKs so phones can overwrite-install without uninstalling.
