package support

import java.util.concurrent.TimeUnit

import io.sdkman.repos.Candidate
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{MongoClient, ScalaObservable, _}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Mongo {

  import Helpers._

  lazy val mongoClient = MongoClient("mongodb://localhost:27017")

  lazy val db = mongoClient.getDatabase("sdkman")

  lazy val appCollection = db.getCollection("application")

  def insertAliveOk() = appCollection.insertOne(Document("alive" -> "OK")).results()

  lazy val candidatesCollection = db.getCollection("candidates")

  def insertCandidates(cs: Seq[Candidate]) = cs.foreach(insertCandidate)

  def insertCandidate(c: Candidate) =
    candidatesCollection.insertOne(
      Document(
        "candidate" -> c.candidate,
        "name" -> c.name,
        "description" -> c.description,
        "default" -> c.default,
        "websiteUrl" -> c.websiteUrl,
        "distribution" -> c.distribution))
      .results()

  def dropAllCollections() = {
    appCollection.drop().results()
    candidatesCollection.drop().results()
  }
}

object Helpers {

  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))

    def headResult() = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))

    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }

    def printHeadResult(initial: String = ""): Unit = println(s"$initial${converter(headResult())}")
  }
}