/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.stampdutylandtax.service.submission

import base.SpecBase
import org.apache.commons.codec.binary.Base32
import service.submission.*

import java.util.Base64
import scala.xml.{Comment, Elem, NodeSeq}

class IrMarkServiceSpec extends SpecBase {

  private val service = new IrMarkService()
  
  private def envelope(markerText: String = "PRESENT", irMark: String = ""): Elem =
    <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
      <Header>
        <MessageDetails><Class>IR-SDLT-LTR</Class></MessageDetails>
      </Header>
      <Body>
        <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/SDLT/6">
          <IRheader>
            <Keys><Key Type="STORN">STORN12345</Key></Keys>
            <PeriodEnd>2026-01-31</PeriodEnd>
            <IRmark Type="generic">{irMark}</IRmark>
            <Sender>Agent</Sender>
          </IRheader>
          <SDLTReturn><Marker>{markerText}</Marker></SDLTReturn>
        </IRenvelope>
      </Body>
    </GovTalkMessage>

  private def decodeB64(s: String): Seq[Byte] = Base64.getDecoder.decode(s).toSeq
  private def decodeB32(s: String): Seq[Byte] = new Base32().decode(s).toSeq

  "IrMarkServiceImpl applyIrMark" - {

    "must return non-empty base64 and base32 encodings" in {
      val result = service.applyIrMark(envelope())
      result.base64.nonEmpty mustBe true
      result.base32.nonEmpty mustBe true
    }

    "must produce a base64 mark that decodes to a 20-byte SHA-1 digest" in {
      decodeB64(service.applyIrMark(envelope()).base64).length mustBe 20
    }

    "must produce a base32 mark that decodes to a 20-byte SHA-1 digest" in {
      decodeB32(service.applyIrMark(envelope()).base32).length mustBe 20
    }

    "must produce a 28-character base64 mark (spec form for submission)" in {
      service.applyIrMark(envelope()).base64.length mustBe 28
    }

    "must produce a 32-character base32 mark (spec form for the PDF)" in {
      service.applyIrMark(envelope()).base32.length mustBe 32
    }

    "must encode the same digest bytes in both base64 and base32" in {
      val result = service.applyIrMark(envelope())
      decodeB64(result.base64) mustBe decodeB32(result.base32)
    }

    "must be deterministic for the same envelope" in {
      val first  = service.applyIrMark(envelope())
      val second = service.applyIrMark(envelope())
      first.base64 mustBe second.base64
      first.base32 mustBe second.base32
    }

    "must splice the base64 mark into the IRmark element" in {
      val result = service.applyIrMark(envelope())
      val marks  = result.envelope \\ "IRmark"
      marks.size mustBe 1
      marks.text mustBe result.base64
      (marks \ "@Type").text mustBe "generic"
    }

    "must preserve the rest of the envelope when splicing" in {
      val result = service.applyIrMark(envelope())
      (result.envelope \\ "Class").text mustBe "IR-SDLT-LTR"
      (result.envelope \\ "Sender").text mustBe "Agent"
      (result.envelope \\ "Marker").text mustBe "PRESENT"
      ((result.envelope \\ "Key").head \ "@Type").text mustBe "STORN"
    }

    "must ignore any existing IRmark content when computing the mark" in {
      val fromEmpty     = service.applyIrMark(envelope(irMark = "")).base64
      val fromPopulated = service.applyIrMark(envelope(irMark = "STALE-MARK-VALUE")).base64
      fromPopulated mustBe fromEmpty
    }

    "must be idempotent when re-applied to an already-marked envelope" in {
      val expected    = service.applyIrMark(envelope()).base64
      val markedOnce  = service.applyIrMark(envelope()).envelope
      val markedTwice = service.applyIrMark(markedOnce).base64
      markedTwice mustBe expected
    }

    "must produce a different mark when the body content changes" in {
      val original = service.applyIrMark(envelope(markerText = "PRESENT")).base64
      val changed  = service.applyIrMark(envelope(markerText = "CHANGED")).base64
      changed mustNot equal(original)
    }

    "must treat XML comments as significant (C14N is #WithComments per spec)" in {
      def withBody(body: NodeSeq): Elem =
        <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
          <Body>
            <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/SDLT/6">
              <IRheader><IRmark Type="generic"></IRmark></IRheader>
              <SDLTReturn>{body}</SDLTReturn>
            </IRenvelope>
          </Body>
        </GovTalkMessage>

      val plain     = withBody(<Marker>PRESENT</Marker>)
      val commented = withBody(Comment(" audit note ") ++ <Marker>PRESENT</Marker>)

      service.applyIrMark(commented).base64 mustNot equal(service.applyIrMark(plain).base64)
    }

    "must throw a wrapped RuntimeException when the envelope has no Body" in {
      val noBody: Elem =
        <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
          <Header/>
        </GovTalkMessage>

      val ex = intercept[RuntimeException](service.applyIrMark(noBody))
      ex.getMessage must include("IRmark generation failed")
    }

    "must throw when the Body carries no IRmark placeholder" in {
      val noPlaceholder: Elem =
        <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
          <Body>
            <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/SDLT/6">
              <IRheader><Sender>Agent</Sender></IRheader>
              <SDLTReturn><Marker>PRESENT</Marker></SDLTReturn>
            </IRenvelope>
          </Body>
        </GovTalkMessage>

      val ex = intercept[RuntimeException](service.applyIrMark(noPlaceholder))
      ex.getMessage must include("IRmark generation failed")
      ex.getCause mustBe a[IllegalArgumentException]
    }
    
    "must reproduce the ETS-verified IRmark for the canonical fixture" ignore {
      val etsVerifiedMark = "TODO-PASTE-CHRIS-ETS-ACCEPTED-MARK"
      service.applyIrMark(envelope()).base64 mustBe etsVerifiedMark
    }
  }

  "IrMarkServiceImpl hasMarkChanged" - {

    "must treat a first submission (no previous mark) as changed" in {
      service.hasMarkChanged(None, "any-mark") mustBe true
    }

    "must report no change when the previous mark equals the current one" in {
      service.hasMarkChanged(Some("same-mark"), "same-mark") mustBe false
    }

    "must report a change when the previous mark differs from the current one" in {
      service.hasMarkChanged(Some("old-mark"), "new-mark") mustBe true
    }

    "must treat whitespace as significant" in {
      service.hasMarkChanged(Some("mark "), "mark") mustBe true
    }

    "must report no change for two identical empty marks" in {
      service.hasMarkChanged(Some(""), "") mustBe false
    }
  }
}