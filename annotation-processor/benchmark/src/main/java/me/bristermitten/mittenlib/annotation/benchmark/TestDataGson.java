package me.bristermitten.mittenlib.annotation.benchmark;

import org.bukkit.Material;

import java.util.Map;

public class TestDataGson {
    private boolean a;
    private Material b;
    private int c;
    private int d;
    private int e;
    private Map<Integer, Material> f;

    private int g;

    private int h;

    private Material i;

    private int j;
    private int k;

    private LGson l;
    private Material m;
    private Material n;
    private Material o;


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

    public boolean isA() {
        return a;
    }

    public void setA(boolean a) {
        this.a = a;
    }

    public Material getB() {
        return b;
    }

    public void setB(Material b) {
        this.b = b;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public int getD() {
        return d;
    }

    public void setD(int d) {
        this.d = d;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    public Map<Integer, Material> getF() {
        return f;
    }

    public void setF(Map<Integer, Material> f) {
        this.f = f;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public Material getI() {
        return i;
    }

    public void setI(Material i) {
        this.i = i;
    }

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public LGson getL() {
        return l;
    }

    public void setL(LGson l) {
        this.l = l;
    }

    public Material getM() {
        return m;
    }

    public void setM(Material m) {
        this.m = m;
    }

    public Material getN() {
        return n;
    }

    public void setN(Material n) {
        this.n = n;
    }

    public Material getO() {
        return o;
    }

    public void setO(Material o) {
        this.o = o;
    }

    public static class LGson {
        private String p;
        private int q;

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public int getQ() {
            return q;
        }

        public void setQ(int q) {
            this.q = q;
        }
    }
}
