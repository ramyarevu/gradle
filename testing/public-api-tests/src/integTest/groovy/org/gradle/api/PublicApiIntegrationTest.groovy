/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class PublicApiIntegrationTest extends AbstractIntegrationSpec {
    void 'can use public API'() {
        def apiJarRepoLocation = System.getProperty('integTest.apiJarRepoLocation')
        def apiJarVersion = System.getProperty("integTest.distZipVersion")

        buildFile << """
            plugins {
                id("java-library")
            }

            repositories {
                maven {
                    url = uri("$apiJarRepoLocation")
                }
                mavenCentral()
            }

            dependencies {
                implementation("org.gradle.experimental:gradle-public-api:${apiJarVersion}")
            }
        """

        file("src/main/java/org/example/PublishedApiTestPlugin.java") << """
            package org.example;

            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class PublishedApiTestPlugin implements Plugin<Project> {
                public void apply(Project project) {
                    project.getTasks().register("myTask", CustomTask.class);
                }
            }
        """
        file("src/main/java/org/example/CustomTask.java") << """
            package org.example;

            import org.gradle.api.DefaultTask;
            import org.gradle.api.tasks.TaskAction;

            public class CustomTask extends DefaultTask {
                @TaskAction
                public void customAction() {
                    System.out.println("Hello from CustomTask");
                }
            }
        """

        expect:
        succeeds(":compileJava")
    }
}
