/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.tasks.compile

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.DirectoryBuildCacheFixture
import org.gradle.test.fixtures.file.TestFile

abstract class AbstractCachedCompileIntegrationTest extends AbstractIntegrationSpec implements DirectoryBuildCacheFixture {
    def setup() {
        setupProjectInDirectory()
    }

    abstract setupProjectInDirectory(TestFile project = temporaryFolder.testDirectory)
    abstract String getCompilationTask()
    abstract String getCompiledFile()

    def 'compilation can be cached'() {
        when:
        withBuildCache().run compilationTask

        then:
        compileIsNotCached()

        when:
        withBuildCache().run 'clean', 'run'

        then:
        compileIsCached()
    }

    def "compilation is cached if the build executed from a different directory"() {
        // Compile in a different copy of the project
        def remoteProjectDir = file("remote-project")
        setupProjectInDirectory(remoteProjectDir)

        when:
        executer.inDirectory(remoteProjectDir)
        withBuildCache().run compilationTask
        then:
        compileIsNotCached()
        remoteProjectDir.file(getCompiledFile()).exists()

        // Remove the project completely
        remoteProjectDir.deleteDir()

        when:
        // Move the dependencies around by using a new Gradle user home
        executer.requireOwnGradleUserHomeDir()
        withBuildCache().run compilationTask
        then:
        compileIsCached()
    }

    void compileIsCached() {
        assert skippedTasks.contains(compilationTask)
        assert file(compiledFile).exists()
    }

    void compileIsNotCached() {
        assert !skippedTasks.contains(compilationTask)
    }

    def populateCache() {
        def remoteProjectDir = file("remote-project")
        setupProjectInDirectory(remoteProjectDir)
        executer.inDirectory(remoteProjectDir)
        withBuildCache().run compilationTask
        compileIsNotCached()
        // Remove the project completely
        remoteProjectDir.deleteDir()
    }
}
