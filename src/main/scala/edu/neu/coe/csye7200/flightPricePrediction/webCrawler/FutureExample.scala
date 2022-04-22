package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** @author Caspar
  * @date 2022/4/22 01:56
  */
object FutureExample extends App {

  //观察开始时间
  println(new java.util.Date())

  val future1: Future[Int] = Future {
    // 在这个计划中，执行它的线程会首先休眠 3 秒，然后返回一个 Int 值。
    Thread.sleep(3000)
    println("执行 future1")
    4
  }

  val future2: Future[Int] = Future {
    Thread.sleep(2000)
    println("执行 future2")
    5
  }

  val future3: Future[Int] = for {
    x <- future1
    y <- future2
  } yield {
    x + y
  }
  //我们这里使用了 Await 等待结果调用完毕，不限制等待时间。
  println(Await.result(future3, Duration.Inf))
  //观察结束时间
  println(new java.util.Date())

}
