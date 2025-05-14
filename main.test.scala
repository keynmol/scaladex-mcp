//> using platforms scala-native

import sttp.client4.curl.*

class Test extends munit.FunSuite:
  val backend = CurlBackend(verbose = true)

  test("project details"):
    println(scaladex(backend).project("indoorvivants", "sn-bindgen"))

  test("search"):
    println(scaladex(backend).search("bindgen"))
