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

package net.lshift.diffa.kernel.config

import reflect.BeanProperty
import net.lshift.diffa.kernel.participants.EasyConstraints._
import scala.collection.JavaConversions._
import net.lshift.diffa.kernel.participants.{CategoryFunction, QueryConstraint, YearlyCategoryFunction}
import net.lshift.diffa.kernel.participants.IntegerCategoryFunction._
import java.util.{TreeMap, SortedMap}

trait ConfigStore {
  def createOrUpdateEndpoint(endpoint: Endpoint): Unit
  def deleteEndpoint(name: String): Unit
  def listEndpoints: Seq[Endpoint]

  def createOrUpdatePair(pairDef: PairDef): Unit
  def deletePair(key: String): Unit

  def createOrUpdateGroup(group: PairGroup): Unit
  def deleteGroup(key: String): Unit
  def listGroups: Seq[GroupContainer]

  def getEndpoint(name: String): Endpoint
  def getPair(key: String): Pair
  def getGroup(key: String): PairGroup
  def getUser(name: String) : User

  def createOrUpdateUser(user: User): Unit
  def deleteUser(name: String): Unit
  def listUsers: Seq[User]

  def getPairsForEndpoint(epName:String):Seq[Pair]

}

case class Endpoint(
  @BeanProperty var name: String,
  @BeanProperty var url: String,
  @BeanProperty var contentType: String,
  @BeanProperty var inboundUrl: String,
  @BeanProperty var online: Boolean) {

  def this() = this(null, null, null, null, false)
}

case class Pair(
  @BeanProperty var key: String = null,
  @BeanProperty var upstream: Endpoint = null,
  @BeanProperty var downstream: Endpoint = null,
  @BeanProperty var group: PairGroup = null,
  @BeanProperty var versionPolicyName: String = null,
  @BeanProperty var matchingTimeout: Int = Pair.NO_MATCHING,
  @BeanProperty var categories: SortedMap[String,String] = new TreeMap[String, String]) {

  def this() = this(null, null, null, null, null, Pair.NO_MATCHING, new TreeMap[String, String])

  /**
   * Fuses a list of runtime attributes together with their
   * static schema bound keys because the static attributes
   * are not transmitted over the wire.
   */
  def schematize(runtimeValues:Seq[String]) = {
    val staticValues = categories.keySet.toList
    (staticValues, runtimeValues).zip.toMap
  }

  def defaultBucketing() : Map[String, CategoryFunction] = {
    categories.map {
      case (name, "date") => name -> YearlyCategoryFunction
      case (name, "int") => name -> AutoNarrowingIntegerCategoryFunction(1000, 10)
    }.toMap
  }

  /**
   * Returns a set of the coarsest unbound query constraints for
   * each of the category types that has been configured for this pair.
   */
  def defaultConstraints() : Seq[QueryConstraint] =
    categories.flatMap({
      case (name, "date") => Some(unconstrainedDate(name))
      case (name, "int") => Some(unconstrainedInt(name))
      case _ => None
    }).toList
}

object Pair {
  val NO_MATCHING = null.asInstanceOf[Int]
}

case class PairGroup(@BeanProperty var key: String) {
  def this() = this(null)
}

case class GroupContainer(@BeanProperty group: PairGroup, @BeanProperty pairs: Array[Pair])

case class PairDef(
  @BeanProperty var pairKey: String,
  @BeanProperty var versionPolicyName: String,
  @BeanProperty var matchingTimeout: Int,
  @BeanProperty var upstreamName: String,
  @BeanProperty var downstreamName: String,
  @BeanProperty var groupKey: String,
  @BeanProperty var categories: java.util.SortedMap[String,String]) {

  def this() = this(null, null, null.asInstanceOf[Int], null, null, null, null)
}

case class User(@BeanProperty var name: String,
                @BeanProperty var email: String) {
  def this() = this(null, null)
}
