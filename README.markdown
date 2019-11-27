Gnieh Diffson [![Build Status](https://travis-ci.org/gnieh/diffson.png)](https://travis-ci.org/gnieh/diffson) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/9892e2c968974ecb951d21969adbadaa)](https://www.codacy.com/app/satabin/diffson?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=gnieh/diffson&amp;utm_campaign=Badge_Grade) [![Code Coverage](https://codecov.io/github/gnieh/diffson/coverage.svg?branch=master)](https://codecov.io/github/gnieh/diffson?branch=master) [![Maven Central](https://img.shields.io/maven-central/v/org.gnieh/diffson-core_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/org.gnieh/diffson-core_2.13) [![Scaladoc](https://javadoc.io/badge/org.gnieh/diffson-core_2.13.svg)](https://javadoc.io/doc/org.gnieh/diffson-core_2.13)
=============

[![Join the chat at https://gitter.im/gnieh/diffson](https://badges.gitter.im/gnieh/diffson.svg)](https://gitter.im/gnieh/diffson?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A [scala][6] implementation of the [RFC-6901][1], [RFC-6902][2], and [RFC-7396][11].
It also provides methods to compute _diffs_ between two Json values that produce valid Json patches or merge patches.

**Note:** if you still want to use the `3.x.y` series (without cats), please see [this documentation][diffson3]

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents

- [Getting Started](#getting-started)
- [Json Library](#json-library)
- [Json Patch (RFC-6902)](#json-patch-rfc-6902)
  - [Basic Usage](#basic-usage)
  - [Simple diffs](#simple-diffs)
  - [Remembering old values](#remembering-old-values)
- [Json Merge Patches (RFC-7396)](#json-merge-patches-rfc-7396)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Getting Started
---------------

This library is published in the [Maven][7] [Central Repository][8].
You can add it to your sbt project by putting this line into your build description:
```scala
libraryDependencies += "org.gnieh" %% f"diffson-$jsonLib" % "4.0.1"
```

where `jsonLib` is either:

 - `spray-json`
 - `play-json`
 - `circe`

These versions are built for Scala 2.12 and 2.13.

Scala.JS is also supported for both Scala 2.12 and 2.13. To use it, add this dependency to your build file:
```scala
libraryDependencies += "org.gnieh" %%% f"diffson-$jsonLib" % "4.0.1"
```

Json Library
------------

Diffson was first developped for [spray-json][3], however, it is possible to use it with any json library of your liking.
The only requirement is to have a `Jsony` for your json library.
`Jsony` is a type class describing what operations are required to compute diffs and apply patches to Json-like types.

At the moment, diffson provides instances for [spray-json][3], [Play! Json][9], and [circe][10].
To use these implementations you need to link with the correct module and import the instance:

```scala
// spray-json
import diffson.sprayJson._
// play-json
import diffson.playJson._
// circe
import diffson.circe._
```

If you want to add support for your favorite Json library, you may only depend on diffson core module `diffson-core` and all you need to do then is to implement the `diffson.DiffsonInstance` class, which provides the `JsonProvider` for the specific library. Contribution of new Json libraries in this repository are more than welcome.

Json Patch (RFC-6902)
---------------------

### Basic Usage

Although the library is quite small and easy to use, here comes a summary of its basic usage.
Diffson uses a type-class approach based on the [cats][cats] library.
All operations that may fail are wrapped in type with a `MonadError` instance.

There are two different entities living in the `diffson.jsonpatch` and one on `diffson.jsonpointer` package usefull to work with Json patches:
 - `Pointer` which allows to parse and manipulate Json pointers as defined in [RFC-6901][1],
 - `JsonPatch` which allows to parse, create and apply Json patches as defined in [RFC-6902][2],
 - `JsonDiff` which allows to compute the diff between two Json values and create Json patches.

Basically if one wants to compute the diff between two Json objects, on can execute the following:
```scala
import diffson._
import diffson.lcs._
import diffson.circe._
import diffson.jsonpatch._
import diffson.jsonpatch.lcsdiff._

import io.circe._
import io.circe.parser._

import cats._
import cats.implicits._

implicit val lcs = new Patience[Json]

val json1 = parse("""{
                    |  "a": 1,
                    |  "b": true,
                    |  "c": ["test", "plop"]
                    |}""".stripMargin)

val json2 = parse("""{
                    |  "a": 6,
                    |  "c": ["test2", "plop"],
                    |  "d": false
                    |}""".stripMargin)

val patch =
  for {
    json1 <- json1
    json2 <- json2
  } yield diff(json1, json2)
```
which will return a patch that can be serialized in json as:
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
  "path":"/c/0",
  "value":"test2"
},{
  "op":"add",
  "path":"/d",
  "value":false
}]
```

This example computes a diff based on an LCS, so we must provide an implicit instance of `Lcs`.
In that case we used the `Patience` instance, but other could be used.
See package `diffson.lcs` to see what implementations are available by default, or provide your own.

You can then apply an existing patch to a Json object as follows:
```scala
import scala.util.Try

import cats.implicits._

val json2 = patch[Try](json1)
```

which results in a json like:
```json
{
  "d":false,
  "c":"test2",
  "a":6
}
```
which we can easily verify is the same as `json2` modulo reordering of fields.

A patch may fail, this is why the `apply` method wraps the result in an `F[_]` with a `MonadError`.
In this example, we used the standard `Try` class, but any type `F` with the appropriate `MonadError[F, Throwable]` instance in scope can be used.

### Simple diffs

The example above uses an LCS based diff, which makes it possible to have smart diffs for arrays.
However, depending on your use case, this feature might not be what you want:
 - LCS can be intensive to compute if you have huge arrays;
 - you might want to see a modified array as a single `replace` operation.

To do so, instead of importing `diffson.jsonpatch.lcsdiff._`, import `diffson.jsonpatch.simplediff._` and you do not need to provide an `Lcs` instance.
Resulting diff will be bigger in case of different arrays, but quicker to compute.

For instance, the resulting simple diff for the example above is:
```json
[
  {
    "op" : "replace",
    "path" : "/a",
    "value" : 6
  },
  {
    "op" : "remove",
    "path" : "/b"
  },
  {
    "op" : "replace",
    "path" : "/c",
    "value" : [
      "test2",
      "plop"
    ]
  },
  {
    "op" : "add",
    "path" : "/d",
    "value" : false
  }
]
```

Note the `replace` operation for the entire array, instead of the single modified element.

### Remembering old values

Whether you use the LCS based or simple diff, you can make it remember old values for `remove` and `replace` operations.

To that end, you just need to import `diffson.jsonpatch.lcsdiff.remembering._` or `diffson.jsonpatch.simplediff.remembering._` instead.
The generated diff will add an `old` field to `remove` and `replace` operations in the patch, containing the previous version of the field in original object.
Taking the first example with the new import, we have similar code.

```scala
import diffson._
import diffson.lcs._
import diffson.circe._
import diffson.jsonpatch._
import diffson.jsonpatch.lcsdiff.remembering._

import io.circe._
import io.circe.parser._

import cats._
import cats.implicits._

implicit val lcs = new Patience[Json]

val json1 = parse("""{
                    |  "a": 1,
                    |  "b": true,
                    |  "c": ["test", "plop"]
                    |}""".stripMargin)

val json2 = parse("""{
                    |  "a": 6,
                    |  "c": ["test2", "plop"],
                    |  "d": false
                    |}""".stripMargin)

val patch =
  for {
    json1 <- json1
    json2 <- json2
  } yield diff(json1, json2)
```

which results in a result with the old value remembered in the patch:

```json
[
  {
    "op" : "replace",
    "path" : "/a",
    "value" : 6,
    "old" : 1
  },
  {
    "op" : "remove",
    "path" : "/b",
    "old" : true
  },
  {
    "op" : "replace",
    "path" : "/c/0",
    "value" : "test2",
    "old" : "test"
  },
  {
    "op" : "add",
    "path" : "/d",
    "value" : false
  }
]
```

Patches produced with this methods are still valid according to the RFC, as the new field must simply be ignored by implementations that are not aware of this encoding, so interoperability is not broken.

Json Merge Patches (RFC-7396)
-----------------------------

There are two different entities living in the `diffson.jsonmergepatch` package useful to work with Json merge patches:
 - `JsonMergePatch` which allows to parse, create and apply Json merge patches as defined in [RFC-7396][11],
 - `JsonMergeDiff` which allows to compute the diff between two Json values and create Json merge patches.

Basically if one wants to compute the diff between two Json objects, on can execute the following:
```scala
import diffson._
import diffson.circe._
import diffson.jsonmergepatch._

import io.circe.parser._
import io.circe.syntax._

val json1 = parse("""{
              |  "a": 1,
              |  "b": true,
              |  "c": "test"
              |}""".stripMargin)

val json2 = parse("""{
              |  "a": 6,
              |  "c": "test2",
              |  "d": false
              |}""".stripMargin)

val patch =
  for {
    json1 <- json1
    json2 <- json2
  } yield diff(json1, json2)
```
which will return the following Json Merge Patch:
```json
{
  "a": 6,
  "b": null,
  "c": "test2",
  "d": false
}
```
You can then apply the patch to `json1`:
```scala
val json3 = patch(json1)
```

which will create the following Json:
```json
{
  "d":false,
  "c":"test2",
  "a":6
}
```
which we can easily verify is the same as `json2` modulo reordering of fields.

[1]: http://tools.ietf.org/html/rfc6901
[2]: http://tools.ietf.org/html/rfc6902
[3]: https://github.com/spray/spray-json
[4]: http://alfedenzo.livejournal.com/170301.html
[5]: https://en.wikipedia.org/wiki/Longest_common_subsequence_problem
[6]: http://scala-lang.org
[7]: http://maven.apache.org/
[8]: http://search.maven.org/
[9]: https://www.playframework.com/documentation/latest/ScalaJson
[10]: https://circe.github.io/circe/
[11]: http://tools.ietf.org/html/rfc7396
[cats]: https://typelevel.org/cats/
[Foldable]: https://typelevel.org/cats/typeclasses/foldable.html
[diffson3]: https://github.com/gnieh/diffson/tree/v3.1.x
