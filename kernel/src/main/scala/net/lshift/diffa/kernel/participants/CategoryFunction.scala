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

package net.lshift.diffa.kernel.participants

/**
 * This is a struct for desceding partition requests. 
 */
case class IntermediateResult(constraint:QueryConstraint, next:CategoryFunction)

/**
 * This is a function definition that can:
 * - Given a value in a given domain, it can determine what partition that value belongs to
 * - Given the value of a partition, it can determine what the relevant upper and lower bounds are for
 *   any further introspection.
 */
trait CategoryFunction {

  /**
   * The external name of this function.
   */
  def name : String

  /**
   * Descends into a more fine-grained partitioning mechanism.
   *
   * If this function returns None, then no more finer grained partitioning is possible.
   * This occurs for example when trying to descend using a category function for an individual entity.   
   */
  def descend : Option[CategoryFunction]

  /**
   * Given the name of a valid partition, return a query constraint that will constrain a request to including
   * only data that exists with the partition.
   */
  def constrain(categoryName:String, partition:String) : QueryConstraint

  /**
   * Indicates whether this function supports bucketing.
   */
  def shouldBucket() : Boolean

  /**
   * Given a particular value from the value domain (encoded as a string), returns the name of the partition it
   * belongs to.
   */
  def owningPartition(value:String) : String
}

/**
 * A special type of function that indicates that no further partitioning should take place.
 *
 */
object IndividualCategoryFunction extends CategoryFunction {
  def name = "individual"
  def descend = None
  def constrain(categoryName:String, partition:String) = new ListQueryConstraint(categoryName, Seq(partition))
  def shouldBucket() = false
  def owningPartition(value:String) = value
}

/**
 * Indicates that the chosen category function is not valid for the values being received
 */
case class InvalidAttributeValueException(msg: String) extends RuntimeException(msg)
