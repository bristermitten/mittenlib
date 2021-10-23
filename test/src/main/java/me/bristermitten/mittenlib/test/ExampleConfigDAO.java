package me.bristermitten.mittenlib.test;

import me.bristermitten.mittenlib.config.Config;

import java.util.Arrays;
import java.util.List;

@Config
public class ExampleConfigDAO {
    final int messageDelay = 10;
    final List<String> message = Arrays.asList("You're playing &cCoolServerMC", "Have fun %player_name%");
    final String username = null;
    final BDAO b = null;

    @Config
    static class BDAO {
        final int a = 3;
        final String b = "aaa";
    }
}
