//package benchmarks
//
//import cloak.format.mappings.MappingsFile
//import cloak.format.mappings.read
//import org.openjdk.jmh.annotations.*
//import org.openjdk.jmh.infra.Blackhole
//import java.io.File
//import java.util.concurrent.TimeUnit
//
//private val mappings =
//    listOf("Block", "Item", "Items", "ServerPlayPacketListener", "Block2", "Block3", "Block4", "Block5")
//
//private val Dir = ParserBenchmark::class.java.getResource("/")
//
////fun main(){
////    val x = 2
////}
//
//@Fork(1)
//@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//open class ParserBenchmark {
//
//
//    @Benchmark
//    fun benchmarkNormalParse(bh: Blackhole) {
//        mappings.forEach { bh.consume(MappingsFile.read(File("C:\\Users\\natan\\Desktop\\Cloak\\Mappings\\src\\jmh\\resources/$it.mapping"))) }
//    }
//
//}