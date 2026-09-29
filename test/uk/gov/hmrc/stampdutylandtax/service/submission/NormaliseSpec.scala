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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.stampdutylandtax.service.submission.Normalise.*

class NormaliseSpec extends AnyWordSpec with Matchers:

  "moneyFromString (AS-IS formatCurrency)" should {

    "add .00 to whole pounds" in {
      moneyFromString(Some("191250")) shouldBe Some("191250.00")
      moneyFromString(Some("37"))     shouldBe Some("37.00")
      moneyFromString(Some("0"))      shouldBe Some("0.00")
    }

    "keep an amount already at .00" in {
      moneyFromString(Some("2000000.00")) shouldBe Some("2000000.00")
    }

    "truncate the pence rather than round them" in {
      moneyFromString(Some("450.75")) shouldBe Some("450.00")
      moneyFromString(Some("450.99")) shouldBe Some("450.00")
      moneyFromString(Some("450.5"))  shouldBe Some("450.00")
      moneyFromString(Some("0.99"))   shouldBe Some("0.00")
    }

    "accept thousands separators and surrounding spaces" in {
      moneyFromString(Some("1,000"))        shouldBe Some("1000.00")
      moneyFromString(Some(" 1,234,567.89 ")) shouldBe Some("1234567.00")
    }

    "give None for blank or non-numeric input, so the element is left out" in {
      moneyFromString(None)          shouldBe None
      moneyFromString(Some(""))      shouldBe None
      moneyFromString(Some("   "))   shouldBe None
      moneyFromString(Some("abc"))   shouldBe None
      moneyFromString(Some("£100"))  shouldBe None
    }

    "always match the schema's SDmonetaryStructure pattern" in {
      Seq("0", "1", "37", "450.75", "1,000.50", "9999999999").foreach { in =>
        moneyFromString(Some(in)).get should fullyMatch regex """(([1-9][0-9]*)|0)\.0{2}"""
      }
    }
  }

  "money(BigDecimal)" should {

    "truncate to whole pounds with .00" in {
      money(BigDecimal("15000.49")) shouldBe "15000.00"
      money(BigDecimal("15000.50")) shouldBe "15000.00"
      money(BigDecimal("15000"))    shouldBe "15000.00"
    }

    "never use scientific notation" in {
      money(BigDecimal("1E+6")) shouldBe "1000000.00"
    }
  }

  "isoDate" should {

    "convert dd/MM/yyyy to ISO" in {
      isoDate(Some("10/10/2022")) shouldBe Some("2022-10-10")
    }

    "keep ISO dates" in {
      isoDate(Some("2022-10-10")) shouldBe Some("2022-10-10")
    }

    "give None for anything else" in {
      isoDate(Some("2022/10/10")) shouldBe None
      isoDate(None)               shouldBe None
    }
  }