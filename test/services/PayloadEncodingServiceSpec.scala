/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import base.BaseSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import utils.JsonFixtures

class PayloadEncodingServiceSpec extends BaseSpec with JsonFixtures with Matchers {

  val service = new PayloadEncodingService

  ".encode" must {

    "return a base64 encoded string when given a Json payload" in {

      val payload = Json.toJson(registrationRequest)

      val result = service.encode(payload)

      result mustBe "eyJtYXRjaERhdGEiOnsidXRyIjoiNTQ1NDU0MTYxNSIsIm5hbWUiOiJNYXJrIFdvbGJlcmciLCJwb3N0Q29kZSI6IlRGMyAyQlgifSwiY29ycmVzcG9uZGVuY2UiOnsiYWJyb2FkSW5kaWNhdG9yIjpmYWxzZSwibmFtZSI6IkFjbWUgSW52ZXN0IiwiYWRkcmVzcyI6eyJsaW5lMSI6IjEzNDQgQXJteSBSb2FkIiwibGluZTIiOiJTdWl0ZSAxMTEiLCJsaW5lNCI6IlRlbGZvcmQiLCJjb3VudHJ5IjoiWlcifSwicGhvbmVOdW1iZXIiOiIwMTIxODIyOTgzIn0sImRlY2xhcmF0aW9uIjp7Im5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiIsIm1pZGRsZU5hbWUiOiJXaWxsaWFtIiwibGFzdE5hbWUiOiJPJ0Nvbm5vciJ9LCJhZGRyZXNzIjp7ImxpbmUxIjoiMTM0NCBBcm15IFJvYWQiLCJsaW5lMiI6IlN1aXRlIDExMSIsImxpbmU0IjoiVGVsZm9yZCIsInBvc3RDb2RlIjoiVEYxIDVEUiIsImNvdW50cnkiOiJHQiJ9fSwieWVhcnNSZXR1cm5zIjp7InJldHVybnMiOlt7InRheFJldHVyblllYXIiOiIxNiIsInRheENvbnNlcXVlbmNlIjpmYWxzZX0seyJ0YXhSZXR1cm5ZZWFyIjoiMTUiLCJ0YXhDb25zZXF1ZW5jZSI6ZmFsc2V9LHsidGF4UmV0dXJuWWVhciI6IjE0IiwidGF4Q29uc2VxdWVuY2UiOmZhbHNlfSx7InRheFJldHVyblllYXIiOiIxMyIsInRheENvbnNlcXVlbmNlIjpmYWxzZX1dfSwiZGV0YWlscyI6eyJ0cnVzdCI6eyJkZXRhaWxzIjp7InN0YXJ0RGF0ZSI6IjIwMTktMDMtMDYiLCJhZG1pbmlzdHJhdGlvbkNvdW50cnkiOiJHQiIsInJlc2lkZW50aWFsU3RhdHVzIjp7InVrIjp7InNjb3R0aXNoTGF3IjpmYWxzZSwicHJlT2ZmU2hvcmUiOiJBRCJ9fSwidHlwZU9mVHJ1c3QiOiJFbXBsb3ltZW50IFJlbGF0ZWQiLCJkZWVkT2ZWYXJpYXRpb24iOiJQcmV2aW91c2x5IHRoZXJlIHdhcyBvbmx5IGFuIGFic29sdXRlIGludGVyZXN0IHVuZGVyIHRoZSB3aWxsIiwiaW50ZXJWaXZvcyI6ZmFsc2UsImVmcmJzU3RhcnREYXRlIjoiMjAxOS0wMy0wNiIsInRydXN0VGF4YWJsZSI6dHJ1ZSwiZXhwcmVzc1RydXN0Ijp0cnVlLCJ0cnVzdFVLUmVzaWRlbnQiOnRydWV9LCJlbnRpdGllcyI6eyJuYXR1cmFsUGVyc29uIjpbeyJuYW1lIjp7ImZpcnN0TmFtZSI6IkpvaG4iLCJtaWRkbGVOYW1lIjoiV2lsbGlhbSIsImxhc3ROYW1lIjoiTydDb25ub3IifSwiZGF0ZU9mQmlydGgiOiIxOTU2LTAyLTEyIiwiaWRlbnRpZmljYXRpb24iOnsicGFzc3BvcnQiOnsibnVtYmVyIjoiYWJjZGVmZ2giLCJleHBpcmF0aW9uRGF0ZSI6IjIwMDEtMDEtMDEiLCJjb3VudHJ5T2ZJc3N1ZSI6IkdCIn0sImFkZHJlc3MiOnsibGluZTEiOiIxMzQ0IEFybXkgUm9hZCIsImxpbmUyIjoiU3VpdGUgMTExIiwibGluZTQiOiJUZWxmb3JkIiwicG9zdENvZGUiOiJURjEgNURSIiwiY291bnRyeSI6IkdCIn19fV0sImJlbmVmaWNpYXJ5Ijp7ImluZGl2aWR1YWxEZXRhaWxzIjpbeyJuYW1lIjp7ImZpcnN0TmFtZSI6IkpvaG4iLCJtaWRkbGVOYW1lIjoiV2lsbGlhbSIsImxhc3ROYW1lIjoiTydDb25ub3IifSwiZGF0ZU9mQmlydGgiOiIyMDAxLTAxLTAxIiwidnVsbmVyYWJsZUJlbmVmaWNpYXJ5Ijp0cnVlLCJiZW5lZmljaWFyeVR5cGUiOiJEaXJlY3RvciIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZSwiYmVuZWZpY2lhcnlTaGFyZU9mSW5jb21lIjoiMTAwIiwiaWRlbnRpZmljYXRpb24iOnsicGFzc3BvcnQiOnsibnVtYmVyIjoiYWJjZGVmZ2giLCJleHBpcmF0aW9uRGF0ZSI6IjIwMDEtMDEtMDEiLCJjb3VudHJ5T2ZJc3N1ZSI6IkdCIn0sImFkZHJlc3MiOnsibGluZTEiOiIxMzQ0IEFybXkgUm9hZCIsImxpbmUyIjoiU3VpdGUgMTExIiwibGluZTQiOiJUZWxmb3JkIiwicG9zdENvZGUiOiJURjEgNURSIiwiY291bnRyeSI6IkdCIn19fV0sImNvbXBhbnkiOlt7Im9yZ2FuaXNhdGlvbk5hbWUiOiIxMjM0NTY3ODkwIFF3RXJUeVVpT3AgLC4oLykmJy0gbmFtZSIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZSwiaWRlbnRpZmljYXRpb24iOnsiYWRkcmVzcyI6eyJsaW5lMSI6IjEyMzQ1Njc4OTAgUXdFclR5VWlPcCAsLigvKSYnLSBuYW1lIiwibGluZTIiOiIxMjM0NTY3ODkwIFF3RXJUeVVpT3AgLC4oLykmJy0gbmFtZSIsInBvc3RDb2RlIjoiWjk5IDJZWSIsImNvdW50cnkiOiJHQiJ9fX0seyJvcmdhbmlzYXRpb25OYW1lIjoiMTIzNDU2Nzg5MCBRd0VyVHlVaU9wICwuKC8pJictIG5hbWUiLCJiZW5lZmljaWFyeURpc2NyZXRpb24iOnRydWV9XSwidHJ1c3QiOlt7Im9yZ2FuaXNhdGlvbk5hbWUiOiIxMjM0NTY3ODkwIFF3RXJUeVVpT3AgLC4oLykmJy0gbmFtZSIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZSwiaWRlbnRpZmljYXRpb24iOnsiYWRkcmVzcyI6eyJsaW5lMSI6InNkZiIsImxpbmUyIjoic2RmIiwiY291bnRyeSI6IlRMIn19fSx7Im9yZ2FuaXNhdGlvbk5hbWUiOiIxMjM0NTY3ODkwIFF3RXJUeVVpT3AgLC4oLykmJy0gbmFtZSIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZX1dLCJjaGFyaXR5IjpbeyJvcmdhbmlzYXRpb25OYW1lIjoiY2hhcml0eSBvcmcgbmFtZSIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZSwiaWRlbnRpZmljYXRpb24iOnsiYWRkcmVzcyI6eyJsaW5lMSI6IjEgU29tZSBIaWdoIFN0cmVldCIsImxpbmUyIjoiTmV3Y2FzdGxlIFVwb24gVHluZSIsInBvc3RDb2RlIjoiWjk5IDJZWSIsImNvdW50cnkiOiJHQiJ9fX0seyJvcmdhbmlzYXRpb25OYW1lIjoiY2hhcml0eSBvcmcgbmFtZSIsImJlbmVmaWNpYXJ5RGlzY3JldGlvbiI6dHJ1ZX1dLCJsYXJnZSI6W3sib3JnYW5pc2F0aW9uTmFtZSI6ImRzIiwiZGVzY3JpcHRpb24iOiJEZXNjcmlwdGlvbiIsImRlc2NyaXB0aW9uMSI6IkRlc2NyaXB0aW9uMSIsImRlc2NyaXB0aW9uMiI6IkRlc2NyaXB0aW9uMiIsImRlc2NyaXB0aW9uMyI6IkRlc2NyaXB0aW9uMyIsImRlc2NyaXB0aW9uNCI6IkRlc2NyaXB0aW9uNCIsIm51bWJlck9mQmVuZWZpY2lhcnkiOiIxIiwiaWRlbnRpZmljYXRpb24iOnsiYWRkcmVzcyI6eyJsaW5lMSI6InNkZiIsImxpbmUyIjoic2RmIiwiY291bnRyeSI6IlRMIn19fSx7Im9yZ2FuaXNhdGlvbk5hbWUiOiJkcyIsImRlc2NyaXB0aW9uIjoiRGVzY3JpcHRpb24iLCJkZXNjcmlwdGlvbjEiOiJEZXNjcmlwdGlvbjEiLCJkZXNjcmlwdGlvbjIiOiJEZXNjcmlwdGlvbjIiLCJkZXNjcmlwdGlvbjMiOiJEZXNjcmlwdGlvbjMiLCJkZXNjcmlwdGlvbjQiOiJEZXNjcmlwdGlvbjQiLCJudW1iZXJPZkJlbmVmaWNpYXJ5IjoiMSJ9XSwib3RoZXIiOlt7ImRlc2NyaXB0aW9uIjoiTG9yZW0gaXBzdW0iLCJhZGRyZXNzIjp7ImxpbmUxIjoiMTM0NCBBcm15IFJvYWQiLCJsaW5lMiI6IlN1aXRlIDExMSIsImxpbmU0IjoiVGVsZm9yZCIsInBvc3RDb2RlIjoiVEYxIDVEUiIsImNvdW50cnkiOiJHQiJ9fV19LCJsZWFkVHJ1c3RlZXMiOnsibmFtZSI6IlRydXN0IFNlcnZpY2VzIExURCIsInBob25lTnVtYmVyIjoiMDc1NDU0NTQ1NCIsImVtYWlsIjoibWVAeW91LmNvbSIsImlkZW50aWZpY2F0aW9uIjp7InV0ciI6IjU0NjU0MTY1NDYifSwiY291bnRyeU9mUmVzaWRlbmNlIjoiR0IifSwidHJ1c3RlZXMiOlt7InRydXN0ZWVJbmQiOnsibmFtZSI6eyJmaXJzdE5hbWUiOiJKb2huIiwibWlkZGxlTmFtZSI6IldpbGxpYW0iLCJsYXN0TmFtZSI6Ik8nQ29ubm9yIn0sImRhdGVPZkJpcnRoIjoiMTk1Ni0wMi0xMiIsInBob25lTnVtYmVyIjoiMDEyMTU0NjU0NiIsImlkZW50aWZpY2F0aW9uIjp7Im5pbm8iOiJTVDEyMzQ1NiJ9fX0seyJ0cnVzdGVlT3JnIjp7Im5hbWUiOiJUcnVzdGVlIE9yZyAxIiwicGhvbmVOdW1iZXIiOiIwMTIxNTQ2NTQ2In19LHsidHJ1c3RlZU9yZyI6eyJuYW1lIjoiVHJ1c3RlZSBPcmcgMSIsInBob25lTnVtYmVyIjoiMDEyMTU0NjU0NiIsImlkZW50aWZpY2F0aW9uIjp7InV0ciI6IjU0NjU0MTY1NDYifX19LHsidHJ1c3RlZU9yZyI6eyJuYW1lIjoiVHJ1c3RlZSBPcmcgMiIsInBob25lTnVtYmVyIjoiMDEyMTU0NjU0NiIsImlkZW50aWZpY2F0aW9uIjp7InV0ciI6IjU0NjU0MTY1NDcifX19XSwicHJvdGVjdG9ycyI6eyJwcm90ZWN0b3JDb21wYW55IjpbeyJuYW1lIjoiUHJvdGVjdG9yIGNvbXBhbnkgMSJ9XX0sInNldHRsb3JzIjp7InNldHRsb3IiOlt7Im5hbWUiOnsiZmlyc3ROYW1lIjoiYWJjZGVmZ2hpamtsIiwibWlkZGxlTmFtZSI6ImFiY2RlZmdoaWprbG1uIiwibGFzdE5hbWUiOiJhYmNkZSJ9LCJkYXRlT2ZCaXJ0aCI6IjIwMDEtMDEtMDEiLCJpZGVudGlmaWNhdGlvbiI6eyJuaW5vIjoiU1QwMTkwOTEifX1dLCJzZXR0bG9yQ29tcGFueSI6W3sibmFtZSI6ImFiY2RlZmdoaWprbG1ub3BxciIsImNvbXBhbnlUeXBlIjoiVHJhZGluZyIsImNvbXBhbnlUaW1lIjpmYWxzZSwiaWRlbnRpZmljYXRpb24iOnsidXRyIjoiMTIzNDU2MTIzNCJ9fV19fSwiYXNzZXRzIjp7Im1vbmV0YXJ5IjpbeyJhc3NldE1vbmV0YXJ5QW1vdW50Ijo5OTk5OTk5OTk5OTl9XSwicHJvcGVydHlPckxhbmQiOlt7ImJ1aWxkaW5nTGFuZE5hbWUiOiJBQ0IgSG91c2UiLCJhZGRyZXNzIjp7ImxpbmUxIjoiMTM0NCBBcm15IFJvYWQiLCJsaW5lMiI6IlN1aXRlIDExMSIsImxpbmU0IjoiVGVsZm9yZCIsInBvc3RDb2RlIjoiVEYxIDVEUiIsImNvdW50cnkiOiJHQiJ9LCJ2YWx1ZUZ1bGwiOjk5OTk5OTk5OTk5OSwidmFsdWVQcmV2aW91cyI6OTk5OTk5OTk5OTk5fV0sInNoYXJlcyI6W3sibnVtYmVyT2ZTaGFyZXMiOiIxMDAiLCJvcmdOYW1lIjoiTW9jayBPcmcgTFREIiwic2hhcmVDbGFzcyI6Ik9yZGluYXJ5IHNoYXJlcyIsInR5cGVPZlNoYXJlIjoiUXVvdGVkIiwidmFsdWUiOjk5OTk5OTk5OTk5OX1dLCJwYXJ0bmVyU2hpcCI6W3siZGVzY3JpcHRpb24iOiJQYXJ0bmVyc2hpcCBUcmFkZSBcXCB7w4Aty7_igJl9XFwtICZgJ14iLCJwYXJ0bmVyc2hpcFN0YXJ0IjoiMjAwMS0wMS0wMSJ9XSwib3RoZXIiOlt7ImRlc2NyaXB0aW9uIjoiTG9yZW0gaXBzdW0gYW51bSIsInZhbHVlIjo5OTk5OTk5OTk5OTl9XX19fSwic3VibWlzc2lvbkRhdGUiOiIyMDIwLTEwLTAxIiwiYWdlbnREZXRhaWxzIjp7ImFybiI6IkFBUk4xMjM0NTY3IiwiYWdlbnROYW1lIjoiTXIgLiB4eXMgYWJjZGUiLCJhZ2VudEFkZHJlc3MiOnsibGluZTEiOiJsaW5lMSIsImxpbmUyIjoibGluZTIiLCJwb3N0Q29kZSI6IlRGMyAyQlgiLCJjb3VudHJ5IjoiR0IifSwiYWdlbnRUZWxlcGhvbmVOdW1iZXIiOiIwNzkxMjE4MDEyMCIsImNsaWVudFJlZmVyZW5jZSI6ImNsaWVudFJlZmVyZW5jZSJ9fQ"
    }
  }

  ".generateChecksum" must {

    "return a Sha256 checksum when given a Json payload" in {

      val payload = Json.toJson(registrationRequest)

      val result = service.generateChecksum(payload)

      result mustBe "f4b63238a8fba697c31fa61056017933b7ea5ede24bc27946dfd6e99c599ee4d"

    }
  }
}
