package diffson.ujson

case class FieldMissing(fieldName: String)
  extends Exception(s"Expected field `$fieldName`, but it is missing")