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

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.xml.{Elem, NodeSeq}

enum SenderType(val value: String):
  case Individual       extends SenderType("Individual")
  case Company          extends SenderType("Company")
  case Agent            extends SenderType("Agent")
  case Bureau           extends SenderType("Bureau")
  case Partnership      extends SenderType("Partnership")
  case Trust            extends SenderType("Trust")
  case Employer         extends SenderType("Employer")
  case Government       extends SenderType("Government")
  case ActingInCapacity extends SenderType("Acting in Capacity")
  case Other            extends SenderType("Other")

final case class IrMarkResult(envelope: Elem, base64: String, base32: String)

@Singleton
class GovTalkEnvelopeBuilder @Inject() (
                                         servicesConfig: ServicesConfig,
                                         irMarkService: IrMarkService
                                       ):

  private val EnvelopeNs  = "http://www.govtalk.gov.uk/CM/envelope"
  private val SdltNs      = "http://www.govtalk.gov.uk/taxation/SDLT/6"
  private val SubmitterNs = "http://www.govtalk.gov.uk/gateway/submitterdetails"
  private val SdltClass   = "IR-SDLT-LTR"

  private val channelUri:             String  = servicesConfig.getString("chris.channel.uri")
  private val channelProduct:         String  = servicesConfig.getString("chris.channel.product")
  private val channelVersion:         String  = servicesConfig.getString("chris.channel.version")
  private val useExternalTestService: Boolean = servicesConfig.getBoolean("chris.channel.useExternalTestService")
  
  def submissionRequest(
                         sdlt: Elem,
                         storn: String,
                         periodEnd: LocalDate,
                         sender: SenderType,
                         credentialIdentifier: String,
                         correlationId: String
                       ): IrMarkResult =
    val unmarkedEnvelope = buildEnvelope(sdlt, storn, periodEnd, sender, credentialIdentifier, correlationId)
    irMarkService.applyIrMark(unmarkedEnvelope)

  private def buildEnvelope(
                             sdlt: Elem,
                             storn: String,
                             periodEnd: LocalDate,
                             sender: SenderType,
                             credentialIdentifier: String,
                             correlationId: String
                           ): Elem =
    val irEnvelope = buildIrEnvelope(sdlt, storn, periodEnd, sender)
    <GovTalkMessage xmlns={EnvelopeNs}>
      <EnvelopeVersion>2.0</EnvelopeVersion>
      <Header>
        <MessageDetails>
          <Class>{SdltClass}</Class>
          <Qualifier>request</Qualifier>
          <Function>submit</Function>
          <CorrelationID>{correlationId}</CorrelationID>
          <Transformation>XML</Transformation>
          {gatewayTestElement}
          <GatewayTimestamp></GatewayTimestamp>
        </MessageDetails>
        <SenderDetails></SenderDetails>
      </Header>
      <GovTalkDetails>
        <Keys>
          <Key Type="STORN">{storn}</Key>
        </Keys>
        <ChannelRouting>
          <Channel>
            <URI>{channelUri}</URI>
            <Product>{channelProduct}</Product>
            <Version>{channelVersion}</Version>
          </Channel>
        </ChannelRouting>
        <GatewayAdditions>
          <Submitter xmlns={SubmitterNs}>
            <AgentDetails></AgentDetails>
            <SubmitterDetails>
              <RegistrationCategory>Organisation</RegistrationCategory>
              <UserType>Principal</UserType>
              <CredentialRole>User</CredentialRole>
              <CredentialIdentifier>{credentialIdentifier}</CredentialIdentifier>
            </SubmitterDetails>
          </Submitter>
        </GatewayAdditions>
      </GovTalkDetails>
      <Body>{irEnvelope}</Body>
    </GovTalkMessage>

  private def gatewayTestElement: NodeSeq =
    if useExternalTestService then <GatewayTest>1</GatewayTest>
    else NodeSeq.Empty

  private def buildIrEnvelope(sdlt: Elem, storn: String, periodEnd: LocalDate, sender: SenderType): Elem =
    <IRenvelope xmlns={SdltNs}>
      <IRheader>
        <Keys><Key Type="STORN">{storn}</Key></Keys>
        <PeriodEnd>{periodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)}</PeriodEnd>
        <DefaultCurrency>GBP</DefaultCurrency>
        <IRmark Type="generic"></IRmark>
        <Sender>{sender.value}</Sender>
      </IRheader>
      {sdlt}
    </IRenvelope>