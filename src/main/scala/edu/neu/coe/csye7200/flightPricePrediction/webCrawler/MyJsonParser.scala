package edu.neu.coe.csye7200.flightPricePrediction.webCrawler

import spray.json.DefaultJsonProtocol.{StringJsonFormat, mapFormat}
import spray.json._

import scala.io.Source

/** @author Caspar
  * @date 2022/4/17 16:58
  */
object MyJsonParser {

  def parse(content: String): JsObject = content.parseJson.asJsObject

  def extract(jo: JsValue, path: List[String]): JsValue = (jo, path) match {
    case (_, Nil) => jo
    case (n: JsArray, h :: t) =>
      if (n.elements.size == 1) extract(n.elements.head.asJsObject.fields(h), t)
      else jo
    case (n: JsValue, h :: t) => extract(n.asJsObject.fields(h), t)
  }

  def read(resourceName: String): Map[String, String] = {
    val content = Source.fromResource(resourceName).mkString
    content.parseJson.convertTo[Map[String, String]]
  }

}
