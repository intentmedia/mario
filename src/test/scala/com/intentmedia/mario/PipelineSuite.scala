package com.intentmedia.mario

import com.intentmedia.mario.Pipeline._
import org.scalatest._

class PipelineSuite extends FlatSpec with Matchers {

  def time(f: => Unit) = {
    val s = System.currentTimeMillis
    f
    (System.currentTimeMillis - s).toInt
  }

  trait GeneratorBuilder {
    val generatorResult = 1

    val generator = pipe(generatorResult)
  }

  "A pipeline without dependencies" should "run standalone" in new GeneratorBuilder {
    generator.run() should be(generatorResult)
  }

  "A unary pipeline" should "depend on one matching pipeline to run successfully" in new GeneratorBuilder {
    val transformer = pipe((a: Int) => a + 2, generator)
    transformer.run() should be(generatorResult + 2)
  }

  "A binary pipeline" should "depend on two matching pipelines to run successfully" in new GeneratorBuilder {
    val transformer = pipe((a: Int) => a.toString, generator)
    val binaryTransformer = pipe((a: String, b: Int) => a + b.toString, transformer, generator)
    binaryTransformer.run() should be(generatorResult.toString + generatorResult.toString)
  }

  "runWith" should "execute other pipelines successfully" in new GeneratorBuilder {
    var a = 1
    var b = 1
    val generator1 = pipe(a += 1)
    val generator2 = pipe(b += 2)
    val result = generator.runWith(generator1, generator2)
    result should be(generatorResult)
    a should be(2)
    b should be(3)
  }

  "A pipeline step" should "be executed only once when being a dependency of multiple pipelines" in {
    var a = 1
    val modifier = pipe {
      a += 1
      a
    }
    val transformer1 = pipe((x: Int) => x + 1, modifier)
    val transformer2 = pipe((x: Int) => x + 2, modifier)
    val transformer3 = pipe((x: Int, y: Int) => x + y, transformer1, transformer2)
    val pipelineResult = transformer3.run()
    a should be(2)
    pipelineResult should be(a * 2 + 3)
  }

  "Independent pipelines" should "be executed independently" in {
    val time1 = 100
    val time2 = 200
    val delayedStep1 = pipe(Thread.sleep(time1))
    val delayedStep2 = pipe(Thread.sleep(time2))
    val joinStep = pipe((a: Unit, b: Unit) => {}, delayedStep1, delayedStep2)
    time {
      joinStep.run()
    } should be < time1 + time2
  }

  "A Reducer" should "generate a tree of pipelines reductions" in new GeneratorBuilder {
    generator.setId("A")
    val gen2 = pipe(2).setId("B")
    val gen3 = pipe(3).setId("C")
    val steps = Seq(generator, gen2, gen3)
    val reducer = steps.reduceToPipe((a: Int, b: Int) => a + b)
    reducer.run() should be(6)
    reducer.dependencyTree() should be(
      "Transformer" + "\n" +
        "|- Transformer" + "\n" +
        "|  |- A" + "\n" +
        "|  |- B" + "\n" +
        "|- C")
  }
}