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
import service.submission.SchemaValidator

import scala.xml.Elem

class SchemaValidatorSpec extends SpecBase {

  private val validator = new SchemaValidator()

  private val sdltNs = "http://www.govtalk.gov.uk/taxation/SDLT/6"

  private def errorsOf(result: Either[Seq[String], Unit]): Seq[String] =
    result.swap.getOrElse(Seq.empty)

  private val validSdlt: Elem =
    <SDLT xmlns={sdltNs}>
      <TransactionDetails>
        <TransactionDescription>F</TransactionDescription>
        <EffectiveDate>2026-01-15</EffectiveDate>
        <RestrictionsAffecting Apply="no"/>
        <LandExchanged Exchanged="no"/>
        <PursuantToOption>no</PursuantToOption>
        <TaxCalculation>
          <ClaimingRelief Claiming="no"/>
          <LinkedTransaction IsLinked="no"/>
          <TotalDue>1000.00</TotalDue>
          <AmountPaid IncludesPenalties="no">1000.00</AmountPaid>
        </TaxCalculation>
      </TransactionDetails>
      <LandDetail>
        <NumberOfProperties>1</NumberOfProperties>
        <Property>
          <PropertyType>03</PropertyType>
          <AddressOfLand AddressExtended="no">
            <PostCode>SW1A 1AA</PostCode>
            <Line>10 Downing Street</Line>
          </AddressOfLand>
          <LAnumber>1234</LAnumber>
          <PlanSubmitted>no</PlanSubmitted>
          <InterestTransfered>FP</InterestTransfered>
        </Property>
      </LandDetail>
      <VendorDetails>
        <NumberOfVendors>1</NumberOfVendors>
        <Vendor>
          <Name><CompanyOrSurname>Smith</CompanyOrSurname></Name>
          <Address><PostCode>SW1A 1AA</PostCode><Line>1 Vendor Road</Line></Address>
        </Vendor>
      </VendorDetails>
      <PurchaserDetails>
        <NumberOfPurchasers>1</NumberOfPurchasers>
        <Purchaser>
          <Name><CompanyOrSurname>Jones</CompanyOrSurname></Name>
          <Address><PostCode>SW1A 1AA</PostCode><Line>2 Purchaser Way</Line></Address>
          <Trustee>no</Trustee>
          <VendorConnected>no</VendorConnected>
          <CertificateAddress>Purchaser</CertificateAddress>
          <AuthoriseAgent>no</AuthoriseAgent>
        </Purchaser>
      </PurchaserDetails>
      <SupplementarySections>
        <SDLT2Count>0</SDLT2Count>
        <SDLT3Count>0</SDLT3Count>
        <SDLT4Count>0</SDLT4Count>
      </SupplementarySections>
    </SDLT>

  private val validIrEnvelope: Elem =
    <IRenvelope xmlns={sdltNs}>
      <IRheader>
        <PeriodEnd>2026-01-31</PeriodEnd>
        <Sender>Agent</Sender>
      </IRheader>
      {validSdlt}
    </IRenvelope>

  private val invalidSdlt: Elem =
    <SDLT xmlns={sdltNs}>
      <TransactionDetails>
        <TransactionDescription>Z</TransactionDescription>
        <EffectiveDate>2026-01-15</EffectiveDate>
        <RestrictionsAffecting Apply="no"/>
        <LandExchanged Exchanged="no"/>
        <PursuantToOption>no</PursuantToOption>
        <TaxCalculation>
          <ClaimingRelief Claiming="no"/>
          <LinkedTransaction IsLinked="no"/>
          <TotalDue>1000.5</TotalDue>
          <AmountPaid IncludesPenalties="no">1000.00</AmountPaid>
        </TaxCalculation>
      </TransactionDetails>
    </SDLT>

  private val invalidIrEnvelope: Elem =
    <IRenvelope xmlns={sdltNs}>
      <IRheader>
        <PeriodEnd>2026-01-31</PeriodEnd>
      </IRheader>
      {validSdlt}
    </IRenvelope>
  
  private val irEnvelopeWithInvalidSdlt: Elem =
    <IRenvelope xmlns={sdltNs}>
      <IRheader>
        <PeriodEnd>2026-01-31</PeriodEnd>
        <Sender>Agent</Sender>
      </IRheader>
      {invalidSdlt}
    </IRenvelope>
  
  private val unknownRoot: Elem =
    <NotAnSdltElement xmlns={sdltNs}><Foo>bar</Foo></NotAnSdltElement>
  
  private val notWellFormed: Elem =
    <SDLT xmlns={sdltNs}>{"\u0000"}</SDLT>

  "SchemaValidator validateSdlt" - {

    "must return Right for a schema-valid SDLT payload" in {
      validator.validateSdlt(validSdlt) mustBe Right(())
    }

    "must return Left with errors for a schema-invalid SDLT payload" in {
      val result = validator.validateSdlt(invalidSdlt)
      result.isLeft mustBe true
      errorsOf(result).nonEmpty mustBe true
    }

    "must accumulate every error in a single pass" in {
      errorsOf(validator.validateSdlt(invalidSdlt)).size must be >= 2
    }

    "must format each error as line, column and message" in {
      val errors = errorsOf(validator.validateSdlt(invalidSdlt))
      errors.nonEmpty mustBe true
      errors.foreach(_ must fullyMatch regex """line -?\d+, col -?\d+: .+""")
    }

    "must return Left for an element the schema does not declare" in {
      val result = validator.validateSdlt(unknownRoot)
      result.isLeft mustBe true
      errorsOf(result).nonEmpty mustBe true
    }

    "must return Left (not throw) for XML containing an illegal character" in {
      val result = validator.validateSdlt(notWellFormed)
      result.isLeft mustBe true
      errorsOf(result).nonEmpty mustBe true
    }

    "must format a fatal parse error the same way as a schema error" in {
      errorsOf(validator.validateSdlt(notWellFormed))
        .foreach(_ must fullyMatch regex """line -?\d+, col -?\d+: .+""")
    }
  }

  "SchemaValidator validateIrEnvelope" - {

    "must return Right for a schema-valid IRenvelope" in {
      validator.validateIrEnvelope(validIrEnvelope) mustBe Right(())
    }

    "must return Left when the IRheader is missing its mandatory Sender" in {
      val result = validator.validateIrEnvelope(invalidIrEnvelope)
      result.isLeft mustBe true
      errorsOf(result).nonEmpty mustBe true
    }

    "must return Left when the embedded SDLT payload is itself invalid" in {
      val result = validator.validateIrEnvelope(irEnvelopeWithInvalidSdlt)
      result.isLeft mustBe true
      errorsOf(result).nonEmpty mustBe true
    }
  }
}