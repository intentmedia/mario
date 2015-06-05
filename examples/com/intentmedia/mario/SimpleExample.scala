package com.intentmedia.mario

import com.intentmedia.mario.Pipeline._

case class DataA(data: String)

case class DataB(data: String)

case class DataAB(data: (DataA, DataB))

object SimpleExample extends App {
  def collectDataA = {
    println("collectDataA", Thread.currentThread().getId)
    Seq("dataA1", "dataA2")
  }

  def collectDataB = {
    println("collectDataB", Thread.currentThread().getId)
    Seq("dataB1", "dataB2")
  }

  def other = {
    println("other", Thread.currentThread().getId)
    Seq("other1", "other2")
  }

  def transformDataA(in: Seq[String]) = {
    Thread.sleep(5000)
    println("transformDataA", Thread.currentThread().getId)
    in map DataA
  }

  def transformDataB(in: Seq[String]) = {
    Thread.sleep(2000)
    println("transformDataB", Thread.currentThread().getId)
    in map DataB
  }

  def joinData(a: Seq[DataA], b: Seq[DataB]) = {
    println("joinData", Thread.currentThread().getId)
    a zip b map DataAB
  }

  def join2(a: Seq[DataAB], b: Seq[DataA]) = {
    println("boom", Thread.currentThread().getId)
    a
  }

  val result = for {
    step1 <- pipe(collectDataA)
    step2 <- pipe(transformDataA, step1)
  } yield step2.run()

  println(result)

  val result2 = for {
    step1 <- pipe(collectDataA)
    step2 <- pipe(collectDataB)
    step3 <- pipe(transformDataA, step1)
    step4 <- pipe(transformDataB, step2)
    step5 <- pipe(joinData, step3, step4)
    step6 <- pipe(join2, step5, step3)
    step7 <- pipe(other)
  } yield step6.runWith(
      step6.andThen { x => Thread.sleep(5000); println("end") },
      step1.andThen(println),
      step7.andThen(println),
      step2.andThen(println))

  println(result2)
}