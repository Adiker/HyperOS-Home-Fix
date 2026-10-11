# Repository publication and releases

Repository publication requires the owner's explicit approval. A first release requires separate approval after successful CI and physical-device verification. Do not create the tag merely because the version field is already `0.1.0`.

## Initial repository

After reviewing and approving the local repository:

```sh
gh repo create HyperOS-Home-Fix --public --source=. --remote=origin --push
gh run list --workflow build.yml
gh run watch RUN_ID --exit-status
gh run download RUN_ID --name HyperOS-Home-Fix-debug --dir ci-artifacts
```

Verify the completed Actions conclusion and downloaded artifact, not merely that a run was queued. The first public README must continue to say physical validation is pending until it has happened.

## Signing

Create and back up a dedicated Android release keystore **outside** the repository. Keep the same signing identity for future updates. Do not commit keystores, passwords, Base64 keystores, local.properties, Xiaomi APKs or decompiled output. Configure environment secrets on the GitHub environment named `release`:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64 of the private release keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Signing key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Use GitHub's secret UI or `gh secret set --env release` reading a protected file/stdin, never a secret pasted into a public issue, shell history or workflow. Do not reuse the throwaway/debug signing key for a stable release.

Configure **required reviewers** on `release`. The workflow checks that the environment actually has required reviewers and refuses to sign if it does not. Approval of a public repository does not grant approval to a release.

After the exact candidate commit passes CI and the physical checklist, configure these **environment variables**:

```text
DEVICE_VERIFICATION_CONFIRMED=true
DEVICE_VERIFIED_TAG=v0.1.0
DEVICE_VERIFIED_COMMIT=<full verified commit SHA>
```

The tag and commit must match the workflow event. These values are human attestations, not automatic device tests. Attach a factual validation report to the reviewed release commit and require owner approval. Then create and push the tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```

The protected workflow reruns tests/lint, decodes the keystore into the runner's private temporary directory, builds the signed APK, verifies signature/alignment/package/version/non-debuggable status and legacy-module packaging, then creates a GitHub Release containing APK and `SHA256SUMS`. Temporary signing material is removed even after failure. Changing `versionName` and `versionCode` is required for future releases.

## Local signing configuration

Without `SIGN_RELEASE=true`, `assembleRelease` deliberately produces an unsigned verification build. For signing, provide these environment variables through a private shell/session:

```text
SIGN_RELEASE=true
RELEASE_KEYSTORE_PATH=/absolute/path/outside/repository/release.jks
RELEASE_STORE_PASSWORD=<secret>
RELEASE_KEY_ALIAS=<alias>
RELEASE_KEY_PASSWORD=<secret>
```

Missing signing values fail the build instead of falling back to a debug key. No real release key is generated automatically by this project.
