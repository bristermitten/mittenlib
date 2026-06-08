public class ValidationConfigValidator {
  private final CustomStringValidator customStringValidator;

  @Inject
  public ValidationConfigValidator(CustomStringValidator customStringValidator) {
    this.customStringValidator = customStringValidator;
  }

  public Result<ValidationConfig> validate(ValidationConfig config) {
    List<ConfigValidationException.Violation> violations = new ArrayList<>();
    
    if (config.positiveInt() <= 0) {
      violations.add(new ConfigValidationException.Violation("positiveInt", config.positiveInt(), "Must be positive"));
    }
    
    if (config.notBlankString().trim().isEmpty()) {
      violations.add(new ConfigValidationException.Violation("notBlankString", config.notBlankString(), "Must not be blank"));
    }
    
    // ... custom validator calls ...

    if (!violations.isEmpty()) {
      return Result.fail(new ConfigValidationException(ValidationConfig.class, violations));
    }
    return Result.ok(config);
  }
}
