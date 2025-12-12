plugins {
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":utils"))
}

application {
    mainClass = "pleroma.repl.ReplKt"
}
