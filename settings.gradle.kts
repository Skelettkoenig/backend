rootProject.name = "software-challenge-backend"
rootProject.buildFileName = "gradle/build.gradle.kts"

include("sdk", "server", "plugin", "player", "test-client")
project(":test-client").projectDir = file("helpers/test-client")
