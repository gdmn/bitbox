/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.devsite.bitbox.sendables;

/**
 *
 * @author gorladam
 */
public interface SendableFilter {
    boolean isAllowed(Sendable sendable, Object hostAddress, Object authenticatedUser);
}
