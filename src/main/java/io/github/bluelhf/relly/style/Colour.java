package io.github.bluelhf.relly.style;

import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

public class Colour {
    private final int num;

    public Colour(int num) {
        this.num = num;
    }

    public int getRGB() {
        return num;
    }

    public TextColor getColour() {
        return TextColor.color(num);
    }

    public Colour comment() {
        Color color = new Color(getRGB());
        float[] rgb = color.getRGBColorComponents(null);
        float[] hsb = Color.RGBtoHSB((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), null);
        hsb[2] /= 2F; // Halve brightness
        hsb[1] /= 20F; // Divide saturation by 20

        return new Colour(Color.getHSBColor(hsb[0], hsb[1], hsb[2]).getRGB());
    }
}
