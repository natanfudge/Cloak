import cloak.idea.providerUtils.Intermediary
import org.junit.Test

class IntermediaryTests {
    @Test
    fun `Can get list of intermediary names in 19w45b`() {
        val names = Intermediary.fetch("19w45b")
        assert(names.contains("net/minecraft/class_4581"))
        assert(names.contains("field_20860"))
        assert(names.contains("method_22854"))
    }
}