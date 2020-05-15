import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.toId

fun BuildType.agentRequirement(os: Os) {
    requirements {
        contains("teamcity.agent.jvm.os.name", os.requirementName)
    }
}

fun ParametrizedWithType.javaHomes(os: Os) {
    // default javaHomes
    param("env.JAVA11_HOME", "%${os.name}.java.openjdk11%")
    param("env.JAVA12_HOME", "%${os.name}.java.openjdk12%")
    param("env.JAVA13_HOME", "%${os.name}.java.openjdk13%")
    param("env.JAVA14_HOME", "%${os.name}.java.openjdk14%")

    param("env.JAVA_HOME", "%${os.name}.java.openjdk14%")
}

/**
 * Creates a new subproject with the given name, automatically deriving the [Project.id] from the name.
 *
 * Using this method also implicitly ensures that subprojects are ordered by creation order.
 */
fun Project.subProject(projectName: String, init: Project.() -> Unit): Project {
    val parent = this
    val subProject = subProject {
        name = projectName
        id = RelativeId(name.toId(stripRootProject(parent.id.toString())))
    }.apply(init)

    this.subProjectsOrder += subProject.id!!

    return subProject
}

fun Project.buildType(buildTypeName: String, init: BuildType.() -> Unit): BuildType {
    val buildType = buildType {
        name = buildTypeName
        id = RelativeId(name.toId(stripRootProject(this@buildType.id.toString())))

        artifactRules = "build/reports/** => reports"
        agentRequirement(Os.linux) // default

        params {
            javaHomes(Os.linux)
        }

        vcs {
            root(DslContext.settingsRoot)
            checkoutMode = CheckoutMode.ON_AGENT
        }
    }.apply(init)

    this.buildTypesOrderIds += buildType.id!!
    return buildType
}

fun Project.gradleBuildType(buildTypeName: String, init: BuildType.() -> Unit): BuildType {
    return buildType(buildTypeName, init).apply {
        steps {
            gradle {
                buildFile = ""
                gradleParams = gradleParams + " -Dbwc.checkout.align=true -Dorg.elasticsearch.build.cache.push=true -Dignore.tests.seed -Dscan.capture-task-input-files"
            }
        }
    }
}


fun stripRootProject(id: String): String {
    return id.replace("${DslContext.projectId.value}_", "")
}
