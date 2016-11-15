Gnieh Diffson [![Build Status](https://travis-ci.org/gnieh/diffson.png)](https://travis-ci.org/gnieh/diffson) [![Code Coverage](https://codecov.io/github/gnieh/diffson/coverage.svg?branch=master)](https://codecov.io/github/gnieh/diffson?branch=master) [![Maven Central](https://img.shields.io/maven-central/v/org.gnieh/diffson_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/org.gnieh/diffson_2.11)
=============

A [scala][6] implementation of the [RFC-6901][1] and [RFC-6902][2].
It also provides methods to compute _diffs_ between two Json values that produce valid Json patches.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents

- [Getting Started](#getting-started)
- [Json Library](#json-library)
- [Basic Usage](#basic-usage)
- [Remembering old values](#remembering-old-values)
- [Patches as Collections of Operations](#patches-as-collections-of-operations)
- [Technical Details](#technical-details)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Getting Started
---------------

This library is published in the [Maven][7] [Central Repository][8] and is compiled against scala 2.11.
You can add it to your sbt project by putting this line into your build description:
```scala
libraryDependencies += "org.gnieh" %% f"diffson-$jsonLib" % "2.1.0"
```

where `jsonLib` is either:

 - `spray-json`
 - `play-json`
 - `circe`

If you are using maven, add the following dependency to your `pom.xml`:
```xml
<dependency>
  <groupId>org.gnieh</groupId>
  <artifactId>diffson-${json.lib}_${scala.version}</artifactId>
  <version>2.1.0</version>
</dependency>
```

These versions are built for Scala 2.11 and 2.12 when the underlying json library is already published for 2.12.

Json Library
------------

Diffson was first developped for [spray-json][3], however, it is possible to use it with any json library of your linking.
The only requirement is to have a `DiffsonInstance` for your json library.

At the moment, diffson provides two instances for [spray-json][3], [Play! Json][9], and [circe][10].
To use these implementations you need to link with the correct module and import the instance:

```scala
// spray-json
import gnieh.diffson.sprayJson._
// play-json
import gnieh.diffson.playJson._
// circe
import gnieh.diffson.circe._
```

If you want to add support for your favorite Json library, you may only depend on diffson core module `diffson-core` and all you need to do then is to implement the `gnieh.diffson.DiffsonInstance` class, which provides the `JsonProvider` for the specific library. Contribution of new Json libraries in this repository are more than welcome.

Basic Usage
-----------

Although the library is quite small and easy to use, here comes a summary of its basic usage.

There are three different entities living in the `gnieh.diffson` package:
 - `JsonPointer` which allows to parse and manipulate Json pointers as defined in [RFC-6901][1],
 - `JsonPatch` which allows to parse, create and apply Json patches as defined in [RFC-6902][2],
 - `JsonDiff` which allows to compute the diff between two Json values and create Json patches.

Basically if one wants to compute the diff between two Json objects, on can execute the following:
```scala
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

val patch = JsonDiff.diff(json1, json2, false)

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

Remembering old values
----------------------

The `diff` method takes three parameter. The third one indicates whether the generated patch remembers removed and replaced values.
When set to `true`, the `Replace` and `Remove` operations take an extra field named `old` giving the old value.
The RFC does not define these fields, but it does not fordbid either to add extra fields. Hence, generated patches can still be interpreted by third party implementations.

Patches as Collections of Operations
------------------------------------

Patches may be seen as collections of operations on which you may which to apply some typical collection functions such as `map`, `flatMap`, filtering, folding, ...

```scala
val patch: JsonPatch = ...

val patch2: JsonPatch =
  for(op @ Add("my" :: "prefix" :: _, _) <- patch)
    yield op
```

Technical Details
-----------------

The _diff_ between two arrays is computed by using the [Patience Diff][4] algorithm to compute the [LCS][5] between both arrays, which is quite simple to implement.

However one can replace the implementation by any other algorithm that implements the `gnieh.diffson.Lcs` trait, e.g.:
```scala
val diff = new JsonDiff(new MyLcsAlgorithm)
```

then use `diff` in lieu of `JsonDiff` in the first usage example.

[1]: http://tools.ietf.org/html/rfc6901
[2]: http://tools.ietf.org/html/rfc6902
[3]: https://github.com/spray/spray-json
[4]: http://alfedenzo.livejournal.com/170301.html
[5]: https://en.wikipedia.org/wiki/Longest_common_subsequence_problem
[6]: http://scala-lang.org
[7]: http://maven.apache.org/
[8]: http://search.maven.org/
[9]: https://www.playframework.com/documentation/latest/ScalaJson
[10]: https://travisbrown.github.io/circe/
