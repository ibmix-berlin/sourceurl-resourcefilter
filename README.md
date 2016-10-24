
# sourceurl-resourcefilter
Custom Maven resources filter to prepend Javascript files with `//@ sourceURL=` annotation. Useful for Nashorn Javascript debugging.

# Background
When debugging a mixture of Java and Javascript on the the Nashorn Javascript engine of the JVM, debuggers need to know about the locations of Javascript source files, in order to be able to place breakpoints in them and correctly show Javascript sources for Javascript stack frames.
One precondition for this is a special source code annotation `//@ sourceURL= ...` in the first line of the Javascript source files.
This Maven resourcefilter will automatically add that annotation to any Javascript files during `process-resources`, so you don't have to manually add them in the original sources.

# Limitations
Debuggers will allow placing breakpoints only for JS source files that are loaded using the `load()` Nashorn extension. E.g. Netbeans debugger will show `<eval>.js` as the name of the source when that is not the case, instead of opening the original source file.

With the annotation present, Netbeans will show source file names in the stack frames, and the whole path to the source file for the top stackframe:

Without sourceurl-resourcefilter:
![](docs/images/netbeans-stackframes-before.png?raw=true "Without sourceurl-resourcefilter")

------------------------------------------------------------

With sourceurl-resourcefilter:
![](docs/images/netbeans-stackframes.png?raw=true "With sourceurl-resourcefilter")


# Availability
Currently the plugin is not available in public Maven repos, so you have to build and install it yourself into your local Maven repository.

# Usage
In your project's pom.xml, configure the `maven-resources-plugin` as follows:

	<build>
	   <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-resources-plugin</artifactId>
            <version>3.0.1</version>
            <configuration>
              <mavenFilteringHints>
                <mavenFilteringHint>prependJsSourceUrl</mavenFilteringHint>
              </mavenFilteringHints>
             </configuration>
            <dependencies>
              <dependency>
                <groupId>com.aperto</groupId>
                <artifactId>sourceurl-resourcefilter</artifactId>
                <version>0.0.1-SNAPSHOT</version>
              </dependency>
            </dependencies>
          </plugin>

Also turn on filtering for each folder containing Javascript that you want to process during the build:

	<build>
    <resources>
        <resource>
            <directory>node/lib</directory>
            <filtering>true</filtering>
        </resource>

sourceurl-resourcefilter will prepend the annotation only for files that do not contain the String `sourceURL` in their first line, i.e. it won't prepend when an annotation is already present.






