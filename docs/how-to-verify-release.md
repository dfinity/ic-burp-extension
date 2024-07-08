# How to verify a release
0. Make sure you have [GitHub CLI](https://cli.github.com/) installed.
1. Download the JAR from the [release](https://github.com/dfinity/ic-burp-extension/releases) you want to verify, e.g., `ic-burp-extension-0.1.1-alpha.jar`.
2. Run the following command:
```
gh attestation verify ic-burp-extension-0.1.1-alpha.jar -R dfinity/ic-burp-extension
TODO: add output
```
