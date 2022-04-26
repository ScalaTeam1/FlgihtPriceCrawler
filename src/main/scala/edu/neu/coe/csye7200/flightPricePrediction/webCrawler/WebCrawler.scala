package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import edu.neu.coe.csye7200.flightPricePrediction.webCrawler.configure.CrawlerContext._
import org.slf4j.{Logger, LoggerFactory}
import com.neu.edu.FlightPricePrediction.db.MongoDBUtils
import com.neu.edu.FlightPricePrediction.pojo.FlightWithDate

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.util._
import scala.util.control.NonFatal

/** @author scalaprof
  */
object WebCrawler extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def generateTasks: (Seq[String], Seq[String], Seq[String]) = {
    val cs = for {
      a <- candidate_cities; b <- candidate_cities
    } yield (a, b)
    val dates = for (n <- 0 to daysToCrawl) yield getNextFewDay(n)
    Tuple3(
      cs.filter(x => x._1 != x._2).map(_._1),
      cs.filter(x => x._1 != x._2).map(_._2),
      dates
    )
  }

  def getNextFewDay(num: Int): String = {
    val date = Calendar.getInstance()
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    date.add(Calendar.DATE, num)
    sdf.format(date.getTime)
  }

  def getFlight(
      org: String,
      dst: String,
      d: String
  ): Future[Seq[flightCleaned]] = {
    for (
      s <- MonadOps.asFuture(
        try {
          Success(EaseMyTrip.search(org, dst, d))
        } catch {
          case NonFatal(e) =>
            Failure(
              WebCrawlerURLException(
                "Error when request to get flight information!",
                e
              )
            )
        }
      )
    ) yield s
  }

  def executeTasks(
      orgs: Seq[String],
      dsts: Seq[String],
      dates: Seq[String]
  ): Future[Seq[Either[Throwable, Seq[flightCleaned]]]] = {
    val flights =
      for ((org, dst) <- orgs.zip(dsts); d <- dates)
        yield MonadOps.sequence(
          getFlight(org, dst, d): Future[Seq[flightCleaned]]
        )
    Future.sequence(flights)
  }

  def crawler(
      orgs: Seq[String],
      dsts: Seq[String],
      dates: Seq[String]
  ): Future[Seq[flightCleaned]] = {
    for (
      us <- MonadOps.flattenRecover(
        executeTasks(orgs, dsts, dates),
        { x =>
          logger.error(s"""crawler: ignoring exception $x ${if (
            x.getCause != null
          ) " with cause " + x.getCause
          else ""}""")
        }
      )
    ) yield us
  }

  val (orgs, dsts, dates) = generateTasks
  val jobs: Future[Seq[flightCleaned]] = crawler(orgs, dsts, dates)

  val result: Seq[flightCleaned] = Await.result(jobs, Duration("60 second"))
  logger.info(s"Start to insert ${result.size} flights")
  MongoDBUtils.insertManyFlightWithDates(
    result.map(_.toFlight).map(FlightWithDate.toFlightWithDate)
  )
  logger.info(s"Succeed to insert ${result.size} flights")
}

case class WebCrawlerURLException(url: String, cause: Throwable)
    extends Exception(s"Web Crawler could not decode URL: $url", cause)

case class WebCrawlerException(msg: String, cause: Throwable)
    extends Exception(msg, cause)

object WebCrawlerException {
  def apply(msg: String): WebCrawlerException = apply(msg, null)
}
