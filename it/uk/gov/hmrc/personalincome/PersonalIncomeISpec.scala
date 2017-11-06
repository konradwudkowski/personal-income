/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personalincome

import play.api.libs.json.JsArray
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personalincome.stubs.{AuthStub, PersonalTaxSummaryStub, TaiStub}
import uk.gov.hmrc.personalincome.support.BaseISpec

class PersonalIncomeISpec extends BaseISpec {

  "GET /income/:nino/tax-summary/:year" should {
    "return a tax summary including additions and reductions" in {
      val nino = Nino("AA000000A")
      val year = 2017
      AuthStub.authRecordExists(nino)
      TaiStub.taxSummaryExists(nino, year)
      PersonalTaxSummaryStub.estimatedIncomeExists(nino)
      PersonalTaxSummaryStub.yourTaxableIncomeExists(nino)

      val response = await(wsUrl(s"/income/${nino.value}/tax-summary/$year")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 200
      }

      val estimatedIncome = response.json \ "estimatedIncomeWrapper" \ "estimatedIncome"

      val additionalTaxTable = (estimatedIncome \ "additionalTaxTable").as[JsArray]
      (additionalTaxTable(0) \ "a").as[String] shouldBe "Child Benefit"
      (additionalTaxTable(0) \ "b").as[String] shouldBe "1,500.00"

      (additionalTaxTable(1) \ "a").as[String] shouldBe "Estimate of the tax you owe this year"
      (additionalTaxTable(1) \ "b").as[String] shouldBe "500.00"

      (estimatedIncome \ "additionalTaxTableTotal").as[String] shouldBe "2,000.00"

      val reductionsTable = (estimatedIncome \ "reductionsTable").as[JsArray]
      (reductionsTable(1) \ "a").as[String] shouldBe "Tax on dividends"
      (reductionsTable(1) \ "b").as[String] shouldBe "-2,000.00"
      (reductionsTable(1) \ "c").as[String] shouldBe "Interest from company dividends is taxed at the dividend ordinary rate (10%) before it is paid to you."

      (estimatedIncome \ "reductionsTableTotal").as[String] shouldBe "-3,040.00"
    }
  }

}