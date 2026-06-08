package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.validation.*;

@Config
public class ValidationConfigDTO {
    @Positive
    public int positiveInt;

    @Negative
    public double negativeDouble;

    @Min(10)
    public int minInt;

    @Max(100)
    public long maxLong;

    @Range(min = 1.0, max = 5.0)
    public double rangeDouble;

    @NotBlank
    public String notBlankString;

    @ValidateWith(CustomStringValidator.class)
    public String customValidated;
}
