rootProject.name = "mittenlib"
include("annotation-processor")
include("core")
include("commands")
include("minimessage")
include("papi")
include("annotation-processor:benchmark")
findProject(":annotation-processor:benchmark")?.name = "benchmark"
