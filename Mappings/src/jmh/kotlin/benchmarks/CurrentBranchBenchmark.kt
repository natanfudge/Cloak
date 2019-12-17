package benchmarks

import cloak.format.mappings.MappingsFile
import cloak.format.mappings.read
import org.eclipse.jgit.api.Git
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.util.concurrent.TimeUnit

private val git = Git.open(File("C:\\Users\\natan\\Desktop\\Cloak\\build\\idea-sandbox\\system\\cloak\\yarn"))

data class Foo(val x : String)
private val foo = Foo("azdfasdf")

@Fork(1)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
open class CurrentBranchBenchmark {
    @Benchmark
    fun getCurrentBranch(bh: Blackhole) {
       bh.consume(foo.x)
    }
}