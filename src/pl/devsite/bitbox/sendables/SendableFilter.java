package pl.devsite.bitbox.sendables;

/**
 *
 * @author dmn
 */
public interface SendableFilter {
    boolean isAllowed(Sendable sendable, Object hostAddress, Object authenticatedUser);
}
