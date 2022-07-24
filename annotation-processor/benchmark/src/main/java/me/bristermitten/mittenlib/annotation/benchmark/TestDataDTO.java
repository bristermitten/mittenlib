package me.bristermitten.mittenlib.annotation.benchmark;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatterns;
import org.bukkit.Material;

import java.util.Map;

@Config
@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
public class TestDataDTO {
    boolean a;
    Material b;
    int c;
    int d;
    int e;
    Map<Integer, Material> f;

    int g;

    int h;

    Material i;

    int j;
    int k;

    LGsonDTO l;
    Material m;
    Material n;
    Material o;

    @Override
    public String toString() {
        return "TestDataGson{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                ", f=" + f +
                ", g=" + g +
                ", h=" + h +
                ", i=" + i +
                ", j=" + j +
                ", k=" + k +
                ", l=" + l +
                ", m=" + m +
                ", n=" + n +
                ", o=" + o +
                '}';
    }

    @Config
    static class LGsonDTO {
        String p;
        int q;
    }
}
