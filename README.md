# ic-burp-extension

To compile the burp plugin run
```
$ ./build.sh
```
This will compile the rust library and drop it into the `java-extension/src/main/resources/` folder and then build the Burp extension JAR file in `java-extension/build/libs/`. The JAR file can be imported into Burp under the "Extensions" tab.
