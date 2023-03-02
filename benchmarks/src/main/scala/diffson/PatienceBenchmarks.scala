package diffson

import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.annotations.Measurement

import diffson.circe._
import diffson.jsonpatch.lcsdiff._
import diffson.lcs._

import io.circe.syntax._
import io.circe.Json
import org.openjdk.jmh.annotations.Benchmark

@BenchmarkMode(Array(Mode.Throughput))
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
class PatienceBenchmarks {

  implicit val lcs = new Patience[Json]

  private def createJson(depth: Int, arrayStep: Int) =
    List
      .range(depth, 0, -1)
      .foldLeft(Json.obj("array" := List.range(0, 1000, arrayStep).map(n => Json.obj("n" := n, "other" := "common")))) {
        (acc, idx) =>
          Json.obj(s"key$idx" := acc, "other" := arrayStep)
      }

  def array(size: Int, step: Int) =
    Json.obj("array" := List.range(0, size, step))

  val deep1 =
    createJson(100, 1)

  val deep2 =
    createJson(100, 2)

  val array1 =
    array(1000, 2)

  val array2 =
    array(1000, 1)

  @Benchmark
  def diffArray() =
    diff(array1, array2)

  @Benchmark
  def diffDeep() =
    diff(deep1, deep2)
}
