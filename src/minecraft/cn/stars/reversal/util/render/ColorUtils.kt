/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package cn.stars.reversal.util.render

import net.minecraft.util.ChatAllowedCharacters
import java.awt.Color
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ColorUtils {

    private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

    fun interpolate(oldValue: Double, newValue: Double, interpolationValue: Double): Double {
        return oldValue + (newValue - oldValue) * interpolationValue
    }

    fun interpolateInt(oldValue: Int, newValue: Int, interpolationValue: Double): Int {
        return interpolate(oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat().toDouble()).toInt()
    }

    fun interpolateColorC(color1: Color, color2: Color, amount: Float): Color {
        var amount = amount
        amount = 1f.coerceAtMost(0f.coerceAtLeast(amount))
        return Color(
            interpolateInt(color1.red, color2.red, amount.toDouble()),
            interpolateInt(color1.green, color2.green, amount.toDouble()),
            interpolateInt(color1.blue, color2.blue, amount.toDouble()),
            interpolateInt(color1.alpha, color2.alpha, amount.toDouble())
        )
    }
    fun interpolateFloat(oldValue: Float, newValue: Float, interpolationValue: Double): Float {
        return interpolate(
            oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat()
                .toDouble()
        ).toFloat()
    }
    fun interpolateColorHue(color1: Color, color2: Color, amount: Float): Color {
        var amount = amount
        amount = Math.min(1f, Math.max(0f, amount))
        val color1HSB = Color.RGBtoHSB(color1.red, color1.green, color1.blue, null)
        val color2HSB = Color.RGBtoHSB(color2.red, color2.green, color2.blue, null)
        val resultColor = Color.getHSBColor(
            interpolateFloat(
                color1HSB[0], color2HSB[0], amount.toDouble()
            ),
            interpolateFloat(
                color1HSB[1],
                color2HSB[1],
                amount.toDouble()
            ), interpolateFloat(
                color1HSB[2], color2HSB[2], amount.toDouble()
            )
        )
        return reAlpha(
            resultColor,
            interpolateInt(
                color1.alpha,
                color2.alpha,
                amount.toDouble()
            ) / 255f
        )
    }
    fun interpolateColor(color1: Int, color2: Int, amount: Float): Int {
        var amount = amount
        amount = min(1.0, max(0.0, amount.toDouble())).toFloat()
        val cColor1 = Color(color1)
        val cColor2 = Color(color2)
        return interpolateColorC(cColor1, cColor2, amount).rgb
    }
    fun interpolateColorsBackAndForth(speed: Int, index: Int, start: Color, end: Color, trueColor: Boolean): Color {
        var angle = ((System.currentTimeMillis() / speed + index) % 360).toInt()
        angle = (if (angle >= 180) 360 - angle else angle) * 2
        return if (trueColor) interpolateColorHue(
            start,
            end,
            angle / 360f
        ) else interpolateColorC(start, end, angle / 360f)
    }

    fun applyOpacity(color: Int, opacity: Float): Int {
        val old = Color(color)
        return applyOpacity(old, opacity).rgb
    }

    //Opacity value ranges from 0-1
    fun applyOpacity(color: Color, opacity: Float): Color {
        var opacity = opacity
        opacity = Math.min(1f, Math.max(0f, opacity))
        return Color(color.red, color.green, color.blue, (color.alpha * opacity).toInt())
    }

    fun stripColor(input: String): String {
        return COLOR_PATTERN.matcher(input).replaceAll("")
    }

    @JvmStatic
    fun blend(color1: Color, color2: Color, ratio: Double): Color {
        val r = ratio.toFloat()
        val ir = 1.0f - r
        val rgb1 = FloatArray(3)
        val rgb2 = FloatArray(3)
        color1.getColorComponents(rgb1)
        color2.getColorComponents(rgb2)
        return Color(
            rgb1[0] * r + rgb2[0] * ir,
            rgb1[1] * r + rgb2[1] * ir,
            rgb1[2] * r + rgb2[2] * ir
        )
    }

    @JvmStatic
    fun blend(color1: Color, color2: Color): Color {
        return blend(color1, color2, 0.5)
    }
    @JvmStatic
    fun colorFromInt(color: Int): Color {
        val c = Color(color)
        return Color(c.red, c.green, c.blue, 255)
    }

    @JvmStatic
    fun getRainbowOpaque(seconds: Int, saturation: Float, brightness: Float, index: Int):
            Int { val hue = (System.currentTimeMillis() + index) % (seconds * 1000) / (seconds * 1000).toFloat()
        return Color.HSBtoRGB(hue, saturation, brightness)
    }

    fun astolfoRainbow(delay: Int, offset: Int, index: Int): Int {
        var rainbowDelay = Math.ceil((System.currentTimeMillis() + (delay * index).toLong()).toDouble()) / offset
        return Color.getHSBColor(if ((360.0.also { rainbowDelay %= it } / 360.0).toFloat()
            < 0.5) -(rainbowDelay / 360.0).toFloat() else (rainbowDelay / 360.0).toFloat(),
            0.5f,
            1.0f).rgb
    }

    fun translateAlternateColorCodes(textToTranslate: String): String {
        val chars = textToTranslate.toCharArray()

        for (i in 0 until chars.size - 1) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".contains(chars[i + 1], true)) {
                chars[i] = '§'
                chars[i + 1] = Character.toLowerCase(chars[i + 1])
            }
        }

        return String(chars)
    }


    @JvmStatic
    fun LiquidSlowly(time: Long, count: Int, qd: Float, sq: Float): Color {
        val color = Color(Color.HSBtoRGB((time.toFloat() + count * -3000000f) / 2 / 1.0E9f, qd, sq))
        return Color(color.red / 255.0f * 1, color.green / 255.0f * 1, color.blue / 255.0f * 1, color.alpha / 255.0f)
    }

    fun reAlpha(color: Int, alpha: Float): Int {
        val c = Color(color)
        val r = 0.003921569f * c.red.toFloat()
        val g = 0.003921569f * c.green.toFloat()
        val b = 0.003921569f * c.blue.toFloat()
        return Color(r, g, b, alpha).rgb
    }


    fun randomMagicText(text: String): String {
        val stringBuilder = StringBuilder()
        val allowedCharacters = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000"

        for (c in text.toCharArray()) {
            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                val index = Random().nextInt(allowedCharacters.length)
                stringBuilder.append(allowedCharacters.toCharArray()[index])
            }
        }

        return stringBuilder.toString()
    }

    fun colorCode(code: String, alpha: Int = 255): Color {
        when (code.lowercase()) {
            "0" -> {
                return Color(0, 0, 0, alpha)
            }
            "1" -> {
                return Color(0, 0, 170, alpha)
            }
            "2" -> {
                return Color(0, 170, 0, alpha)
            }
            "3" -> {
                return Color(0, 170, 170, alpha)
            }
            "4" -> {
                return Color(170, 0, 0, alpha)
            }
            "5" -> {
                return Color(170, 0, 170, alpha)
            }
            "6" -> {
                return Color(255, 170, 0, alpha)
            }
            "7" -> {
                return Color(170, 170, 170, alpha)
            }
            "8" -> {
                return Color(85, 85, 85, alpha)
            }
            "9" -> {
                return Color(85, 85, 255, alpha)
            }
            "a" -> {
                return Color(85, 255, 85, alpha)
            }
            "b" -> {
                return Color(85, 255, 255, alpha)
            }
            "c" -> {
                return Color(255, 85, 85, alpha)
            }
            "d" -> {
                return Color(255, 85, 255, alpha)
            }
            "e" -> {
                return Color(255, 255, 85, alpha)
            }
            else -> {
                return Color(255, 255, 255, alpha)
            }
        }
    }


    fun reAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha)
    }

    fun reAlpha(color: Color, alpha: Float): Color {
        return Color(color.red / 255f, color.green / 255f, color.blue / 255f, alpha)
    }

    fun slowlyRainbow(time: Long, count: Int, qd: Float, sq: Float): Color {
        val color = Color(Color.HSBtoRGB((time.toFloat() + count * -3000000f) / 2 / 1.0E9f, qd, sq))
        return Color(color.red / 255.0f * 1, color.green / 255.0f * 1, color.blue / 255.0f * 1, color.alpha / 255.0f)
    }

    fun skyRainbow(var2: Int, bright: Float, st: Float, speed: Double): Color {
        var v1 = ceil(System.currentTimeMillis() / speed + var2 * 109L) / 5
        return Color.getHSBColor(if ((360.0.also { v1 %= it } / 360.0) <0.5) { -(v1 / 360.0).toFloat() } else { (v1 / 360.0).toFloat() }, st, bright)
    }

    fun fade(color: Color, index: Int, count: Int): Color {
        val hsb = FloatArray(3)
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        var brightness =
            abs(((System.currentTimeMillis() % 2000L).toFloat() / 1000.0f + index.toFloat() / count.toFloat() * 2.0f) % 2.0f - 1.0f)
        brightness = 0.5f + 0.5f * brightness
        hsb[2] = brightness % 2.0f
        return Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]))
    }

    fun reverseColor(color: Color) = Color(255 - color.red, 255 - color.green, 255 - color.blue, color.alpha)

    fun healthColor(hp: Float, maxHP: Float, alpha: Int = 255): Color {
        val pct = ((hp / maxHP) * 255F).toInt()
        return Color(max(min(255 - pct, 255), 0), max(min(pct, 255), 0), 0, alpha)
    }

    fun darker(color: Color, percentage: Float): Color {
        return Color((color.red * percentage).toInt(), (color.green * percentage).toInt(), (color.blue * percentage).toInt(), (color.alpha * percentage).toInt())
    }

    fun darkerFixed(color: Color, percentage: Float): Color {
        return Color(((color.red * percentage).toInt()).coerceAtMost(255), ((color.green * percentage).toInt()).coerceAtMost(255), ((color.blue * percentage).toInt()).coerceAtMost(255), ((color.alpha * percentage).toInt()).coerceAtMost(255))
    }

    fun mixColors(color1: Color, color2: Color, percent: Float): Color {
        return Color(color1.red + ((color2.red - color1.red) * percent).toInt(), color1.green + ((color2.green - color1.green) * percent).toInt(), color1.blue + ((color2.blue - color1.blue) * percent).toInt(), color1.alpha + ((color2.alpha - color1.alpha) * percent).toInt())
    }
    @JvmField
    val hexColorsl = IntArray(16)

    init {
        repeat(16) { i ->
            val baseColor = (i shr 3 and 1) * 85

            val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
            val green = (i shr 1 and 1) * 170 + baseColor
            val blue = (i and 1) * 170 + baseColor

            hexColorsl[i] = red and 255 shl 16 or (green and 255 shl 8) or (blue and 255)
        }
    }
}


