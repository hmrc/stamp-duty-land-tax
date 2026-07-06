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
import service.submission.Normalise

class NormaliseSpec extends SpecBase {
  
  "Normalise.isBlank" - {
    "must be true for None" in {
      Normalise.isBlank(None) mustBe true
    }
    "must be true for an empty string" in {
      Normalise.isBlank(Some("")) mustBe true
    }
    "must be true for a whitespace-only string" in {
      Normalise.isBlank(Some("   ")) mustBe true
    }
    "must be false for a non-blank string" in {
      Normalise.isBlank(Some("x")) mustBe false
    }
    "must be false for a padded non-blank string" in {
      Normalise.isBlank(Some("  x  ")) mustBe false
    }
  }

  "Normalise.nonBlank (Option)" - {
    "must return None for None" in {
      Normalise.nonBlank(None) mustBe None
    }
    "must return None for an empty string" in {
      Normalise.nonBlank(Some("")) mustBe None
    }
    "must return None for a whitespace-only string" in {
      Normalise.nonBlank(Some("   ")) mustBe None
    }
    "must return the trimmed value for a padded string" in {
      Normalise.nonBlank(Some("  hello  ")) mustBe Some("hello")
    }
    "must return the value unchanged when already trimmed" in {
      Normalise.nonBlank(Some("hello")) mustBe Some("hello")
    }
  }
  
  "Normalise.nonBlank (String)" - {
    "must return None for a null string" in {
      Normalise.nonBlank(null.asInstanceOf[String]) mustBe None
    }
    "must return None for an empty string" in {
      Normalise.nonBlank("") mustBe None
    }
    "must return None for a whitespace-only string" in {
      Normalise.nonBlank("   ") mustBe None
    }
    "must return the trimmed value for a padded string" in {
      Normalise.nonBlank("  hello  ") mustBe Some("hello")
    }
  }
  
  "Normalise.isYes" - {
    "must be true for any casing of yes" in {
      Normalise.isYes(Some("yes")) mustBe true
      Normalise.isYes(Some("YES")) mustBe true
      Normalise.isYes(Some("Yes")) mustBe true
    }
    "must be true for any casing of the y shorthand" in {
      Normalise.isYes(Some("y")) mustBe true
      Normalise.isYes(Some("Y")) mustBe true
    }
    "must be true for a padded yes" in {
      Normalise.isYes(Some("  yes  ")) mustBe true
    }
    "must be false for no" in {
      Normalise.isYes(Some("no")) mustBe false
    }
    "must be false for an unrelated value" in {
      Normalise.isYes(Some("true")) mustBe false
    }
    "must be false for None" in {
      Normalise.isYes(None) mustBe false
    }
    "must be false for a blank string" in {
      Normalise.isYes(Some("   ")) mustBe false
    }
  }
  
  "Normalise.yesNo (Option)" - {
    "must encode yes variants to lowercase yes" in {
      Normalise.yesNo(Some("yes")) mustBe Some("yes")
      Normalise.yesNo(Some("YES")) mustBe Some("yes")
      Normalise.yesNo(Some("y")) mustBe Some("yes")
      Normalise.yesNo(Some("Y")) mustBe Some("yes")
    }
    "must encode no variants to lowercase no" in {
      Normalise.yesNo(Some("no")) mustBe Some("no")
      Normalise.yesNo(Some("NO")) mustBe Some("no")
      Normalise.yesNo(Some("n")) mustBe Some("no")
      Normalise.yesNo(Some("N")) mustBe Some("no")
    }
    "must lowercase any other non-blank value" in {
      Normalise.yesNo(Some("Maybe")) mustBe Some("maybe")
    }
    "must trim before encoding" in {
      Normalise.yesNo(Some("  yes  ")) mustBe Some("yes")
    }
    "must return None for None" in {
      Normalise.yesNo(None) mustBe None
    }
    "must return None for a blank string" in {
      Normalise.yesNo(Some("   ")) mustBe None
    }
  }
  
  "Normalise.yesNo (Boolean)" - {
    "must encode true as yes" in {
      Normalise.yesNo(true) mustBe "yes"
    }
    "must encode false as no" in {
      Normalise.yesNo(false) mustBe "no"
    }
  }
  
