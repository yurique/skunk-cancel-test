package example

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all._
import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import natchez.Trace.Implicits.noop
import org.scalatest._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import skunk.Session
import skunk.codec.all._
import skunk.implicits._

import scala.concurrent.duration._

class ExampleSuite
    extends AsyncFreeSpec
    with BeforeAndAfter
    with Matchers
    with Inspectors
    with EitherValues
    with LoneElement
    with Inside
    with BeforeAndAfterAll
    with AsyncIOSpec
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    DockerImageName.parse("postgres").withTag("12"),
    databaseName = "skunk-test",
    username = "skunk-test",
    password = "skunk-test-pwd"
  )

  private[this] val logger = org.typelevel.log4cats.slf4j.Slf4jLogger.getLogger[IO]

  "skunk session pool" - {
    "cancels during waiting" in {

      IO.ref(List.empty[String]).flatMap { log =>

          def addLog(s: String): IO[Unit] =
            logger.info(s) *>
              log.update(_ :+ s)

          Session
            .pooled[IO](
              host = container.host,
              port = container.mappedPort(5432),
              user = container.username,
              password = container.password.some,
              database = container.databaseName,
              max = 2
            ).use { sessions =>

              def sleep =
                addLog(s"getting session for sleep") *>
                  sessions.use { s =>
                    addLog("got session for sleep") *>
                      s.execute(
                        sql"""select 1 from (select pg_sleep(5)) s""".query(int4)
                      ) <* addLog("sleep done")
                  }

              def select =
                IO.sleep(100.milliseconds) *> // make sure the sleeps get their chance to grab their sessions first
                  addLog(s"getting session for select") *>
                  sessions.use { s =>
                    addLog("got session for select") *>
                      s
                        .execute(
                          sql"""select 1""".query(int4)
                        )
                        .flatTap { result =>
                          addLog(s"select result: $result")
                        }
                  }

              (
                sleep,
                sleep, // all two sessions will be busy until the sleeps are done
                select.start
                  .flatMap { fio =>
                    IO.sleep(200.milliseconds) *>
                      addLog("cancelling select") *>
                      fio.cancel *>
                      fio.join.flatMap { outcome =>
                        addLog(s"select outcome: $outcome")
                      }
                  }
              ).parTupled
            } *> log.get

        }.asserting { log =>
          log shouldBe List(
            "getting session for sleep",
            "getting session for sleep",
            "getting session for select",
            "got session for sleep",
            "got session for sleep",
            "cancelling select",
            "select outcome: Canceled()",
            "sleep done",
            "sleep done",
          )
        }
    }
  }

}
