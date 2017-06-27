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

package uk.gov.hmrc.personalincome.domain.userdata

import org.joda.time.DateTime
import play.api.libs.json.Json

case class PaymentSummary(workingTaxCredit: Option[Payment], childTaxCredit: Option[Payment])

case class Payment(amount: Double, paymentDate: DateTime, paymentFrequency:Option[String])

object Payment {
  implicit val formats = Json.format[Payment]
}

object PaymentSummary {
  def key: String = "payments-data"

  implicit val formats = Json.format[PaymentSummary]
}

case class FuturePaymentSummary(workingTaxCredit: Option[PaymentSection], childTaxCredit: Option[PaymentSection], paymentSummaryEnabled: Boolean)

case class PaymentSection(payments: List[FuturePayment], frequency: String)

case class FuturePayment(amount: Double, paymentDate: DateTime, oneOffPayment: Boolean)


object FuturePayment {
  implicit val formats = Json.format[FuturePayment]
}

object PaymentSection {
  implicit val formats = Json.format[PaymentSection]
}

object FuturePaymentSummary {
  def key: String = "payment-data"

  implicit val formats = Json.format[FuturePaymentSummary]
}