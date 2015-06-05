package com.intentmedia.mario

import java.util.concurrent.Executors

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

/**
 * Pipelines are objects representing a series of dependent computations.
 * When run, all steps will be executed concurrently.
 * @tparam OUT represents the output type of this pipeline.
 */
trait Pipeline[OUT] {
  self =>

  val dependencies: Seq[Pipeline[_]]

  /**
   * An identifier for distinguishing this object when visualizing the pipeline dependencies.
   */
  var id: String

  def produce(values: Seq[_]): OUT

  override def toString: String = dependencyTree()

  /**
   * A string representation of this pipeline as a dependency graph.
   */
  def dependencyTree(depth: Int = 0): String = {
    val deps = dependencies.map("\n" + _.dependencyTree(depth + 1)).mkString
    val extra = if (depth > 0) "|- " else ""
    (1 until depth).map(_ => "|  ").mkString + extra + id + deps
  }

  /**
   * Creates a new map by merging all dependencies from a sequence of pipelines and the given map
   */
  private def foldPipelines(futures: Map[Pipeline[_], Future[_]],
                            pipelines: Seq[Pipeline[_]] = dependencies)
                           (implicit runner: PipelineRunner): Map[Pipeline[_], Future[_]] = {
    implicit val ec = runner.ec
    pipelines.foldLeft(futures) { (futures, dep) => futures ++ dep.run(futures) }
  }

  def map[A](f: Pipeline[OUT] => A): A = f(this)

  def flatMap[A](f: Pipeline[OUT] => A): A = f(this)

  /**
   * Generates a map of each pipeline step with it's computation as a Future
   */
  def run(futures: Map[Pipeline[_], Future[_]])(implicit runner: PipelineRunner): Map[Pipeline[_], Future[_]] =
    if (futures.contains(this))
      futures
    else {
      val newFeatures = foldPipelines(futures)
      implicit val ec = runner.ec
      val dependenciesFutures: Future[Seq[Any]] = Future.sequence(dependencies map newFeatures)
      val future = Future {
        val values = runner.await(dependenciesFutures)
        produce(values)
      }
      newFeatures + (this -> future)
    }

  /**
   * Executes this pipeline with all it's dependencies.
   * Concurrency is guaranteed for each independent step.
   * Blocks the current thread until all steps are executed.
   * Each step is wrapped on a Future.
   * @return the result of this pipeline step
   */
  def run()(implicit runner: PipelineRunner = PipelineRunner()): OUT =
    runner.run {
      val future = run(Map()).get(this).get
      runner.await(future).asInstanceOf[OUT]
    }

  /**
   * Executes this pipeline with all it's dependencies including the others pipelines provided.
   * Concurrency is guaranteed for each independent step.
   * Blocks the current thread until all steps are executed, will execute this pipeline computation after all others are completed.
   * Each step is wrapped on a Future.
   * @param others a list of pipelines of any type
   * @return the result of this pipeline step
   */
  def runWith(others: Pipeline[_]*)(implicit runner: PipelineRunner = PipelineRunner()): OUT =
    runner.run {
      implicit val ec = runner.ec
      val allPipelines = others :+ this
      val futures: Seq[Future[Any]] = allPipelines map foldPipelines(Map(), allPipelines)
      val future = Future.sequence(futures)
      val results = runner.await(future)
      results.last.asInstanceOf[OUT]
    }

  def setId(newId: String) = {
    id = newId
    this
  }

  /**
   * Creates a subsequent pipeline to this one
   * @param then the function applied for the next pipeline
   * @return a new pipeline dependent to this one
   */
  def andThen[T](then: OUT => T): Pipeline[T] = new Pipeline[T] {
    override val dependencies = Seq(self)

    override var id: String = self.id

    override def produce(values: Seq[_]): T = then(values.head.asInstanceOf[OUT])
  }
}

/**
 * Provides a set of pipeline constructors.
 */
object Pipeline {

  /**
   * Wraps a function into a Pipeline. Required dependencies are determined by the type signature of that function.
   */
  def pipe[A](producer: => A): Pipeline[A] = new Pipeline[A] {
    override val dependencies = Seq()

    override var id: String = "Generator"

    override def produce(values: Seq[_]): A = producer
  }

  def pipe[A, B](producer: A => B, dep: Pipeline[A]): Pipeline[B] = new Pipeline[B] {
    override val dependencies = Seq(dep)

    override var id: String = "Transformer"

    override def produce(values: Seq[_]): B = producer(values.head.asInstanceOf[A])
  }

  def pipe[A, B, C](producer: (A, B) => C, dep1: Pipeline[A], dep2: Pipeline[B]): Pipeline[C] = new Pipeline[C] {
    override val dependencies = Seq(dep1, dep2)

    override var id: String = "Transformer"

    override def produce(values: Seq[_]): C = producer(values.head.asInstanceOf[A], values(1).asInstanceOf[B])
  }

  implicit class PipelineSeqUtils[T](pipelines: Seq[Pipeline[T]]) {
    /**
     * Creates a pipeline. Steps are generated from the applied reductions.
     */
    def reduceToPipe(reducer: (T, T) => T): Pipeline[T] =
      pipelines.reduce(pipe(reducer, _, _))

    /**
     * Groups into a pipeline of a sequence.
     * Pipelines will be executed independently.
     */
    def toPipe = new Pipeline[Seq[T]] {
      override val dependencies = Seq()

      override var id: String = "Sequence"

      override def produce(values: Seq[_]): Seq[T] = pipelines.par.map(_.run).seq
    }
  }

}

/**
 * A simple object for pipelines execution configuration
 * @param maxAwaitDuration maximum time of execution for each step
 * @param threadPool max number of threads
 */
case class PipelineRunner(maxAwaitDuration: Duration = Duration.Inf, threadPool: Int = 10) {
  val executorService = Executors.newFixedThreadPool(threadPool)
  val ec = ExecutionContext.fromExecutor(executorService)

  def run[OUT](block: => OUT): OUT = {
    val result = block
    executorService.shutdown()
    result
  }

  def await[A](a: Awaitable[A]) = Await.result(a, maxAwaitDuration)
}