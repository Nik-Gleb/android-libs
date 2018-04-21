package arch.blocks;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Some Manager.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 18/04/2018
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Manager<T> extends Provider<Optional<T>>, BiPredicate<Runnable, Boolean>
{/** Registration flags. */  boolean REGISTER = true, UNREGISTER = false;}