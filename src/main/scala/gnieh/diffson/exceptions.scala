/*
* This file is part of the diffson project.
* Copyright (c) 2016 Lucas Satabin
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

class DiffsonException(msg: String) extends Exception(msg)

class PatchException(msg: String) extends DiffsonException(msg)

class PointerException(msg: String) extends DiffsonException(msg)

class FormatException(msg: String) extends DiffsonException(msg)

class SchemaException(msg: String) extends DiffsonException(msg)
