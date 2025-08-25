import kotlin.properties.ReadOnlyProperty

val projectName by gradlePropertyDelegate()
rootProject.name = projectName

fun gradlePropertyDelegate(): ReadOnlyProperty<Any?, String> =
    ReadOnlyProperty { _, property ->
        providers.gradleProperty(property.name).get()
    }
