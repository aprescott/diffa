/**
 * Copyright (C) 2010 LShift Ltd.
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

import net.lshift.diffa.kernel.participants.{UpstreamParticipant, DownstreamParticipant}
import net.lshift.diffa.kernel.events.{PairChangeEvent, ChangeEvent}

/**
 * Policy implementations of this trait provide different mechanism for handling the matching of upstream
 * and downstream events. This functionality is pluggable since different systems may have different views
 * on how to compare version information between participants.
 */
trait VersionPolicy {
  /**
   * Indicates to the policy that a change has occurred within a participant.
   */
  def onChange(evt:PairChangeEvent)

  /**
   * Requests that the policy difference the given participants for the given time range. Differences that are
   * detected will be reported to the listener configured in the policy.
   */
  def difference(pairKey:String, dates:DateConstraint, us:UpstreamParticipant, ds:DownstreamParticipant, listener:DifferencingListener)
}