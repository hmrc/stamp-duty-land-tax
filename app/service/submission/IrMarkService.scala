/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package service.submission

import com.google.inject.Singleton
import org.apache.commons.codec.binary.Base32
import org.apache.xml.security.Init
import org.apache.xml.security.signature.XMLSignatureInput
import org.apache.xml.security.transforms.Transforms
import org.apache.xml.security.transforms.params.XPathContainer
import org.w3c.dom.Document
import play.api.Logging

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import scala.xml.{Elem, Node}

@Singleton
class IrMarkService extends Logging:

  Init.init()

  private val GovTalkEnvelopeNs = "http://www.govtalk.gov.uk/CM/envelope"
  
  private val IrMarkXPath: String =
    "(count(ancestor-or-self::node()|/gt:GovTalkMessage/gt:Body)=count(ancestor-or-self::node()))" +
      " and " +
      "(count(ancestor-or-self::node()|/gt:GovTalkMessage/gt:Body/*[name()='IRenvelope']/*[name()='IRheader']/*[name()='IRmark'])!=count(ancestor-or-self::node()))"

  private val dbf: DocumentBuilderFactory =
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    factory

  def applyIrMark(envelope: Elem): IrMarkResult =
    val (b64, b32) = generateMark(envelope)
    logger.debug(s"[IrMarkService] computed IRmark b64Len=${b64.length} b32Len=${b32.length}")
    val markedEnvelope = spliceMark(envelope, b64)
    IrMarkResult(markedEnvelope, b64, b32)

  def hasMarkChanged(previous: Option[String], current: String): Boolean =
    !previous.contains(current)

  private def generateMark(envelope: Elem): (String, String) =
    try
      require(
        (envelope \\ "IRmark").nonEmpty,
        "Envelope has no <IRmark> placeholder — the IRmark XPath transform requires an " +
          "(empty) IRmark element to be present before the mark is generated."
      )

      val doc = elemToDom(envelope)

      val transforms = new Transforms(doc)
      val xpath = new XPathContainer(doc)
      xpath.setXPathNamespaceContext("gt", GovTalkEnvelopeNs)
      xpath.setXPath(IrMarkXPath)
      transforms.addTransform(Transforms.TRANSFORM_XPATH, xpath.getElement)
      transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS)

      val output    = transforms.performTransforms(new XMLSignatureInput(doc))
      val canonical = output.getBytes

      val digest = MessageDigest.getInstance("SHA-1").digest(canonical)
      val b64    = Base64.getEncoder.encodeToString(digest)
      val b32    = new Base32().encodeToString(digest)
      (b64, b32)
    catch
      case e: Throwable =>
        logger.error("[IrMarkService] IRmark generation failed", e)
        throw new RuntimeException(s"IRmark generation failed: ${e.getMessage}", e)

  private def elemToDom(elem: Elem): Document =
    dbf.newDocumentBuilder().parse(new ByteArrayInputStream(elem.toString.getBytes("UTF-8")))
  
  private def spliceMark(envelope: Elem, mark: String): Elem =
    def rep(children: Seq[Node], replaced: Boolean): Seq[Node] = children match
      case Seq()                                                 => Seq()
      case (e: Elem) +: tail if e.label == "IRmark" && !replaced =>
        <IRmark Type="generic">{mark}</IRmark>.copy(scope = e.scope) +: rep(tail, replaced = true)
      case (e: Elem) +: tail                                     =>
        e.copy(child = rep(e.child, replaced)).asInstanceOf[Node] +: rep(tail, replaced)
      case n +: tail                                             => n +: rep(tail, replaced)
    envelope.copy(child = rep(envelope.child, replaced = false))