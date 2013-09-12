/*
* This file is part of the diffson project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.diffson

import net.liftweb.json._

object JsonPatchSerializer extends Serializer[JsonPatch] {

  private val JsonPatchClass = classOf[JsonPatch]

  private def extractOp(raw: JObject): Operation = raw \ "op" match {
    case JString("add")     => raw.extract[RawAdd].resolve
    case JString("remove")  => raw.extract[RawRemove].resolve
    case JString("replace") => raw.extract[RawReplace].resolve
    case JString("move")    => raw.extract[RawMove].resolve
    case JString("copy")    => raw.extract[RawCopy].resolve
    case JString("test")    => raw.extract[RawTest].resolve
    case op                 => throw new PatchException("unknown operation '" + pp(op))
  }

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), JsonPatch] = {
    case (TypeInfo(JsonPatchClass, _), json) =>
      val raw = json.extract[List[JObject]](format, manifest[List[JObject]])
      JsonPatch(raw map (extractOp _))
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case JsonPatch(ops) =>
      JArray(ops map (_.toJson))
  }

}
