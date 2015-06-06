# Mario

![image](http://rocketdock.com/images/screenshots/thumbnails/Mario_pipe_trash_f.png)

Mario is a Scala library focused on defining complex data pipelines in a functional, typesafe, and efficient way.

## ![image](http://icons.iconarchive.com/icons/ph03nyx/super-mario/32/Retro-Block-Question-icon.png) Defining  pipelines

Pipelines are very easy to build, using only the `pipe` function. You can construct pipelines with and without depedencies.  Pipelines can be non-linear, but must be acyclic.  The lack of cycles is enforced by the library, so it is impossible to define a cyclic dependency in Mario.

Execution of pipelines is done concurrently, guaranteeing that each step is executed just once.

## ![image](http://icons.iconarchive.com/icons/ph03nyx/super-mario/32/Retro-Flower-Fire-icon.png) Usage

### Import

```scala
import com.intentmedia.mario.Pipeline._
```

### Example

Here is a simple 3 step pipeline:

```scala
// independent step
val step1 = pipe(0 until 100)
// unary dependent step
val step2 = pipe((a: Range) => a.size until 200, step1)
// binary dependent step
val step3 = pipe((a: Range, b: Range) => a ++ b, step1, step2)

step3.run().size
// 200 (step1 will be only executed once)
```

Independent pipelines can be executed using `runWith`:

```scala
val result = step3.runWith(pipe(println("foo")), pipe(println("bar")))
result.run().size
// 200 (will also print "foo" and "bar")
```

Pipelines can be composed using for comprehensions:

```scala
for {
  step1 <- pipe(0 until 100)
  step2 <- pipe((a: Range) => a.size until 200, step1)
  step3 <- pipe((a: Range, b: Range) => a ++ b, step1, step2)
} yield step3.run().size
```

## ![image](http://icons.iconarchive.com/icons/ph03nyx/super-mario/32/Retro-Mushroom-1UP-icon.png) Installation
Add the following to your sbt build:

```scala
libraryDependencies += "com.intentmedia.mario" %% "mario" % "0.1.0"
```

## ![image](http://icons.iconarchive.com/icons/ph03nyx/super-mario/32/Retro-Fire-Ball-icon.png) Roadmap
* Fault tolerance
* Implicit caching (in Spark)
* Web UI
