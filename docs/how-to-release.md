# How to create a release
1. Make sure the correct version is set in [build.gradle.kts](../java-extension/build.gradle.kts), e.g., `0.1.0-alpha-SNAPSHOT` and it has been pushed to the main branch.
2. Checkout the right commit
```
git checkout 391cddca4537547053cc69057358a43ac74bb8ae
```
3. Create a tag with the name `release/<version>` and push it
```
git tag release/0.1.0-alpha
git push origin release/0.1.0-alpha
```
4. Check if a new [release workflow](https://github.com/dfinity/ic-burp-extension/actions/workflows/release.yml) has been triggered and wait until it has completed. This workflow will build the JAR, create a new draft release and attach the JAR to it.
5. If the workflow has completed successfully a new draft release should be available under [releases](https://github.com/dfinity/ic-burp-extension/releases). Click on the pencil on the upper right corner to edit the release. Verify that all specified information is correct, set the `pre-release` flag if applicable and finally click `Publish release`.
