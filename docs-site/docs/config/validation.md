# Validation

MittenLib provides a powerful validation system that runs during the loading process, ensuring that your application never starts with invalid data.

## Built-in Constraints

| Annotation | Description |
| :--- | :--- |
| `@NotBlank` | String must not be null or whitespace only. |
| `@Positive` | Number must be > 0. |
| `@Negative` | Number must be < 0. |
| `@Min(val)` | Number must be at least `val`. |
| `@Max(val)` | Number must be at most `val`. |
| `@Range(min, max)` | Number must be between `min` and `max`. |

## Custom Validators

You can define custom validation logic using `@ValidateWith`.

```java
@Config
public class UserConfigDTO {
    @ValidateWith(EmailValidator.class)
    String contactEmail;
}
```

The validator must implement `me.bristermitten.mittenlib.config.validation.Validator<T>`.

```java
public class EmailValidator implements Validator<String> {
    @Override
    public Optional<String> validate(String value) {
        if (isValidEmail(value)) return Optional.empty();
        return Optional.of("Must be a valid email address");
    }
}
```
