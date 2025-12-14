plugins {
    id("buildlogic.kotlin-library-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":utils"))
}
