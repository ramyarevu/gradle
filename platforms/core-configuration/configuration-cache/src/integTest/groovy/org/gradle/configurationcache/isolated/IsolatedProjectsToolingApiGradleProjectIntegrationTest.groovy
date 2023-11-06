/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.configurationcache.isolated

import org.gradle.tooling.model.GradleProject

class IsolatedProjectsToolingApiGradleProjectIntegrationTest extends AbstractIsolatedProjectsToolingApiIntegrationTest {

    def "can fetch IsolatedGradleProject model"() {
        settingsFile << """
            rootProject.name = 'root'

            include("lib1")
        """

        file("lib1/build.gradle") << """
            description = "Library 1"

            tasks.register("lazy") {
                println "realizing lazy task"
            }
        """

        when:
        executer.withArguments(ENABLE_CLI)
        def projectModels = runBuildAction(new FetchIsolatedGradleProjectForEachProjectInBuild())

        then:
        outputContains("Running build action to fetch isolated project models")

        projectModels.size() == 2
        projectModels[0].name == "root"
        projectModels[0].tasks.size() > 0

        projectModels[1].name == "lib1"
        projectModels[1].tasks.find { it.name == "lazy" }
        outputContains("realizing lazy task")

        and: "all properties are available"
        with(projectModels[1]) {
            name == "lib1"
            description == "Library 1"
            projectIdentifier.projectPath == ":lib1"
            projectIdentifier.buildIdentifier.rootDir == testDirectory
            path == ":lib1"
            buildScript.sourceFile == file("lib1/build.gradle")
            buildDirectory == file("lib1/build")
            projectDirectory == file("lib1")
        }

        when:
        executer.withArguments(ENABLE_CLI)
        def projectModels2 = runBuildAction(new FetchIsolatedGradleProjectForEachProjectInBuild())

        then: "build action is cached"
        outputDoesNotContain("Running build action to fetch isolated project models")
        projectModels2.size() == 2
    }

    def "can fetch GradleProject model"() {
        settingsFile << """
            rootProject.name = 'root'

            include(":lib1")
            include(":lib1:lib11")
        """

        when:
        executer.withArguments(ENABLE_CLI)
        def projectModel = fetchModel(GradleProject)

        then:
        projectModel.name == "root"
        projectModel.tasks.size() > 0
        projectModel.tasks.forEach {
            assert it.project == projectModel
        }

        projectModel.children.size() == 1
        projectModel.children[0].name == "lib1"
        projectModel.children[0].children.name == ["lib11"]
    }

    def "can fetch GradleProject model without tasks"() {
        settingsFile << """
            rootProject.name = 'root'

            include(":lib1")
        """

        buildFile << """
            tasks.register("lazy") {
                println "realizing lazy task"
            }
        """

        when:
        executer.withArguments(ENABLE_CLI, "-Dorg.gradle.internal.GradleProjectBuilderOptions=omit_all_tasks")
        def projectModel = fetchModel(GradleProject)

        then:
        projectModel.name == "root"
        projectModel.tasks.isEmpty()
        projectModel.children[0].name == "lib1"
        projectModel.children[0].tasks.isEmpty()

        outputDoesNotContain("realizing lazy task")
    }

    def "can fetch GradleProject model for non-root project"() {
        settingsFile << """
            rootProject.name = 'root'

            include(":lib1")
        """

        file("lib1/build.gradle") << """plugins { id 'java' }"""

        when:
        executer.withArguments(ENABLE_CLI)
        def projectModel = runBuildAction(new FetchGradleProjectForTarget(":lib1"))

        then:
        projectModel != null

        and: "GradleProject model is always returned for the root regardless of the target"
        projectModel.name == "root"
        projectModel.children.size() == 1
        projectModel.children[0].name == "lib1"
    }

    def "can fetch GradleProject model for an included build project"() {
        settingsFile << """
            rootProject.name = 'root'
            includeBuild("included1")
            include("lib1")
        """

        file("included1/settings.gradle") << """
            rootProject.name = 'included1'
            include("lib2")
        """

        when:
        executer.withArguments(ENABLE_CLI)
        def projectModel = runBuildAction(new FetchGradleProjectForTarget(":included1:lib2"))

        then:
        projectModel != null

        and:
        projectModel.name == "included1"
        projectModel.children.size() == 1
        projectModel.children[0].name == "lib2"
    }

}
