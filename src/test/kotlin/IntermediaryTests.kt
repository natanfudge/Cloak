import cloak.fabric.Intermediary
import org.junit.Test

class IntermediaryTests {
    @Test
    fun `Can get list of intermediary names in 19w45b`() {
        val names = Intermediary.fetchExistingNames("19w45b")
        assert(names.classNames.contains("net/minecraft/class_4581"))
        assert(names.fieldNames.containsKey("field_20860"))
        assert(names.methodNames.containsKey("method_22854"))
    }
}