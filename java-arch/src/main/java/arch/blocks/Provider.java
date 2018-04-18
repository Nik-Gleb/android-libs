package arch.blocks;

import java.util.function.Supplier;

/**
 * Some Module Provider.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 18/04/2018
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Provider<T> extends Module, Supplier<T> {}
