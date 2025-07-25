package me.bristermitten.mittenlib.codegen;

/// Different strategies for generating match methods for records or unions.
/// Examples of the specific strategies will work with the following example:
/// {@snippet
///
/// @Config interface MyUnion {
///    MyUnion Child1(String value);
///    MyUnion Child2(int value);
/// }
/// }
/// ```
public enum MatchStrategies {
    /// Generates match methods that take functional interfaces accepting the constructor subtype.
    /// For example, for the above union, it would generate:
    ///
    /// ```java
    /// static void match(Consumer<Child1> child1Consumer, Consumer<Child2> child2Consumer);
    ///````
    NOMINAL,
    /// Generates match methods that take functional interfaces accepting the constructor's properties
    ///  For example, for the above union, it would generate:
    /// ```java
    /// static void match(
    ///     Runnable child1Consumer,
    ///     Consumer<Integer> child2Consumer);
    ///```
    STRUCTURAL
}
