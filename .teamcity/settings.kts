import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.*

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.2"

project {
    params {
        java14Home(Os.linux)
        text("systemProp.org.gradle.internal.publish.checksums.insecure", "true")
    }

    val checkPart1 = buildType("Check Part 1") {
        steps {
            gradle {
                tasks = "checkPart1"
                buildFile = ""
                gradleParams = "-Dbwc.checkout.align=true -Dorg.elasticsearch.build.cache.push=true -Dignore.tests.seed -Dscan.capture-task-input-files"
            }
        }
    }

    val checkPart2 = buildType("Check Part 2") {
        steps {
            gradle {
                tasks = "checkPart2"
                buildFile = ""
                gradleParams = "-Dbwc.checkout.align=true -Dorg.elasticsearch.build.cache.push=true -Dignore.tests.seed -Dscan.capture-task-input-files"
            }
        }
    }

    buildType("Verify all") {
        triggers.vcs {}

        dependencies {
            snapshot(checkPart1) {
                onDependencyFailure = FailureAction.CANCEL
                onDependencyCancel = FailureAction.CANCEL
            }
            snapshot(checkPart2) {
                onDependencyFailure = FailureAction.CANCEL
                onDependencyCancel = FailureAction.CANCEL
            }
        }
    }

}
