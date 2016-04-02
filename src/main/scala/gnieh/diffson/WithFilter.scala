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

class WithFilter(p: Operation => Boolean, patch: JsonPatch) {

  def map(f: Operation => Operation): JsonPatch =
    patch.flatMap(op => if (p(op)) JsonPatch(f(op)) else EmptyPatch)

  def flatMap(f: Operation => JsonPatch): JsonPatch =
    patch.flatMap(op => if (p(op)) f(op) else EmptyPatch)

  def withFilter(p2: Operation => Boolean): WithFilter =
    new WithFilter(op => p(op) && p2(op), patch)

  def foreach(f: Operation => Unit): Unit =
    patch.foreach(op => if (p(op)) f(op))

}
