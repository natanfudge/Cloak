package cloak.idea

import com.intellij.ui.JBColor
import java.awt.Color

object JbColors {
    val Green = staticColor(0, 255, 0)
    val Orange = staticColor(255, 165, 0)
    val Yellow = staticColor(255, 255, 0)
    val Red = staticColor(255, 0, 0)
    val HalfRed = staticColor(128, 0, 0)
}

 fun staticColor(red : Int, green : Int, blue : Int) = JBColor(Color(red,green,blue),Color(red,green,blue))

