Gnieh Diffson
=============

A [scala](http://scala-lang.org) implementation of the [RFC-6901][1] and [RFC-6902][2].
It also provides methods to compute _diffs_ between two Json values that produce valid Json patches.

Basic Usage
-----------

Although the library is quite small and easy to use, here comes a summary of its basic usage.

There are three different entities living in the `gnieh.diffson` package:
 - `JsonPointer` which allows to parse and manipulate Json pointers as defined in [RFC-6901][1],
 - `JsonPatch` which allows to parse, create and apply Json patches as defined in [RFC-6902][2],
 - `JsonDiff` which allows to compute the diff between two Json values and create Json patches.

Basically if one wants to compute the diff between two Json objects, on can execute the following:
```scala
import gnieh.diffson._

val json1 = """{
              |  "a": 1,
              |  "b": true,
              |  "c": "test"
              |}""".stripMargin

val json2 = """{
              |  "a": 6,
              |  "c": "test2",
              |  "d": false
              |}""".stripMargin

val patch = JsonDiff.diff(json1, json2)

println(patch)
```
which will print the following in the console:
```json
[{
  "op":"replace",
  "path":"/a",
  "value":6
},{
  "op":"remove",
  "path":"/b"
},{
  "op":"replace",
  "path":"/c",
  "value":"test2"
},{
  "op":"add",
  "path":"/d",
  "value":false
}]
```
You can then apply the patch to `json1`:
```scala
val json3 = patch(json1)
println(json3)
```

which prints something like:
```json
{
  "d":false,
  "c":"test2",
  "a":6
}
```
which we can easily verify is the same as `json2` modulo reordering of fields.

You may also only want to apply existing patches:
```scala
import gnieh.diffson._

val raw = """[
            |  {
            |    "op": "test",
            |    "path": "/a",
            |    "value": 4
            |  }
            |]""".stripMargin

val patch = JsonPatch.parse(raw)

val json1 = """{ "a": 4 }"""
val json2 = """{ "a": 7 }"""
patch(json1) // ok json1 is returned unchanched
patch(json2) // throws PatchException
```

Technical Details
-----------------

The implementation uses [lift-json][3] to manipulate Json objects.

The _diff_ between two arrays is computed by using the [Patience Diff][4] algorithm to compute the [LCS][5] between both arrays, which is quite simple to implement.

However one can replace the implementation by any other algorithm that implements the `gnieh.diffson.Lcs` trait, e.g.:
```scala
val diff = new JsonDiff(new MyLcsAlgorithm)
```

then use `diff` in lieu of `JsonDiff` in the first usage example.

[1]: http://tools.ietf.org/html/rfc6901
[2]: http://tools.ietf.org/html/rfc6902
[3]: https://github.com/lift/framework/tree/master/core/json
[4]: http://alfedenzo.livejournal.com/170301.html
[5]: https://en.wikipedia.org/wiki/Longest_common_subsequence_problem
