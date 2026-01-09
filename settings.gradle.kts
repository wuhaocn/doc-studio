rootProject.name = "doc-studio"

include("doc-server-common")
include("doc-server-manager")
include("doc-server-start")

project(":doc-server-common").projectDir = file("doc-server/doc-server-common")
project(":doc-server-manager").projectDir = file("doc-server/doc-server-manager")
project(":doc-server-start").projectDir = file("doc-server/doc-server-start")

