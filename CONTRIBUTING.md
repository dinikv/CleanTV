# Contributing

Start with a small issue describing the feature or sanitized compatibility finding. For package changes, include evidence and expected feature losses; names alone are not enough.

Run `python3 -m unittest discover -s tests -v`. Never use real-device mutations in automated tests. Test interruption, rollback, mismatched firmware, shared UIDs and refusal paths when changing the CLI.

Keep the default audit read-only. Do not add silent telemetry, command downloads, root bypasses or blanket package lists. Preserve the saved-plan and pre-mutation journal design.

Before opening a pull request, inspect every staged file. Do not include device data, IP/MAC addresses, account names, keys, access tokens, local paths or personal screenshots. Generated launcher files and signing keys must remain ignored. Check new image metadata. Assets must have clear provenance and licensing scope.

Report exactly what you tested. Do not label a profile universally safe based on one TV or one successful command.
