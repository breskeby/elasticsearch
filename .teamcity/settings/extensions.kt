import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.toId

fun BuildType.agentRequirement(os: Os) {
    requirements {
        contains("teamcity.agent.jvm.os.name", os.requirementName)
    }
}

//TODO: FIX for windows
fun ParametrizedWithType.javaDefaults(os: Os) {
    // default javaHomes
    param("env.JAVA7_HOME", "%teamcity.agent.jvm.user.home%/.java/java7%")
    param("env.JAVA8_HOME", "%teamcity.agent.jvm.user.home%/.java/java8")
    param("env.JAVA9_HOME", "%teamcity.agent.jvm.user.home%/.java/java9")
    param("env.JAVA10_HOME", "%teamcity.agent.jvm.user.home%/.java/java10")
    param("env.JAVA11_HOME", "%teamcity.agent.jvm.user.home%/.java/java11")
    param("env.JAVA12_HOME", "%teamcity.agent.jvm.user.home%/.java/openjdk12")
    param("env.JAVA13_HOME", "%teamcity.agent.jvm.user.home%/.java/openjdk13")
    param("env.JAVA14_HOME", "%teamcity.agent.jvm.user.home%/.java/openjdk14")
    param("env.JAVA_HOME", "%teamcity.agent.jvm.user.home%/.java/openjdk14")

    param("env.GRADLE_OPTS", "-XX:+HeapDumpOnOutOfMemoryError -Xmx128m -Xms128m")
    param("GRADLEW", "/gradlew --parallel --scan --build-cache -Dorg.elasticsearch.build.cache.url=https://gradle-enterprise.elastic.co/cache/")
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
        id = RelativeId(name)

        artifactRules = "build/reports/** => reports"
        agentRequirement(Os.linux) // default

        params {
            javaDefaults(Os.linux)
        }

        vcs {
            root(DslContext.settingsRoot)
            checkoutMode = CheckoutMode.ON_AGENT
        }
    }.apply(init)

    this.buildTypesOrderIds += buildType.id!!
    return buildType
}

/**
 * Provides a build type that runs gradle with default parameters already applied
 * */
fun Project.gradleBuildType(buildTypeName: String, init: BuildType.() -> Unit): BuildType {
    return buildType(buildTypeName, init).apply {
        steps.items.forEach {
            // dont know a more elegant way yet
            if(it is GradleBuildStep) {
                it.buildFile = ""
                it.gradleParams = (it.gradleParams ?: "") +
                        " -Dbwc.checkout.align=true" +
                        " -Dorg.elasticsearch.build.cache.push=true" +
                        " -Dscan.capture-task-input-files" +
                        " --parallel" + " --scan" + " --build-cache" +
                        " -Dorg.elasticsearch.build.cache.url=https://gradle-enterprise.elastic.co/cache/" +
                        " -i" +
                        " --stacktrace"
            }
        }
    }
}

fun stripRootProject(id: String): String {
    return id.replace("${DslContext.projectId.value}_", "")
}
