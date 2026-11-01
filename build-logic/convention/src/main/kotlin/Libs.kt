import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.catalog get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
internal fun Project.lib(alias: String) = catalog.findLibrary(alias).get()

object Sdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 26
}
