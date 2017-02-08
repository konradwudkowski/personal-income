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

package uk.gov.hmrc.model.rti

import uk.gov.hmrc.model.rti.PayFrequency._

/**
 * A single employment, containing a list of payments for a given year
 *
 * @param officeRefNo            office number which the scheme operates from
 * @param accountOfficeReference accounts office reference of the scheme making
 *                               the payment to the employee
 * @param payeRef                Pay As You Earn (PAYE) reference of the scheme making the
 *                               payment to the employee
 * @param payments
 * @param currentPayId           employer's current identification of the employee
 * @param sequenceNumber         along with the associated [[uk.gov.hmrc.model.RtiData.nino]], this
 *                               uniquely identifies a specific employment in NPS
 */
case class RtiEmployment(
                          officeRefNo: String,
                          payeRef: String,
                          accountOfficeReference: String,
                          payments: List[RtiPayment] = Nil,
                          currentPayId: Option[String]= None,
                          sequenceNumber: Int
                          ) {

  /**
   * frequency of payments, on RTI API this is at the payment level, so we take
   * the most recent one.
   */
  def payFrequency: PayFrequency.Value =
    payments.lastOption.map(_.payFrequency).getOrElse(Irregular)

  /**
   * Taxable pay across this employment record, taken from the last payment
   */
  def taxablePayYTD: BigDecimal =
    payments.lastOption.map(_.taxablePayYTD).getOrElse(0)
}
