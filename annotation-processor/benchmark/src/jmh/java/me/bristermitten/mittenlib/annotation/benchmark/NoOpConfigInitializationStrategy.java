package me.bristermitten.mittenlib.annotation.benchmark;

import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

public class NoOpConfigInitializationStrategy implements ConfigInitializationStrategy {
    @Override
    public Result<Unit> initializeConfig(String filePath) {
        // No op
        return Unit.unitResult();
    }
}
