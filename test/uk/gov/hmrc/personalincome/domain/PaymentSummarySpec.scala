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

package uk.gov.hmrc.personalincome.domain

import org.joda.time.DateTime
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.personalincome.domain.userdata.PaymentSummary
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class PaymentSummarySpec extends UnitSpec with WithFakeApplication {

  val now = DateTime.now
  def date(n: Int, func:(Int) ⇒ DateTime): Long = func(n).getMillis
  def payment(amount: Double, paymentDate: Long, oneOffPayment: Boolean): String = {
    s"""{
       | "amount": $amount,
       | "paymentDate": $paymentDate,
       | "oneOffPayment": $oneOffPayment
       |}""".stripMargin
  }
  def total(amount: Double, paymentDate: Long): String = {
    s"""{
       |"amount": $amount,
       |"paymentDate": $paymentDate
       |}""".stripMargin
  }

  "PaymentSummary" should {
    "parse correctly if no wtc or ctc is provided" in {
      val request =
        """{"paymentEnabled":true}""".stripMargin

      val response = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response match {
          case success: JsSuccess[PaymentSummary] => {
            success.get
          }
      }
      paymentSummary.paymentEnabled shouldBe true
      paymentSummary.childTaxCredit shouldBe None
      paymentSummary.workingTaxCredit shouldBe None
      paymentSummary.totalsByDate.isDefined shouldBe false

      Json.stringify(Json.toJson(paymentSummary)) shouldBe request
    }
    "parse correctly if no wtc is provided" in {
      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, date(1, now.plusMonths), false)},
           |    ${payment(55.00, date(2, now.plusMonths), false)},
           |    ${payment(55.00, date(3, now.plusMonths), false)}
           |  ],
           |  "paymentFrequency": "weekly"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(55.00, date(1, now.plusMonths))},
           |  ${total(55.00, date(2, now.plusMonths))},
           |  ${total(55.00, date(3, now.plusMonths))}
           |]
         """.stripMargin

      val request = s"""{$ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.stringify(Json.parse(s"""{ $ctc, "paymentEnabled": true, $totalsByDate }"""))
      val response = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response match {
          case success: JsSuccess[PaymentSummary] => {
            success.get
          }
      }
      paymentSummary.paymentEnabled shouldBe true
      paymentSummary.childTaxCredit.isDefined shouldBe true
      paymentSummary.workingTaxCredit.isDefined shouldBe false
      paymentSummary.totalsByDate.isDefined shouldBe true

      Json.stringify(Json.toJson(paymentSummary)) shouldBe expectedResponse
    }
    "parse correctly if no ctc is provided" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, date(1, now.plusMonths), false)},
           |    ${payment(55.00, date(2, now.plusMonths), false)},
           |    ${payment(55.00, date(3, now.plusMonths), false)}
           |  ],
           |  "paymentFrequency": "weekly"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(55.00, date(1, now.plusMonths))},
           |  ${total(55.00, date(2, now.plusMonths))},
           |  ${total(55.00, date(3, now.plusMonths))}
           |]
         """.stripMargin

      val request = s"""{$wtc,"paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.stringify(Json.parse(s"""{ $wtc, "paymentEnabled": true, $totalsByDate }"""))
      val response = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response match {
        case success: JsSuccess[PaymentSummary] => {
          success.get
        }
      }
      paymentSummary.paymentEnabled shouldBe true
      paymentSummary.childTaxCredit.isDefined shouldBe false
      paymentSummary.workingTaxCredit.isDefined shouldBe true
      paymentSummary.totalsByDate.isEmpty shouldBe false

      Json.stringify(Json.toJson(paymentSummary)) shouldBe expectedResponse
    }
    "parse correctly and sort calculated totalsByDate by Date" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, date(1, now.plusMonths), false)},
           |    ${payment(55.00, date(2, now.plusMonths), false)},
           |    ${payment(55.00, date(3, now.plusMonths), false)}
           |  ],
           |  "paymentFrequency": "weekly"
           |}
         """.stripMargin

      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, date(1, now.plusMonths), false)},
           |    ${payment(55.00, date(2, now.plusMonths), false)},
           |    ${payment(55.00, date(3, now.plusMonths), false)}
           |  ],
           |  "paymentFrequency": "weekly"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(110.00, date(1, now.plusMonths))},
           |  ${total(110.00, date(2, now.plusMonths))},
           |  ${total(110.00, date(3, now.plusMonths))}
           |]
         """.stripMargin

      val request = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.stringify(Json.parse(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate }"""))

      val response = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response match {
        case success: JsSuccess[PaymentSummary] => {
          success.get
        }
      }
      paymentSummary.paymentEnabled shouldBe true
      paymentSummary.childTaxCredit.isDefined shouldBe true
      paymentSummary.workingTaxCredit.isDefined shouldBe true
      paymentSummary.totalsByDate.isDefined shouldBe true

      Json.stringify(Json.toJson(paymentSummary)) shouldBe expectedResponse
    }
  }
  "correctly parse the informationMessage" in {
    val wtc =
      s"""
         |"workingTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(50.00, date(1, now.plusMonths), false)},
         |    ${payment(82.00, date(2, now.plusMonths), false)},
         |    ${payment(82.00, date(3, now.plusMonths), false)}
         |  ],
         |  "paymentFrequency": "weekly"
         |}
         """.stripMargin
    val ctc =
      s"""
         |"childTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(25.00, date(1, now.plusMonths), false)},
         |    ${payment(25.00, date(2, now.plusMonths), false)},
         |    ${payment(50.00, date(3, now.plusMonths), false)}
         |  ],
         |  "paymentFrequency": "weekly"
         |}
         """.stripMargin
    val totalsByDate =
      s"""
         |"totalsByDate": [
         |  ${total(75.00, date(1, now.plusMonths))},
         |  ${total(107.00, date(2, now.plusMonths))},
         |  ${total(132.00, date(3, now.plusMonths))}
         |]
         """.stripMargin

    val request = s"""{$wtc, $ctc, "specialCircumstances": "FTNAE", "paymentEnabled": true}""".stripMargin
    val expectedResponse = Json.stringify(Json.parse(
      s"""{
         |$wtc, $ctc,
         |"paymentEnabled": true,
         |"specialCircumstances":"FTNAE",
         |"informationMessage": "We are currently working out your payments as your child is changing their education or training. This should be done by 7 September 2017. If your child is staying in education or training, update their details on GOV.UK.",
         |$totalsByDate
         |}""".stripMargin))
    val response = Json.parse(request).validate[PaymentSummary]
    val paymentSummary = response match {
      case success: JsSuccess[PaymentSummary] => {
        success.get
      }
    }
    paymentSummary.paymentEnabled shouldBe true
    paymentSummary.childTaxCredit.isDefined shouldBe true
    paymentSummary.workingTaxCredit.isDefined shouldBe true
    paymentSummary.informationMessage.isDefined shouldBe true
    paymentSummary.totalsByDate.isDefined shouldBe true

    Json.stringify(Json.toJson(paymentSummary)) shouldBe expectedResponse
  }
  "correctly parse NO informationMessage if specialCircumstance value is not configured" in {
    val wtc =
      s"""
         |"workingTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(50.00, date(1, now.plusMonths), false)}
         |  ],
         |  "paymentFrequency": "weekly"
         |}
         """.stripMargin
    val ctc =
      s"""
         |"childTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(25.00, date(2, now.plusMonths), false)}
         |  ],
         |  "paymentFrequency": "weekly"
         |}
         """.stripMargin
    val totalsByDate =
      s"""
         |"totalsByDate": [
         |  ${total(50.00, date(1, now.plusMonths))},
         |  ${total(25.00, date(2, now.plusMonths))}
         |]
         """.stripMargin
    val request = s"""{$wtc,$ctc,
                    |  "specialCircumstances": "This key has not been configured",
                    |  "paymentEnabled": true
                    |}""".stripMargin
    val expectedResponse = Json.stringify(Json.parse(
      s"""
         |{$wtc,$ctc,
         |"paymentEnabled": true,
         |"specialCircumstances":"This key has not been configured",
         |$totalsByDate
         |}""".stripMargin))
    val response = Json.parse(request).validate[PaymentSummary]
    val paymentSummary = response match {
      case success: JsSuccess[PaymentSummary] => {
        success.get
      }
    }
    paymentSummary.paymentEnabled shouldBe true
    paymentSummary.childTaxCredit.isDefined shouldBe true
    paymentSummary.workingTaxCredit.isDefined shouldBe true
    paymentSummary.informationMessage.isDefined shouldBe false
    paymentSummary.totalsByDate.isDefined shouldBe true

    Json.stringify(Json.toJson(paymentSummary)) shouldBe expectedResponse
  }


}
