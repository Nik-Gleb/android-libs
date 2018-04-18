package arch.blocks;

import java.io.Closeable;

/**
 * Some module.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 18/04/2018
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Module extends Closeable
{/** {@inheritDoc} */ @Override default void close() {};}