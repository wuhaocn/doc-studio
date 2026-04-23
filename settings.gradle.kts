rootProject.name = "memora-doc"

include("memora-server-common")
include("memora-server-manager")
include("memora-server-start")

project(":memora-server-common").projectDir = file("memora-server/memora-server-common")
project(":memora-server-manager").projectDir = file("memora-server/memora-server-manager")
project(":memora-server-start").projectDir = file("memora-server/memora-server-start")
