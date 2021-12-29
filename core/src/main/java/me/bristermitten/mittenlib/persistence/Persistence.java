package me.bristermitten.mittenlib.persistence;

import me.bristermitten.mittenlib.util.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persistences are stateful wrappers over some storage mechanism. This could include a flat-file
 * , a database, or simply a cache in memory. They store data in a Key->Value method, using keys
 * for most operations. Note that the backend is not required to store data in this way (for
 * example a JSON file based persistence may simply load a large file and store its data in a
 * {@link java.util.Map}).
 * Persistences should be non-blocking in their operations, returning a {@link CompletableFuture}
 * everywhere. ID's must also be unique, 2 values cannot share the same ID.
 *
 * @param <I> The ID type. The type {@link T} should have 1 public, immutable field of this type
 *            to serve as the ID / Key
 * @param <T> The entity type
 */
public interface Persistence<I, T> {
    /**
     * Initialize the Persistence. This could include reading files into memory, or creating a
     * table in a database.
     *
     * @return A future that completes when the initialization stage finishes
     */
    @NotNull CompletableFuture<Unit> init();

    /**
     * Clean up the Persistence. This could include flushing caches and closing any database
     * connections
     *
     * @return A future that completes when the cleanup stage finishes
     */
    @NotNull CompletableFuture<Unit> cleanup();

    /**
     * Save a value to the Persistence's source
     *
     * @param value the value to save
     * @return A future that completes when the save finishes
     */
    @NotNull CompletableFuture<Unit> save(@NotNull T value);

    /**
     * Load a value from the Persistence by its ID
     * If no value exists, {@link Optional#empty()} should be returned
     *
     * @param id The ID of the value to lookup
     * @return A future that holds the value, if present, otherwise an empty optional
     */
    @NotNull CompletableFuture<Optional<T>> load(@NotNull I id);

    /**
     * Delete a value from the Persistence by its ID
     * If there is no value with the given ID present, nothing happens.
     *
     * @param id The ID of the value to delete
     * @return A future that completes when the delete operation finishes
     */
    @NotNull CompletableFuture<Unit> delete(@NotNull I id);

    /**
     * Load all the values from the Persistence's backend. No guarantee is made about the
     * returned {@link Collection}'s implementation, but it must contain all values from the
     * backend once and once only.
     * @return A future that holds all the values in the Persistence's backend
     */
    @NotNull CompletableFuture<Collection<T>> loadAll();

    /**
     * Save all values in a given collection. This could be equivalent to looping over the
     * collection and calling {@link Persistence#save(Object)} for each element, however this
     * method gives the opportunity for more efficient operations (for example using transactions
     * for a SQL database).
     * This method should override any <i>duplicate</i> values in the persistence, but preserve
     * any values that are not in the given collection
     * @param values The values to save.
     * @return A future that completes when all the values are saved.
     */
    @NotNull CompletableFuture<Unit> saveAll(@NotNull Collection<T> values);
}
