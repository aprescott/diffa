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

package net.lshift.diffa.participants

import org.joda.time.DateTime
import java.lang.String
import net.lshift.diffa.kernel.participants.{RangeGranularity, UpstreamParticipant}
import org.apache.commons.io.IOUtils
import java.io.{FileInputStream, File}
import net.lshift.diffa.kernel.events.{VersionID, UpstreamChangeEvent}

/**
 * Upstream participant implementation backed off the filesystem.
 */
class UpstreamFileParticipant(epName:String, root:String, agentRoot:String) extends FileParticipant(root, agentRoot)
    with UpstreamParticipant {

  def retrieveContent(identifier: String) = {
    val path = new File(rootDir, identifier)
    IOUtils.toString(new FileInputStream(path))
  }

  protected def onFileChange(f: File) = {
    changesClient.onChangeEvent(UpstreamChangeEvent(epName, idFor(f), dateFor(f), dateFor(f), versionFor(f)))
  }
}