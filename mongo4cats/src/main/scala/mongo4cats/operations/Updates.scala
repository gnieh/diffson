package mongo4cats.operations

// trick to expose the empty updates
object Updates {
  val empty: Update = UpdateBuilder(Nil)
}
