package arch.blocks;

import java.util.function.Consumer;

/**
 * Some Repository.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 18/04/2018
 */
public interface Repository<T> extends Manager<T>, Consumer<T>
{/** {@inheritDoc} */ @Override default void accept(T t) {}}