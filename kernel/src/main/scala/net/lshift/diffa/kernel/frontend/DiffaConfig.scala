/**
 * Copyright (C) 2010-2011 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lshift.diffa.kernel.frontend

import reflect.BeanProperty
import net.lshift.diffa.kernel.config._

/**
 * Describes a complete diffa configuration.
 */
case class DiffaConfig(
  users:Set[User] = Set(),
  properties:Map[String, String] = Map(),
  endpoints:Set[Endpoint] = Set(),
  groups:Set[PairGroup] = Set(),
  pairs:Set[PairDef] = Set(),
  repairActions:Set[Actionable] = Set()
)
