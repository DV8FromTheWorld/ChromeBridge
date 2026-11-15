# Third-party notices

Chrome Bridge's application code is MIT-licensed. This does not change third-party licenses.

- **Kotlin standard library 1.9.24**: Copyright JetBrains s.r.o. and Kotlin contributors; Apache License 2.0. Included in the APK through the Kotlin Android plugin's default dependency. Source/license: https://github.com/JetBrains/kotlin/tree/v1.9.24/license
- **Gradle wrapper 8.7**: Copyright the original Gradle authors; Apache License 2.0. Original wrapper headers and the JAR's `META-INF/LICENSE` remain intact. https://github.com/gradle/gradle/tree/v8.7.0
- **JUnit 4.13.2** (test only): Eclipse Public License 1.0. https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt
- **Hamcrest Core 1.3** (transitive test only): BSD 3-Clause. https://github.com/hamcrest/JavaHamcrest/blob/v1.3/LICENSE.txt

JUnit/Hamcrest, the Android SDK/build tools, Gradle distribution and plugin caches are not redistributed in the clean source archive. Build dependency tooling retains its own licenses. Apache 2.0 text is included in `licenses/Apache-2.0.txt`; APK assets also include the runtime license notice.

This project is not affiliated with Google, Chrome, Island or Android. Those names identify compatibility targets.
