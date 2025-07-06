/**
 * The structure of a config's data in a more abstract form than a class hierarchy.
 * <p>
 * As a reference, there are 3 ways of organising data for a config.
 * The first, most simple, is an atomic structure, described by a simple class or interface, eg:
 * {@snippet :
 *
 * @Config public interface TestConfig {
 * int number();
 * }
 *}
 * <p>
 * We can then compose config types into bigger ones using inheritance, creating an intersection structure:
 * {@snippet :
 *     @Config
 *     public interface AddedConfig extends TestConfig {
 *         int anotherNumber();
 *     }
 *}
 * This describes a structure with 2 properties, the <code>int number</code> and the <code>int anotherNumber</code>.
 * When using interfaces, multiple inheritance is allowed for more flexible composition.
 * Note that the actual implementation (composition vs inheritance) is left as an implementation detail.
 * Finally, we can create a union of types:
 * {@snippet :
 *
 * import me.bristermitten.mittenlib.config.Config;
 * import org.jetbrains.annotations.Nullable;
 *     @ConfigUnion
 *     public interface RepositoryConfig {
 *         @Config
 *         interface URLConfig extends RepositoryConfig {
 *             String url();
 *
 *             @Nullable String authToken();
 *         }
 *
 *         @Config
 *         interface FileSystemConfig extends RepositoryConfig {
 *             String path();
 *         }
 *     }
 *}
 * which essentially says that anytime a <code>RepositoryConfig</code> is referenced, it can be in the form of either of the subtypes.
 * Note that this is an <i>undiscriminated</i> union, i.e. there's nothing to distinguish between the two subtypes aside from their structure.
 * This is not considered a design flaw, but it means there's often no fast way to deserialise aside from trial and error.
 * //     * To denote discriminated unions; we can use annotations:
 * //     * {@snippet :
 * //     *
 * //     * import me.bristermitten.mittenlib.config.Config;
 * //     * import org.jetbrains.annotations.Nullable;
 * //     *     @ConfigUnion(tag = "type")
 * //     *     public interface RepositoryConfig {
 * //     *         @Config
 * //     *         @UnionTag("url")
 * //     *         interface URLConfig extends RepositoryConfig {
 * //     *             String url();
 * //     *
 * //     *             @Nullable String authToken();
 * //     *         }
 * //     *
 * //     *         @Config
 * //     *         @UnionTag("file")
 * //     *         interface FileSystemConfig extends RepositoryConfig {
 * //     *             String path();
 * //     *         }
 * //     * }}
 */
package me.bristermitten.mittenlib.annotations.ast;