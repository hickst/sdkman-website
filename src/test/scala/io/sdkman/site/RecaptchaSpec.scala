package io.sdkman.site

import com.typesafe.scalalogging.LazyLogging
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import ratpack.handling.Context
import ratpack.test.embed.EmbeddedApp
import support.{FutureTransform, Handler}

@RunWith(classOf[JUnitRunner])
class RecaptchaSpec extends WordSpec with Matchers with BeforeAndAfter with OptionValues {
  "Recaptcha" should {
    "return an either right on success with challenge_ts" in {
      val successResponse =
        """
          |{
          |  "success": true,
          |  "challenge_ts": "2018-05-17T12:39:22Z",
          |  "hostname": "beta.sdkman.io"
          |}
        """.stripMargin

      val handler = new TestHandler {
        override lazy val recaptchaUrl = EmbeddedApp.fromHandler(ctx => ctx.render(successResponse)).getAddress
      }

      EmbeddedApp.fromHandler(handler).test { httpClient =>
        httpClient.getText shouldBe "OK"
      }

      handler.success shouldBe true
      handler.hostname.value shouldBe "beta.sdkman.io"
      handler.challengeTs.value shouldBe "2018-05-17T12:39:22Z"
      handler.errorCodes shouldBe None
    }

    "return an either left on failure with error codes" in {

      val failedResponse =
        """
          |{
          |  "success": false,
          |  "error-codes": [
          |    "timeout-or-duplicate"
          |  ]
          |}
        """.stripMargin

      val handler = new TestHandler {
        override lazy val recaptchaUrl = EmbeddedApp.fromHandler(ctx => ctx.render(failedResponse)).getAddress
      }

      EmbeddedApp.fromHandler(handler).test { httpClient =>
        httpClient.getText shouldBe "OK"
      }

      handler.success shouldBe false
      handler.hostname shouldBe None
      handler.challengeTs shouldBe None
      handler.errorCodes.value shouldBe List("timeout-or-duplicate")
    }
  }

  private class TestHandler extends Handler with Recaptcha with Configuration with FutureTransform with LazyLogging {

    var success: Boolean = false
    var challengeTs: Option[String] = None
    var hostname: Option[String] = None
    var errorCodes: Option[List[String]] = None

    override def handles(implicit ctx: Context): Unit = {
      recaptcha(RecaptchaRequest("secret", "response", "remoteIp")).map[RecaptchaResponse] { resp =>
        success = resp.success
        challengeTs = resp.challenge_ts
        hostname = resp.hostname
        errorCodes = resp.`error-codes`
        resp
      }.then(_ => ctx.render("OK"))
    }
  }

}