  "Normalise.money (BigDecimal)" - {
    "must render zero with two decimal places" in {
      Normalise.money(BigDecimal(0)) mustBe "0.00"
    }
    "must pad a whole number to two decimal places" in {
      Normalise.money(BigDecimal(1)) mustBe "1.00"
    }
    "must pad a single decimal place" in {
      Normalise.money(BigDecimal("1.5")) mustBe "1.50"
    }
    "must round half up at the second decimal place" in {
      Normalise.money(BigDecimal("1.005")) mustBe "1.01"
    }
    "must round down below the halfway point" in {
      Normalise.money(BigDecimal("1.004")) mustBe "1.00"
    }
    "must render negatives with two decimal places" in {
      Normalise.money(BigDecimal("-1.5")) mustBe "-1.50"
    }
    "must render large values as plain (non-scientific) strings" in {
      Normalise.money(BigDecimal("1E7")) mustBe "10000000.00"
    }
  }
  
  "Normalise.toMoney" - {
    "must parse a valid decimal string" in {
      Normalise.toMoney("1.50") mustBe Some(BigDecimal("1.50"))
    }
    "must trim before parsing" in {
      Normalise.toMoney("  2  ") mustBe Some(BigDecimal("2"))
    }
    "must return None for a non-numeric string" in {
      Normalise.toMoney("abc") mustBe None
    }
    "must return None for an empty string" in {
      Normalise.toMoney("") mustBe None
    }
    "must return None for a whitespace-only string" in {
      Normalise.toMoney("   ") mustBe None
    }
    "must return None for a value with thousands separators" in {
      Normalise.toMoney("1,000") mustBe None
    }
  }
  
  "Normalise.money (Option)" - {
    "must format a present value" in {
      Normalise.money(Some(BigDecimal("1.5"))) mustBe Some("1.50")
    }
    "must return None for None" in {
      Normalise.money(None) mustBe None
    }
  }

  "Normalise.moneyOrZero" - {
    "must format a present value" in {
      Normalise.moneyOrZero(Some(BigDecimal("2.5"))) mustBe "2.50"
    }
    "must default to 0.00 for None" in {
      Normalise.moneyOrZero(None) mustBe "0.00"
    }
  }


  "Normalise.moneyFromString" - {
    "must parse and format a valid string" in {
      Normalise.moneyFromString(Some("1.5")) mustBe Some("1.50")
    }
    "must trim before parsing" in {
      Normalise.moneyFromString(Some("  2  ")) mustBe Some("2.00")
    }
    "must round half up" in {
      Normalise.moneyFromString(Some("1234.567")) mustBe Some("1234.57")
    }
    "must return None for a non-numeric string" in {
      Normalise.moneyFromString(Some("abc")) mustBe None
    }
    "must return None for None" in {
      Normalise.moneyFromString(None) mustBe None
    }
    "must return None for a blank string" in {
      Normalise.moneyFromString(Some("   ")) mustBe None
    }
  }


  "Normalise.isoDate" - {
    "must pass through a value already in ISO format" in {
      Normalise.isoDate(Some("2026-01-31")) mustBe Some("2026-01-31")
    }
    "must convert a UK dd/MM/yyyy date to ISO" in {
      Normalise.isoDate(Some("31/01/2026")) mustBe Some("2026-01-31")
    }
    "must interpret the UK format as day-first, not month-first" in {
      Normalise.isoDate(Some("05/11/2026")) mustBe Some("2026-11-05")
    }
    "must trim before parsing" in {
      Normalise.isoDate(Some("  31/01/2026  ")) mustBe Some("2026-01-31")
    }
    "must accept a valid leap-year date" in {
      Normalise.isoDate(Some("29/02/2024")) mustBe Some("2024-02-29")
    }
    "must clamp an invalid leap-year date to the last valid day (SMART resolver)" in {
      Normalise.isoDate(Some("29/02/2026")) mustBe Some("2026-02-28")
    }
    "must reject an out-of-range day" in {
      Normalise.isoDate(Some("32/01/2026")) mustBe None
    }
    "must reject a slash-separated non-UK-ordered date" in {
      Normalise.isoDate(Some("2026/01/31")) mustBe None
    }
    "must return None for an unparseable string" in {
      Normalise.isoDate(Some("not-a-date")) mustBe None
    }
    "must return None for None" in {
      Normalise.isoDate(None) mustBe None
    }
    "must return None for a blank string" in {
      Normalise.isoDate(Some("   ")) mustBe None
    }
  }
}