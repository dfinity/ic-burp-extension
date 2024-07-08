# How to verify a release
0. Make sure you have [GitHub CLI](https://cli.github.com/) installed.
1. Download the JAR from the [release](https://github.com/dfinity/ic-burp-extension/releases) you want to verify, e.g., `ic-burp-extension-0.1.1-alpha.jar`.
2. Run the following command:
```
gh attestation verify ic-burp-extension-0.1.1-alpha.jar -R dfinity/ic-burp-extension
Loaded digest sha256:f0df600729fc3177c8f001cb70c33601ca50824d54f797faf4516c1d3bb4f0ec for file://ic-burp-extension-0.1.1-alpha.jar
Loaded 1 attestation from GitHub API
✓ Verification succeeded!

sha256:f0df600729fc3177c8f001cb70c33601ca50824d54f797faf4516c1d3bb4f0ec was attested by:
REPO                       PREDICATE_TYPE                  WORKFLOW                                                   
dfinity/ic-burp-extension  https://slsa.dev/provenance/v1  .github/workflows/release.yml@refs/tags/release/0.1.1-alpha
```
