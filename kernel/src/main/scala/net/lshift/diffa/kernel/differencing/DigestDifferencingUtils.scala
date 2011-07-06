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

package net.lshift.diffa.kernel.differencing

import collection.mutable.{ListBuffer, HashMap}
import net.lshift.diffa.kernel.participants._
import net.lshift.diffa.kernel.config.CategoryDescriptor
import net.lshift.diffa.participant.scanning.ScanResultEntry
import scala.collection.JavaConversions._

/**
 * Utility methods for differencing sequences of digests.
 */
object DigestDifferencingUtils {

  def differenceEntities(categories:Map[String, CategoryDescriptor],
                         ds1:Seq[ScanResultEntry],
                         ds2:Seq[ScanResultEntry],
                         constraints:Seq[QueryConstraint]) : Seq[VersionMismatch] = {
    val result = new ListBuffer[VersionMismatch]
    val ds1Ids = indexScanResultById(ds1)
    val ds2Ids = indexScanResultById(ds2)

    ds1Ids.foreach { case (label, ds1Digest) => {
      val (otherMatches, otherDigest, otherDigestUpdated) = ds2Ids.remove(label) match {
        case Some(hs2Digest) => (ds1Digest.getVersion == hs2Digest.getVersion, hs2Digest.getVersion, hs2Digest.getLastUpdated)
        case None => (false, null, null)
      }

      if (!otherMatches) {
        result += VersionMismatch(label, AttributesUtil.toTypedMap(categories, ds1Digest.getAttributes.toMap), ds1Digest.getLastUpdated, ds1Digest.getVersion, otherDigest)
      }
    }}

    ds2Ids.foreach { case (label, hs2Digest) => {
      val otherMatches = ds1Ids.remove(label) match {
        case None => false
        case _    => true    // No need to compare, since we did that above
      }

      if (!otherMatches) {
        result += VersionMismatch(label, AttributesUtil.toTypedMap(categories, hs2Digest.getAttributes.toMap), hs2Digest.getLastUpdated, null, hs2Digest.getVersion)
      }
    }}

    result
  }

  def differenceAggregates(ds1:Seq[ScanResultEntry],
                           ds2:Seq[ScanResultEntry],
                           bucketing:Seq[CategoryFunction],
                           constraints:Seq[QueryConstraint]) : Seq[QueryAction] = {
    var preemptVersionQuery = false
    val results = new ListBuffer[QueryAction]
    val ds1Ids = indexScanResultByAttributeValues(ds1)
    val ds2Ids = indexScanResultByAttributeValues(ds2)
    val namedBuckets = indexFunctionsByAttrName(bucketing)

    def evaluate(partitions:Map[String, String]) = try {
        val empty = ds1.isEmpty || ds2.isEmpty

        val nextConstraints = constraints.map(c => {
          // Narrow the constraints for those that we had partitions for. Non-bucketing constraints stay the same.
          if (bucketing.contains(c.category)) {
            val partition = partitions(c.category)

            namedBuckets(c.category).constrain(c, partition)
          } else {
            c
          }
        })
        val nextBuckets = new ListBuffer[CategoryFunction]
        bucketing.foreach { bucket => {
          bucket.descend match {
            case None =>    // Do nothing - we won't bucket on this next time
            case Some(fun) => nextBuckets += fun
          }
        }}

        if (nextBuckets.size > 0 && !empty) {
          results += AggregateQueryAction(nextBuckets.toSeq, nextConstraints)
        } else {
          results += EntityQueryAction(nextConstraints)
        }
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          "%s { bucketing = %s, constraints = %s, partitions = %s, results = %s }"
            .format(e.getMessage, bucketing, constraints, partitions, results), e)
    }

    ds1Ids.foreach { case (label, ds1Digest) => {
      val (otherMatches, otherDigest) = ds2Ids.remove(label) match {
        case Some(hs2Digest) => (ds1Digest.getVersion == hs2Digest.getVersion, hs2Digest.getVersion)
        case None => (false, null)
      }

      if (!otherMatches) {
        evaluate(ds1Digest.getAttributes.toMap)
      }
    }}

    ds2Ids.foreach { case (label, hs2Digest) => {
      val otherMatches = ds1Ids.remove(label) match {
        case None => false
        case _    => true    // No need to compare, since we did that above
      }

      if (!otherMatches) {
        evaluate(hs2Digest.getAttributes.toMap)
      }
    }}

    if (preemptVersionQuery) {
      List(EntityQueryAction(constraints))
    } else {
      results
    }
  }

  private def indexScanResultById(hs:Seq[ScanResultEntry]) = {
    val res = new HashMap[String, ScanResultEntry]
    hs.foreach(d => res(d.getId) = d)
    res
  }
  private def indexScanResultByAttributeValues(hs:Seq[ScanResultEntry]) = {
    val res = new HashMap[Map[String, String], ScanResultEntry]
    hs.foreach(d => res(d.getAttributes.toMap) = d)
    res
  }
  private def indexFunctionsByAttrName(functions:Seq[CategoryFunction]) =
    functions.map(f => f.getAttributeName -> f).toMap

}
