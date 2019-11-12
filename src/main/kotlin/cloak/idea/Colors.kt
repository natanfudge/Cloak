package cloak.idea

import com.intellij.ui.JBColor
import java.awt.Color

object JbColors {
    val Green = staticColor(0, 200, 0)
}

private fun staticColor(red : Int, green : Int, blue : Int) = JBColor(Color(red,green,blue),Color(red,green,blue))

