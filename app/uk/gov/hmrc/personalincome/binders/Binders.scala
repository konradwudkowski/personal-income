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

package uk.gov.hmrc.personalincome.binders

import uk.gov.hmrc.domain.Nino
import play.api.mvc.PathBindable
import uk.gov.hmrc.personalincome.domain.RenewalReference

object Binders {

  implicit def ninoBinder(implicit stringBinder: PathBindable[String]) = new PathBindable[Nino] {

    def unbind(key: String, nino: Nino): String = stringBinder.unbind(key, nino.value)

    def bind(key: String, value: String): Either[String, Nino] = {
      Nino.isValid(value) match {
        case true => Right(Nino(value))
        case false => Left("ERROR_NINO_INVALID")
      }
    }
  }

  implicit def renewalReferenceBinder(implicit stringBinder: PathBindable[String]) = new PathBindable[RenewalReference] {

    def unbind(key: String, renewalReference: RenewalReference): String = stringBinder.unbind(key, renewalReference.value)

    def bind(key: String, value: String): Either[String, RenewalReference] = { Right(RenewalReference(value)) }
  }
}
