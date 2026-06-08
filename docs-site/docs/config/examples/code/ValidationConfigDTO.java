@Config
public class ValidationConfig {
    @Positive
    public int positiveInt;

    @NotBlank
    public String notBlankString;

    @ValidateWith(CustomStringValidator.class)
    public String customValidated;
}
