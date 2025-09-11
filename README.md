[![Latest version](https://img.shields.io/maven-central/v/software.xdev/vaadin-package-json-optimizer?logo=apache%20maven)](https://mvnrepository.com/artifact/software.xdev/vaadin-package-json-optimizer)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/vaadin-package-json-optimizer/check-build.yml?branch=develop)](https://github.com/xdev-software/vaadin-package-json-optimizer/actions/workflows/check-build.yml?query=branch%3Adevelop)

# `package.json` optimizer for Vaadin

Patches `package.json` and replaces unused packages with an empty package.

This also prevents the installation of the corresponding transitive dependencies and lowers the overall attack surface.

As of Vaadin 24.8 this results in the following:
* at least 280 fewer packages (-55%): ~500 → ~210
* 80MB fewer required storage space (-40%): ~210MB → ~130MB
* overall faster build/`npm install` by processing/downloading less packages

Please note that this is currently intended as a stopgap measure until Vaadin implements improvements upstream.

<details><summary>Currently these npm package groups are overwritten with an empty package</summary>

* `glob`'s and `rollup-plugin-visualizer`'s CLI packages
  * Vaadin never uses their CLI and only library methods
* Incorrectly declared dependencies of `transform-ast`
* All Vaadin Pro components and their corresponding packages
* Unused dependencies of `workbox`

Full details are available in the [source code](./vaadin-package-json-optimizer/src/main/java/software/xdev/vaadin/vpjo/VPJOptimizer.java).

</details>

## Usage

`package.json` needs to be modified BEFORE the Vaadin installs all dependencies using `npm install`.

This needs to happen in 2 places:
1. When building the frontend with the build-frontend goal
    ```xml
        <build>
            <plugins>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>exec-maven-plugin</artifactId>
                    <version>...</version>
                    <executions>
                        <execution>
                            <id>patch-package-json-overrides</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>java</goal>
                            </goals>
                            <configuration>
                                <mainClass>software.xdev.vaadin.vpjo.Launcher</mainClass>
                                <arguments>
                                    <argument>${project.basedir}</argument>
                                    <argument>${project.build.directory}</argument>
                                </arguments>
                                <includeProjectDependencies>false</includeProjectDependencies>
                                <includePluginDependencies>true</includePluginDependencies>
                                <executableDependency>
                                    <groupId>software.xdev</groupId>
                                    <artifactId>vaadin-package-json-optimizer</artifactId>
                                </executableDependency>
                            </configuration>
                        </execution>
                    </executions>
                    <dependencies>
                        <dependency>
                            <groupId>software.xdev</groupId>
                            <artifactId>vaadin-package-json-optimizer</artifactId>
                            <version>...</version>
                        </dependency>
                    </dependencies>
                </plugin>
    ...
    ```
2. When running in dev mode
    ```xml
        <profiles>
            <profile>
                <id>dev</id>
                <activation>
                    <activeByDefault>true</activeByDefault>
                </activation>
                <dependencies>
                    <!-- Only needed as dependency during development not in production! -->
                    <dependency>
                        <groupId>software.xdev</groupId>
                        <artifactId>vaadin-package-json-optimizer</artifactId>
                        <version>...</version>
                    </dependency>
                </dependencies>
            </profile>
        ...
    ```

For more information have a look at the [demo's `pom.xml`](./vaadin-package-json-optimizer-demo/pom.xml).

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/vaadin-package-json-optimizer/releases/latest#Installation)

## Support
If you need support as soon as possible and you can't wait for any pull request, feel free to use [our support](https://xdev.software/en/services/support).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Dependencies and Licenses
View the [license of the current project](LICENSE) or the [summary including all dependencies](https://xdev-software.github.io/vaadin-package-json-optimizer/dependencies)
