package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

public class NoOpConfigInitializationStrategy implements ConfigInitializationStrategy {
    @Override
    public Result<Unit> initializeConfig(String filePath) {
        // No op
        return Unit.unitResult();
    }
}