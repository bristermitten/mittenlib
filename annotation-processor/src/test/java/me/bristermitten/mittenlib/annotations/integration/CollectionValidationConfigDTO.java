package me.bristermitten.mittenlib.annotations.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.validation.*;
import org.jspecify.annotations.Nullable;

@Config
public class CollectionValidationConfigDTO {
    public List<@NotBlank String> names;

    public Set<@Positive Integer> values;

    public Map<@NotBlank String, @Min(0) Integer> scores;

    public List<@Nullable @NotBlank String> nullableNames;

    public List<@ValidateWith(CustomStringValidator.class) String> customList;

    public Map<String, @ValidateWith(CustomStringValidator.class) String> customMap;
}
