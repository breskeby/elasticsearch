import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId
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

const val useGradleInternalScansServer = "-I gradle/init-scripts/build-scan.init.gradle.kts"

const val buildCacheSetup = "--build-cache -Dgradle.cache.remote.url=%gradle.cache.remote.url% -Dgradle.cache.remote.username=%gradle.cache.remote.username% -Dgradle.cache.remote.password=%gradle.cache.remote.password%"
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

fun stripRootProject(id: String): String {
    return id.replace("${DslContext.projectId.value}_", "")
}
