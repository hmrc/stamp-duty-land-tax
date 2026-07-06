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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import service.submission.*
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.xml.Elem

final class GovTalkEnvelopeBuilderSpec extends SpecBase {
  
  private val sdlt: Elem               = <SDLTReturn><Marker>PRESENT</Marker></SDLTReturn>
  private val storn: String            = "STORN12345"
  private val periodEnd: LocalDate     = LocalDate.parse("2026-01-31")
  private val credentialIdentifier: String = "CRED-123"

  private val channelUri: String     = "http://channel/uri"
  private val channelProduct: String = "SDLT-PRODUCT"
  private val channelVersion: String = "1.0.0"

  private val markResult: IrMarkResult = IrMarkResult(<Marked/>, "b64-mark", "b32-mark")

  private def mockServicesConfig(useExternalTestService: Boolean): ServicesConfig = {
    val sc = mock[ServicesConfig]
    when(sc.getString("chris.channel.uri")).thenReturn(channelUri)
    when(sc.getString("chris.channel.product")).thenReturn(channelProduct)
    when(sc.getString("chris.channel.version")).thenReturn(channelVersion)
    when(sc.getBoolean("chris.channel.useExternalTestService")).thenReturn(useExternalTestService)
    sc
  }

  private def newBuilder(
                          irMarkService: IrMarkService,
                          useExternalTestService: Boolean = false
                        ): GovTalkEnvelopeBuilder =
    new GovTalkEnvelopeBuilder(mockServicesConfig(useExternalTestService), irMarkService)

  private def capturedEnvelope(
                                useExternalTestService: Boolean = false,
                                sender: SenderType = SenderType.Agent
                              ): Elem = {
    val irMarkService = mock[IrMarkService]
    val builder       = newBuilder(irMarkService, useExternalTestService)

    when(irMarkService.applyIrMark(any[Elem])).thenReturn(markResult)

    builder.submissionRequest(sdlt, storn, periodEnd, sender, credentialIdentifier)

    val captor = ArgumentCaptor.forClass(classOf[Elem])
    verify(irMarkService).applyIrMark(captor.capture())
    captor.getValue
  }
  
  "GovTalkEnvelopeBuilder submissionRequest" - {

    "must return the IrMarkResult produced by IrMarkService" in {
      val irMarkService = mock[IrMarkService]
      val builder       = newBuilder(irMarkService)

      when(irMarkService.applyIrMark(any[Elem])).thenReturn(markResult)

      val result = builder.submissionRequest(sdlt, storn, periodEnd, SenderType.Agent, credentialIdentifier)
      result mustBe markResult

      verify(irMarkService).applyIrMark(any[Elem])
      verifyNoMoreInteractions(irMarkService)
    }

    "must call IrMarkService.applyIrMark exactly once" in {
      val irMarkService = mock[IrMarkService]
      val builder       = newBuilder(irMarkService)

      when(irMarkService.applyIrMark(any[Elem])).thenReturn(markResult)

      builder.submissionRequest(sdlt, storn, periodEnd, SenderType.Agent, credentialIdentifier)

      verify(irMarkService, times(1)).applyIrMark(any[Elem])
      verifyNoMoreInteractions(irMarkService)
    }

    "must propagate exceptions thrown by IrMarkService" in {
      val irMarkService = mock[IrMarkService]
      val builder       = newBuilder(irMarkService)
      val boom          = new RuntimeException("mark failed")

      when(irMarkService.applyIrMark(any[Elem])).thenThrow(boom)

      val ex = intercept[RuntimeException] {
        builder.submissionRequest(sdlt, storn, periodEnd, SenderType.Agent, credentialIdentifier)
      }
      ex mustBe boom
    }
  }
  
  "GovTalkEnvelopeBuilder envelope" - {

    "must set the GovTalk message details" in {
      val env = capturedEnvelope()
      (env \\ "EnvelopeVersion").text mustBe "2.0"
      (env \\ "Class").text mustBe "IR-SDLT-LTR"
      (env \\ "Qualifier").text mustBe "request"
      (env \\ "Function").text mustBe "submit"
      (env \\ "Transformation").text mustBe "XML"
    }

    "must include the STORN key in both the GovTalkDetails and IRheader" in {
      val keys = env2Keys(capturedEnvelope())
      keys must have size 2
      keys.map(_._1).distinct mustBe Seq("STORN")
      keys.map(_._2).distinct mustBe Seq(storn)
    }

    "must include the channel routing from config" in {
      val env = capturedEnvelope()
      (env \\ "URI").text mustBe channelUri
      (env \\ "Product").text mustBe channelProduct
      (env \\ "Version").text mustBe channelVersion
    }

    "must include the submitter credential identifier" in {
      val env = capturedEnvelope()
      (env \\ "CredentialIdentifier").text mustBe credentialIdentifier
    }

    "must render the IRheader fields" in {
      val env = capturedEnvelope()
      (env \\ "PeriodEnd").text mustBe "2026-01-31"
      (env \\ "DefaultCurrency").text mustBe "GBP"
    }

    "must leave the IRmark element empty and marked as generic before signing" in {
      val env = capturedEnvelope()
      (env \\ "IRmark").text mustBe ""
      ((env \\ "IRmark") \ "@Type").text mustBe "generic"
    }

    "must embed the supplied SDLT body" in {
      val env = capturedEnvelope()
      (env \\ "Marker").text mustBe "PRESENT"
    }

    "must include a GatewayTest element of 1 when useExternalTestService is true" in {
      val env = capturedEnvelope(useExternalTestService = true)
      (env \\ "GatewayTest").text mustBe "1"
    }

    "must omit the GatewayTest element when useExternalTestService is false" in {
      val env = capturedEnvelope(useExternalTestService = false)
      (env \\ "GatewayTest") mustBe empty
    }
  }

  "GovTalkEnvelopeBuilder sender rendering" - {
    SenderType.values.foreach { sender =>
      s"must render the <Sender> value for ${sender.toString}" in {
        val env = capturedEnvelope(sender = sender)
        (env \\ "Sender").text mustBe sender.value
      }
    }
  }
  
  "SenderType" - {

    "must expose the correct SDLT/6 schema value for every case" in {
      val expected = Map(
        SenderType.Individual       -> "Individual",
        SenderType.Company          -> "Company",
        SenderType.Agent            -> "Agent",
        SenderType.Bureau           -> "Bureau",
        SenderType.Partnership      -> "Partnership",
        SenderType.Trust            -> "Trust",
        SenderType.Employer         -> "Employer",
        SenderType.Government       -> "Government",
        SenderType.ActingInCapacity -> "Acting in Capacity",
        SenderType.Other            -> "Other"
      )

      SenderType.values.foreach { st =>
        st.value mustBe expected(st)
      }
    }

    "must define exactly ten cases" in {
      SenderType.values.length mustBe 10
    }
  }
  
  private def env2Keys(env: Elem): Seq[(String, String)] =
    (env \\ "Key").map(k => ((k \ "@Type").text, k.text))
}